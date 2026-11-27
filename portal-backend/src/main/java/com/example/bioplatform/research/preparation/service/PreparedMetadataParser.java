package com.example.bioplatform.research.preparation.service;

import com.example.bioplatform.research.collection.common.dto.SourcePublicationRecord;
import com.example.bioplatform.research.common.PublicationMetadataProcessor;
import com.example.bioplatform.research.common.PublicationValues;
import com.example.bioplatform.research.preparation.model.PreparedMember;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 逐文献的纯本地解析器；分别构造五类成员，不联网、不保存跨文献作者实体，内存仅保留当前文献。
 */
@Component
public class PreparedMetadataParser {
    /**
     * TYPES：本组件支持的维度或类型白名单。
     */
    public static final List<String> TYPES = List.of("AUTHOR", "INSTITUTION", "FUND", "TOPIC", "YEAR");
    /**
     * json：JSON解析与序列化组件。
     */
    private final ObjectMapper json;
    /**
     * processor：共享的作者、机构、资助方和主题处理组件。
     */
    private final PublicationMetadataProcessor processor;

    /**
     * 构造PreparedMetadataParser，注入本组件所需依赖并建立初始状态。
     * @param json JSON解析与序列化组件
     */
    public PreparedMetadataParser(ObjectMapper json) {
        this.json = json;
        processor = new PublicationMetadataProcessor(json);
    }

    /**
     * 把受支持的维度名映射为固定成员表名；拒绝任意动态表名输入。
     */
    public static String table(String type) {
        if (!TYPES.contains(type)) throw new IllegalArgumentException("Unknown member type");
        return "research_prepared_" + type.toLowerCase(Locale.ROOT);
    }

    /**
     * 单篇文献五类成员及其内容摘要，供事务内持久化和变化检测。
     * @param members 按AUTHOR、INSTITUTION、FUND、TOPIC、YEAR分别保存的文献内成员列表
     * @param contentHash 解析内容摘要，用于判断记录是否发生变化
     */
    public record Parsed(Map<String, List<PreparedMember>> members, String contentHash) {
    }

    /**
     * 纯函数式拆分一篇记录的作者机构、独立机构、资助方、主题和年份；每类按成员键去重，避免维度交叉产生笛卡尔积。
     */
    public Parsed parse(SourcePublicationRecord row, Map<String, String> dictionary) {
        validate(row.getAuthorsJson());
        validate(row.getGrantsJson());
        validate(row.getMeshHeadingsJson());
        validate(row.getKeywordList());
        validate(row.getAffiliationInfo());
        var input = new PublicationMetadataProcessor.Input(row.getAuthorsJson(), row.getAffiliationInfo(), row.getGrantsJson(), row.getKeywordList(), row.getMeshHeadingsJson(), row.getCncbBanksJson());
        var output = processor.parse(input, dictionary);
        Map<String, LinkedHashMap<String, PreparedMember>> values = new LinkedHashMap<>();
        TYPES.forEach(t -> values.put(t, new LinkedHashMap<>()));
        List<JsonNode> nodes = new ArrayList<>();
        try {
            if (row.getAuthorsJson() != null) authorNodes(json.readTree(row.getAuthorsJson()), nodes);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid authors", ex);
        }
        List<PublicationMetadataProcessor.AuthorIdentity> authors = new ArrayList<>();
        Set<String> known = new LinkedHashSet<>();
        for (var node : nodes) {
            var single = processor.parse(new PublicationMetadataProcessor.Input("[" + node.toString() + "]", null, null, null, null, null), Map.of());
            for (var author : single.authors()) {
                authors.add(author);
                if (author.institution() != null)
                    for (String v : author.institution().split(";\\s*")) if (!v.isBlank()) known.add(v);
            }
        }
        int order = 0;
        for (var author : authors) {
            order++;
            // 只有其他作者的已知机构唯一时才继承；存在多个不同机构则继续保留未知。
            String selected = author.institution() == null && known.size() == 1 ? known.iterator().next() : author.institution();
            String[] institutions = selected == null ? new String[]{
                    ""
            }
                    : selected.split(";\\s*");
            for (String institution : institutions) {
                var m = member(author.name() + "\u001f" + institution, author.name() + (institution.isBlank() ? "" : " · " + institution));
                m.setAuthorName(author.name());
                m.setInstitutionName(institution.isBlank() ? null : institution);
                m.setAuthorOrder(order);
                values.get("AUTHOR").putIfAbsent(m.getMemberKey(), m);
                if (!institution.isBlank()) add(values, "INSTITUTION", institution);
            }
        }
        // Include every author's affiliation, including when other authors have pre-normalized names.
        // 机构成员按文献独立去重，同机构多位作者不能让机构文献数重复增加。
        output.institutions().forEach(v -> {
            for (String name : v.split(";\\s*")) if (!name.isBlank()) add(values, "INSTITUTION", name);
        });
        output.funds().forEach(v -> add(values, "FUND", v));
        output.topics().forEach(v -> add(values, "TOPIC", v));
        if (row.getPubYear() != null && row.getPubYear() > 0) {
            var m = member(row.getPubYear().toString(), row.getPubYear().toString());
            m.setStatYear(row.getPubYear());
            values.get("YEAR").put(m.getMemberKey(), m);
        }
        Map<String, List<PreparedMember>> result = new LinkedHashMap<>();
        values.forEach((t, v) -> result.put(t, new ArrayList<>(v.values())));
        try {
            return new Parsed(result, PublicationValues.sha("preparation-v1|" + json.writeValueAsString(result)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * 提前检查看起来是JSON的载荷；格式异常让当前整理批次回滚，避免写入不完整成员。
     */
    private void validate(String value) {
        if (value == null || value.isBlank()) return;
        String v = value.trim();
        if (v.startsWith("[") || v.startsWith("{")) try {
            json.readTree(v);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Malformed metadata JSON; preparation batch rolled back", ex);
        }
    }

    /**
     * 兼容作者数组及嵌套对象，提取实际作者节点；不把整篇机构随意分配给所有作者。
     */
    private void authorNodes(JsonNode node, List<JsonNode> out) {
        if (node == null || node.isNull()) return;
        if (node.isTextual()) {
            if (!node.asText().isBlank()) {
                var author = json.createObjectNode();
                author.put("fullName", node.asText());
                out.add(author);
            }
            return;
        }
        if (node.isArray()) {
            node.forEach(n -> authorNodes(n, out));
            return;
        }
        if (!node.isObject()) return;
        if (node.hasNonNull("fullName") || node.hasNonNull("collectiveName") || node.hasNonNull("firstName") || node.hasNonNull("foreName") || node.hasNonNull("givenName") || node.hasNonNull("lastName") || node.hasNonNull("familyName")) {
            out.add(node);
            return;
        }
        node.elements().forEachRemaining(n -> {
            if (n.isContainerNode()) authorNodes(n, out);
        });
    }

    /**
     * 规范化大小写及空白后生成成员摘要；该摘要用于同维度分组，不代表已验证的自然人身份。
     */
    private static PreparedMember member(String key, String label) {
        var m = new PreparedMember();
        m.setMemberKey(PublicationValues.sha(key.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ")));
        m.setDisplayName(label);
        return m;
    }

    /**
     * 把当前维度的规范成员加入文献内去重集合。
     * @param values 待遍历或去重的值集合
     * @param type 当前分类、维度或唯一值类型，按调用上下文解释
     * @param label 成员或页面的展示文本
     */
    private static void add(Map<String, LinkedHashMap<String, PreparedMember>> values, String type, String label) {
        var m = member(label, label);
        values.get(type).putIfAbsent(m.getMemberKey(), m);
    }
}

package com.example.bioplatform.research.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

/**
 * 纯元数据处理：固定结构输入/输出，无数据库和网络访问，不持有跨文献状态。
 */
public final class PublicationMetadataProcessor {
    /**
     * objectMapper：JSON解析与序列化组件。
     */
    private final ObjectMapper objectMapper;

    /**
     * 构造PublicationMetadataProcessor，注入本组件所需依赖并建立初始状态。
     * @param objectMapper JSON解析与序列化组件
     */
    public PublicationMetadataProcessor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 统一元数据处理输入；作者、机构、原始资助、关键词及资源关联分别保留。
     * @param authorsJson 逐作者原始信息及作者所属机构的JSON载荷
     * @param affiliationInfo 来源提供的机构汇总原文，不应随意分配给所有作者
     * @param grantsJson 保留完整资助记录的JSON载荷，便于整理时重新解析
     * @param keywordList 来源提供的论文关键词载荷
     * @param meshHeadingsJson PubMed MeSH主题词的原始JSON
     * @param cncbBanksJson CNCB资源关联数组，包含db、accession和link等
     */
    public record Input(String authorsJson, String affiliationInfo, String grantsJson, String keywordList,
                        String meshHeadingsJson, String cncbBanksJson) {
    }

    /**
     * 解析后的作者姓名与其所属机构组合，不代表已验证的跨文献自然人实体。
     * @param name 当前对象或资源的展示名称
     * @param institution 机构维度相关的处理结果或覆盖量
     */
    public record AuthorIdentity(String name, String institution) {
    }

    /**
     * 元数据解析后的独立作者、机构、资助方及主题集合，不做跨维度交叉展开。
     * @param authors 已解析的作者信息集合
     * @param institutions 去重后的机构集合
     * @param funds 按agency或本地字典解析后可用于统计的去重资助方名称
     * @param topics 解析后的主题列表或冻结主题配置
     */
    public record Output(Set<AuthorIdentity> authors, Set<String> institutions, Set<String> funds, Set<String> topics) {
    }

    /**
     * 从统一输入独立提取作者机构、资助方、主题及来源信息；纯本地转换，不访问网络或数据库。
     */
    public Output parse(Input input, Map<String, String> dictionary) {
        return new Output(ordered(authorWithInstitutions(input)), ordered(values(input, "INSTITUTION", dictionary)), ordered(values(input, "FUND", dictionary)), ordered(publicationTopics(input)));
    }

    /**
     * 采集阶段只合并原始JSON，不运行机构归一化或基金名称解析。
     */
    public Input merge(Input existing, Input incoming) {
        return new Input(ResearchPublicationJsonMerger.mergeAuthors(existing.authorsJson, incoming.authorsJson, objectMapper),
                present(incoming.affiliationInfo) ? incoming.affiliationInfo : existing.affiliationInfo,
                GrantPayloads.merge(existing.grantsJson, incoming.grantsJson, objectMapper),
                present(incoming.keywordList) ? incoming.keywordList : existing.keywordList,
                present(incoming.meshHeadingsJson) ? incoming.meshHeadingsJson : existing.meshHeadingsJson,
                ResearchPublicationJsonMerger.mergeBanks(existing.cncbBanksJson, incoming.cncbBanksJson, objectMapper));
    }

    /**
     * 将grants或funding转换为统一的基金JSON结构，保留原始可用字段。
     * @param grants 原始或合并后的资助记录
     * @param funding 上游funding载荷，可能只包含资助编号
     * @return 按上述规则计算或读取的String结果
     */
    public String normalizeGrants(JsonNode grants, JsonNode funding) {
        return GrantPayloads.normalize(grants, funding, objectMapper);
    }

    /**
     * 判断值或来源身份是否存在且有效，避免把空字符串或字面null当成有效信息。
     * @param value 待校验、转换或合并的值
     * @return true表示满足上述条件，false表示不满足
     */
    private static boolean present(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * 返回保持首次出现顺序的去重结果，避免无序集合打乱作者或标签顺序。
     * @param values 待遍历或去重的值集合
     * @return 按上述规则计算或读取的Set<T>结果
     */
    private static <T> Set<T> ordered(Set<T> values) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(values));
    }

    /**
     * 读取当前输入容器中的有效值，并按本类约定转换为统一集合。
     * @param p 当前记录、文件或文献对象
     * @param type 当前分类、维度或唯一值类型，按调用上下文解释
     * @param dictionary 基金编号到规范名称的本地查询结果
     * @return 按上述规则计算或读取的Set<String>结果
     */
    private Set<String> values(Input p, String type, Map<String, String> dictionary) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if ("INSTITUTION".equals(type)) {
            collectJsonValues(p.authorsJson, Set.of("institutionname"), result);
            if (result.isEmpty()) {
                collectJsonValues(p.authorsJson, Set.of("affiliation", "institution", "organization", "affiliationname"), result);
                collectJsonValues(p.affiliationInfo, Set.of("affiliation", "institution", "organization", "name"), result);
                if (result.isEmpty() && notBlank(p.affiliationInfo) && !looksJson(p.affiliationInfo))
                    splitAffiliations(p.affiliationInfo, result);
                Set<String> normalized = normalizeInstitutions(result);
                result.clear();
                result.addAll(normalized);
            }
        } else if ("FUND".equals(type)) {
            collectGrantValues(p.grantsJson, dictionary, result);
        }
        result.removeIf(v -> v.length() < 2 || "null".equalsIgnoreCase(v));
        return result;
    }

    /**
     * 把单个作者对应的机构拆成作者机构身份；机构归属保持在作者层级，不按整篇机构列表交叉组合。
     */
    private Set<AuthorIdentity> authorWithInstitutions(Input publication) {
        LinkedHashSet<AuthorIdentity> result = new LinkedHashSet<>();
        if (!notBlank(publication.authorsJson)) return result;
        try {
            JsonNode root = objectMapper.readTree(publication.authorsJson);
            List<JsonNode> authors = new ArrayList<>();
            collectAuthorNodes(root, authors);
            List<AuthorIdentity> parsed = new ArrayList<>();
            Set<String> knownAffiliations = new LinkedHashSet<>();
            for (JsonNode author : authors) {
                String name = authorName(author);
                if (!notBlank(name)) continue;
                LinkedHashSet<String> rawAffiliations = new LinkedHashSet<>();
                String preNormalized = firstText(author, "institutionName");
                Set<String> affiliations;
                if (notBlank(preNormalized)) affiliations = Set.of(preNormalized.trim());
                else {
                    collectNode(author, Set.of("affiliation", "institution", "organization", "affiliationname"), rawAffiliations);
                    affiliations = normalizeInstitutions(rawAffiliations);
                }
                knownAffiliations.addAll(affiliations);
                parsed.add(new AuthorIdentity(name, affiliations.isEmpty() ? null : String.join("; ", affiliations)));
            }
            String commonAffiliation = knownAffiliations.size() == 1
                    ? knownAffiliations.iterator().next() : null;
            for (AuthorIdentity author : parsed) {
                // 文章级机构汇总无法可靠对应到个人；仅在其他作者机构唯一时继承。
                result.add(new AuthorIdentity(author.name,
                        notBlank(author.institution) ? author.institution : commonAffiliation));
            }
        } catch (Exception ignored) {
        }

        // 少数来源只有姓名而没有标准作者数组，仍保留作者但明确机构未知。
        if (result.isEmpty()) {
            LinkedHashSet<String> names = new LinkedHashSet<>();
            collectJsonValues(publication.authorsJson, Set.of("fullname", "collectivename"), names);
            collectComposedNames(publication.authorsJson, names);
            names.forEach(name -> result.add(new AuthorIdentity(name, null)));
        }
        return result;
    }

    /**
     * 遍历作者容器并提取实际作者对象，不把其他元数据当成作者。
     * @param node 当前JSON、XML或队列节点
     * @param out 用于累计解析结果的输出对象
     */
    private void collectAuthorNodes(JsonNode node, List<JsonNode> out) {
        if (node == null) return;
        if (node.isArray()) {
            node.forEach(child -> collectAuthorNodes(child, out));
            return;
        }
        if (!node.isObject()) return;
        if (notBlank(authorName(node))) {
            out.add(node);
            return;
        }
        node.fields().forEachRemaining(entry -> collectAuthorNodes(entry.getValue(), out));
    }

    /**
     * 从完整姓名或firstName、lastName字段构建展示姓名，避免只保留姓氏。
     * @param node 当前JSON、XML或队列节点
     * @return 按上述规则计算或读取的String结果
     */
    private String authorName(JsonNode node) {
        String direct = firstText(node, "fullName", "collectiveName");
        if (notBlank(direct)) return direct.trim().replaceAll("\\s+", " ");
        String first = firstText(node, "firstName", "foreName", "givenName");
        String last = firstText(node, "lastName", "familyName");
        String composed = (first + " " + last).trim().replaceAll("\\s+", " ");
        return composed;
    }

    /**
     * 从指定字段载荷中收集规范化文本，兼容数组及嵌套对象。
     * @param json JSON解析与序列化组件
     * @param keys 待定位的业务键集合
     * @param out 用于累计解析结果的输出对象
     */
    private void collectJsonValues(String json, Set<String> keys, Set<String> out) {
        if (!notBlank(json)) return;
        try {
            collectNode(objectMapper.readTree(json), keys, out);
        } catch (Exception ignored) {
        }
    }

    /**
     * 收集原始资助条目的目标字段，保留本方法所需的基金信息。
     * @param json JSON解析与序列化组件
     * @param dictionary 基金编号到规范名称的本地查询结果
     * @param out 用于累计解析结果的输出对象
     */
    private void collectGrantValues(String json, Map<String, String> dictionary, Set<String> out) {
        if (!notBlank(json)) return;
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isArray()) return;
            for (JsonNode grant : root) {
                String name = FundAgencyParser.resolve(grant, dictionary);
                if (!name.isEmpty()) addText(out, name);
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * 遍历JSON节点，把符合字段约定的值加入去重输出集合。
     * @param node 当前JSON、XML或队列节点
     * @param keys 待定位的业务键集合
     * @param out 用于累计解析结果的输出对象
     */
    private void collectNode(JsonNode node, Set<String> keys, Set<String> out) {
        if (node == null) return;
        if (node.isArray()) node.forEach(child -> collectNode(child, keys, out));
        else if (node.isObject()) node.fields().forEachRemaining(e -> {
            if (keys.contains(e.getKey().toLowerCase(Locale.ROOT)) && e.getValue().isValueNode())
                addText(out, e.getValue().asText());
            else collectNode(e.getValue(), keys, out);
        });
    }

    /**
     * 从拆分姓名字段构建完整作者姓名并追加到输出集合。
     * @param json JSON解析与序列化组件
     * @param out 用于累计解析结果的输出对象
     */
    private void collectComposedNames(String json, Set<String> out) {
        if (!notBlank(json)) return;
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isArray()) return;
            root.forEach(n -> {
                String first = firstText(n, "firstName", "foreName", "givenName");
                String last = firstText(n, "lastName", "familyName");
                if (notBlank(first) || notBlank(last)) addText(out, (first + " " + last).trim());
            });
        } catch (Exception ignored) {
        }
    }

    /**
     * 按候选字段顺序返回第一个有效文本，不用空值覆盖已找到的信息。
     * @param node 当前JSON、XML或队列节点
     * @param names 来源或主题的显示名称映射
     * @return 按上述规则计算或读取的String结果
     */
    private String firstText(JsonNode node, String... names) {
        for (String name : names) if (node.hasNonNull(name)) return node.get(name).asText("");
        return "";
    }

    /**
     * 仅将有效文本加入当前去重结果，过滤空白及无意义缺失值。
     * @param out 用于累计解析结果的输出对象
     * @param value 待校验、转换或合并的值
     */
    private void addText(Set<String> out, String value) {
        if (value == null) return;
        String clean = value.trim().replaceAll("\\s+", " ");
        if (!clean.isEmpty() && !"null".equalsIgnoreCase(clean)) out.add(clean);
    }

    /**
     * 按机构边界拆分署名信息，去除空项并保留可用机构文本。
     * @param value 待校验、转换或合并的值
     * @param out 用于累计解析结果的输出对象
     */
    private void splitAffiliations(String value, Set<String> out) {
        Arrays.stream(value.split("[;\\n]")).forEach(v -> addText(out, v));
    }

    /**
     * 规范化机构文本并去重，仍保持各作者自己的机构边界。
     * @param affiliations 当前作者或文献的原始机构信息
     * @return 按上述规则计算或读取的Set<String>结果
     */
    private Set<String> normalizeInstitutions(Collection<String> affiliations) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (affiliations == null) return result;
        for (String affiliation : affiliations) {
            if (!notBlank(affiliation)) continue;
            String clean = affiliation.replaceAll("(?i)\\b[\\w.+-]+@[\\w.-]+\\.[A-Z]{2,}\\b", "")
                    .replaceAll("https?://\\S+", "").trim();
            for (String segment : clean.split("[;\\n]")) {
                String selected = null;
                String[] tokens = segment.split(",");
                // 常见署名格式为“科室/学院, 机构, 城市, 国家”，第二段若像机构名则优先采用。
                if (tokens.length > 1) {
                    String second = tokens[1].trim().replaceAll("^[\\d\\s.]+", "").replaceAll("[.\\s]+$", "");
                    boolean secondIsGenericUnit = second.matches("(?i)^(department|dept\\.?|division|faculty|school)\\b.*");
                    boolean secondIsOrganization = second.matches("(?i).*(university|universit[eé]|institute|institut|hospital|medical center|medical centre|\\bcenter\\b|\\bcentre\\b|academy|laboratory|laboratories|\\blab\\b|college|clinic|health system).*");
                    if (secondIsOrganization && !secondIsGenericUnit) selected = second;
                }
                for (String token : selected == null ? tokens : new String[0]) {
                    String candidate = token.trim().replaceAll("^[\\d\\s.]+", "").replaceAll("[.\\s]+$", "");
                    if (candidate.isEmpty()) continue;
                    boolean genericUnit = candidate.matches("(?i)^(department|dept\\.?|division|faculty|school)\\b.*");
                    boolean organization = candidate.matches("(?i).*(university|universit[eé]|institute|institut|hospital|medical center|medical centre|\\bcenter\\b|\\bcentre\\b|academy|laboratory|laboratories|\\blab\\b|college|clinic|health system).*");
                    if (organization && !genericUnit) selected = candidate;
                }
                result.add(notBlank(selected) ? selected : segment.replaceAll("\\s+", " ").trim());
            }
        }
        result.removeIf(String::isBlank);
        return result;
    }

    /**
     * 判断文本是否具有JSON容器外形，决定使用结构化解析还是普通文本处理。
     * @param value 待校验、转换或合并的值
     * @return true表示满足上述条件，false表示不满足
     */
    private boolean looksJson(String value) {
        String v = value.trim();
        return v.startsWith("[") || v.startsWith("{");
    }

    /**
     * 判断文本是否具有非空内容，供解析和合并前过滤。
     * @param value 待校验、转换或合并的值
     * @return true表示满足上述条件，false表示不满足
     */
    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * 合并论文关键词与MeSH主题，规范化标签展示并按文献内去重。
     * @param publication 当前文献或已合并文献对象
     * @return 按上述规则计算或读取的Set<String>结果
     */
    private Set<String> publicationTopics(Input publication) {
        LinkedHashSet<String> topics = new LinkedHashSet<>();
        collectTopicField(publication.keywordList, topics);
        collectTopicField(publication.meshHeadingsJson, topics);
        topics.removeIf(value -> value.length() < 2 || value.length() > 200 || "null".equalsIgnoreCase(value));
        return topics;
    }

    /**
     * 从关键词或MeSH载荷提取主题名称，同一文献内重复主题只保留一次。
     * @param raw 上游原始载荷或未规范化参数
     * @param out 用于累计解析结果的输出对象
     */
    private void collectTopicField(String raw, Set<String> out) {
        if (!notBlank(raw)) return;
        try {
            JsonNode node = objectMapper.readTree(raw);
            if (node.isArray()) {
                for (JsonNode item : node) {
                    String value = item.isTextual() ? item.asText() : firstText(item, "descriptorName", "name", "term", "keyword", "value");
                    if (notBlank(value)) out.add(topicDisplayName(value));
                }
                return;
            }
        } catch (Exception ignored) {
        }
        for (String value : raw.split("[;,；]")) if (notBlank(value)) out.add(topicDisplayName(value));
    }

    /**
     * 规范主题展示名称，处理MeSH倒装词及共同前缀，不按逗号任意拆坏完整词条。
     * @param value 待校验、转换或合并的值
     * @return 按上述规则计算或读取的String结果
     */
    private String topicDisplayName(String value) {
        String clean = value.trim();
        if (clean.regionMatches(true, 0, "Databases, ", 0, "Databases, ".length()))
            return clean.substring("Databases, ".length()).trim();
        int comma = clean.indexOf(", ");
        if (comma > 0 && clean.indexOf(',', comma + 2) < 0)
            return clean.substring(comma + 2).trim() + " " + clean.substring(0, comma).trim();
        return clean;
    }

    /**
     * 从关联正文元数据提取机构汇总，保留作者原始机构数组供独立整理。
     * @param row 本次处理的一条记录
     * @param authorsJson 逐作者原始信息及作者所属机构的JSON载荷
     * @return 按上述规则计算或读取的String结果
     */
    public String relationAffiliationSummary(JsonNode row, String authorsJson) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        try {
            JsonNode authors = objectMapper.readTree(authorsJson);
            if (authors.isArray()) for (JsonNode a : authors) {
                String v = text(a, "institutionName");
                if (v != null && !v.isBlank()) values.add(v);
            }
        } catch (Exception ignored) {
        }
        if (values.isEmpty()) for (JsonNode a : row.path("authors")) {
            String v = text(a, "affiliation");
            if (v != null && !v.isBlank()) values.add(v.trim().replaceAll("\\s+", " "));
        }
        return values.isEmpty() ? text(row, "affiliationInfo") : String.join("; ", values);
    }

    /**
     * 读取指定节点中的文本字段，按本类空值约定返回结果。
     * @param node 当前JSON、XML或队列节点
     * @param field 要读取或匹配的字段名
     * @return 按上述规则计算或读取的String结果
     */
    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}

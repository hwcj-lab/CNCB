package com.example.bioplatform.research.collection.common.service.relation;

import com.example.bioplatform.research.collection.bioproject.service.impl.BioProjectPublicationParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 路径一：使用PMID详情已返回的cncbBanks（或资源原有链接），按db工厂解析链接。
 */
@Component
@RequiredArgsConstructor
public class CncbBanksDiscoveryStrategy implements RelationDiscoveryStrategy {
    /**
     * resolvers：可供选择的资源或唯一值解析器实现集合。
     */
    private final RelatedResourceResolverFactory resolvers;
    /**
     * pages：读取资源网页或接口正文的客户端。
     */
    private final RelationPageClient pages;
    /**
     * json：JSON解析与序列化组件。
     */
    private final ObjectMapper json;
    /**
     * PMID：匹配明确PubMed编号的正则表达式。
     */
    private static final Pattern PMID = Pattern.compile("(?i)(?:pubmed(?:[.]ncbi[.]nlm[.]nih[.]gov)?[/=:? ]+|pmid[^0-9]{0,20})([1-9][0-9]{5,11})");

    /**
     * 返回当前发现策略的稳定编码，用于选择路径和记录发现来源。
     * @return 按上述规则计算或读取的String结果
     */
    public String code() {
        return "CNCB_BANKS";
    }

    /**
     * 返回策略选择优先级，用于在来源专用实现和通用实现之间确定处理顺序。
     */
    public int priority() {
        return 100;
    }

    /**
     * 判断当前来源或输入能否交给本实现处理；专用实现优先于通用回退实现。
     */
    public boolean supports(RelationDiscoveryInput input) {
        return !links(input.cncbBanksJson()).isEmpty();
    }

    /**
     * 按db选择资源解析器，复用同轮资源结果；最后排除根PMID并按目标PMID去重，保留证据链接。
     */
    public RelationDiscoveryResult discover(RelationDiscoveryInput input, BeforeRequest control) throws Exception {
        Map<String, String> discovered = new LinkedHashMap<>();
        Map<String, JsonNode> references = new LinkedHashMap<>();
        var grants = json.createArrayNode();
        for (Link link : links(input.cncbBanksJson())) {
            control.run();
            var resolver = resolvers.find(link.databaseName());
            List<String> found;
            if (resolver != null) {
                var data = RelationRunCache.resource(resolver.getClass().getName() + ":" + link.accession() + ":" + link.url(),
                        () -> resolver.findRelatedPublications(link.accession(), link.url()));
                found = data.pmids();
                if (input.acceptsPublicationReferences()) for (JsonNode reference : data.publicationRecords()) {
                    if (com.example.bioplatform.research.common.PublicationValues.text(reference, "pubmedId").matches("[1-9][0-9]{5,11}"))
                        continue;
                    var seed = BioProjectPublicationParser.parse(reference, json);
                    String current = input.identifierType() + ":" + input.identifierValue();
                    if (seed.getRecordKey().equals(current) || input.identifierType().equals("SOURCE_KEY") && seed.getRecordKey().equals(input.identifierValue()))
                        continue;
                    references.putIfAbsent(seed.getRecordKey(), reference);
                }
                JsonNode funding = data.grants();
                if (funding != null && funding.isArray()) funding.forEach(item -> grants.add(item.deepCopy()));
                else if (funding != null && !funding.isNull() && !funding.isMissingNode())
                    grants.add(funding.deepCopy());
            } else {
                Set<String> values = new LinkedHashSet<>();
                Matcher match = PMID.matcher(pages.get(link.url()));
                while (match.find()) values.add(match.group(1));
                found = List.copyOf(values);
            }
            for (String pmid : found) if (!pmid.equals(input.pmid())) discovered.putIfAbsent(pmid, link.url());
        }
        return new RelationDiscoveryResult(code(), discovered, List.copyOf(references.values()), grants);
    }

    /**
     * 解析cncbBanks中的有效官方链接及其数据库名称，供后续资源工厂选择解析器。
     * @param value 待校验、转换或合并的值
     * @return 按上述规则计算或读取的List<Link>结果
     */
    private List<Link> links(String value) {
        if (value == null || value.isBlank()) return List.of();
        try {
            List<Link> links = new ArrayList<>();
            for (JsonNode item : json.readTree(value)) {
                String url = text(item, "link");
                if (url != null && (url.startsWith("https://") || url.startsWith("http://")))
                    links.add(new Link(text(item, "db"), text(item, "accession"), url));
            }
            return links;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    /**
     * 读取指定节点中的文本字段，按本类空值约定返回结果。
     * @param node 当前JSON、XML或队列节点
     * @param field 要读取或匹配的字段名
     * @return 按上述规则计算或读取的String结果
     */
    private static String text(JsonNode node, String field) {
        var value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    /**
     * cncbBanks条目的数据库名称、资源编号与官方地址。
     * @param databaseName cncbBanks提供的数据库名称，用于匹配资源解析器
     * @param accession 资源在原始数据库中的登录号
     * @param url 当前请求或资源的访问地址
     */
    private record Link(String databaseName, String accession, String url) {
    }
}

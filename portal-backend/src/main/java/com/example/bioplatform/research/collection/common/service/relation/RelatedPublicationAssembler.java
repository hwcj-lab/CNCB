package com.example.bioplatform.research.collection.common.service.relation;

import com.example.bioplatform.research.collection.common.dto.RelatedPublicationWriteRow;
import com.example.bioplatform.research.common.PublicationMetadataProcessor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;

/**
 * 固定OpenLB/PubMed结构转为固定入库行，保留逐作者署名和原始基金。
 */
@Component
@RequiredArgsConstructor
public class RelatedPublicationAssembler {
    /**
     * json：JSON解析与序列化组件。
     */
    private final ObjectMapper json;

    /**
     * 将OpenLB或PubMed的规范元数据转换为统一来源写入行，保留作者机构与原始资助载荷。
     * @param pmid 已有PubMed标识；为空时不通过标题猜补
     * @param row 本次处理的一条记录
     * @param related 是否作为关联记录处理，或关联查询结果
     * @return 按上述规则计算或读取的RelatedPublicationWriteRow结果
     */
    public RelatedPublicationWriteRow assemble(String pmid, JsonNode row, boolean related) throws Exception {
        var processor = new PublicationMetadataProcessor(json);
        String raw = json.writeValueAsString(row), authors = compact(row.path("authors"));
        int year = row.path("year").asInt();
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
        return new RelatedPublicationWriteRow(pmid, text(row, "doi"), year >= 1500 && year <= 2200 ? year : null, text(row, "journalTitle"), text(row, "title"), text(row, "abstractText"), processor.relationAffiliationSummary(row, authors), text(row, "keywordList"), authors, compact(row.path("meshHeadings")), processor.normalizeGrants(row.path("grants"), row.path("funding")), compact(row.path("cncbBanks")), raw, Optional.ofNullable(text(row, "url")).orElse("https://pubmed.ncbi.nlm.nih.gov/" + pmid + "/"), hash, related);
    }

    /**
     * 把节点转换为紧凑JSON字符串，供原始字段持久化和内容比较。
     * @param value 待校验、转换或合并的值
     * @return 按上述规则计算或读取的String结果
     */
    private String compact(JsonNode value) throws Exception {
        return json.writeValueAsString(value);
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
}

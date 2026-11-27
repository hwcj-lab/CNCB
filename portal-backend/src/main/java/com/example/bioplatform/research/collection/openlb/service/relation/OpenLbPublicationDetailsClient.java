package com.example.bioplatform.research.collection.openlb.service.relation;

import com.example.bioplatform.research.collection.common.service.impl.PubMedXmlMetadataParser;
import com.example.bioplatform.research.collection.common.service.relation.RelationPageClient;
import com.example.bioplatform.research.collection.common.service.relation.PublicationNotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 只读取已有PMID的正文元数据；不搜索、不补写缺失PMID，也不负责递归关联。
 */
@Component
@RequiredArgsConstructor
public class OpenLbPublicationDetailsClient {
    private final RelationPageClient pages;
    private final ObjectMapper json;
    @Value("${app.openlb.endpoint:https://ngdc.cncb.ac.cn/openlb/api/pmid?pmid=}")
    private String endpoint;

    /**
     * 先读OpenLB匹配PMID，再以同一PMID回退PubMed；只有正常空响应才作为无数据，限流及格式错误不得被吞掉。
     */
    public JsonNode read(String pmid) throws Exception {
        var root = json.readTree(pages.get(endpoint + pmid));
        if (!"200".equals(root.path("code").asText()))
            throw new IOException("OpenLB code " + root.path("code").asText());
        var records = root.path("result").path("data");
        if (!records.isArray()) throw new IOException("OpenLB响应结构异常：result.data不是数组");
        for (JsonNode candidate : records)
            if (pmid.equals(candidate.path("pmid").asText())) return candidate;
        var fallback = PubMedXmlMetadataParser.parse(pages.get("https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&retmode=xml&id=" + pmid), pmid, json);
        if (fallback == null) throw new PublicationNotFoundException(pmid);
        return fallback;
    }
}

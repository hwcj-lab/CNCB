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
    private final ObjectMapper json;

    public RelatedPublicationWriteRow assemble(String pmid, JsonNode row, boolean related) throws Exception {
        var processor = new PublicationMetadataProcessor(json);
        String raw = json.writeValueAsString(row), authors = compact(row.path("authors"));
        int year = row.path("year").asInt();
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
        return new RelatedPublicationWriteRow(pmid, text(row, "doi"), year >= 1500 && year <= 2200 ? year : null, text(row, "journalTitle"), text(row, "title"), text(row, "abstractText"), processor.relationAffiliationSummary(row, authors), text(row, "keywordList"), authors, compact(row.path("meshHeadings")), processor.normalizeGrants(row.path("grants"), row.path("funding")), compact(row.path("cncbBanks")), raw, Optional.ofNullable(text(row, "url")).orElse("https://pubmed.ncbi.nlm.nih.gov/" + pmid + "/"), hash, related);
    }

    private String compact(JsonNode value) throws Exception {
        return json.writeValueAsString(value);
    }

    private static String text(JsonNode node, String field) {
        var value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}

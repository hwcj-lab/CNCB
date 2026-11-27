package com.example.bioplatform.research.collection.common.service.relation;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 关联发现固定输出：文献PMID与证据地址、无PMID的原始出版记录、原始基金。
 */
public record RelationDiscoveryResult(String strategy, Map<String, String> pmids, List<JsonNode> publicationRecords,
                                      JsonNode grants) {
    public RelationDiscoveryResult {
        pmids = Collections.unmodifiableMap(new LinkedHashMap<>(pmids));
        publicationRecords = List.copyOf(publicationRecords);
    }
}

package com.example.bioplatform.research.collection.common.service;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * 资源发现返回PMID及无PMID论文的原始出版信息；队列遍历和访问去重统一处理。
 */
public record RelatedResourceData(List<String> pmids, JsonNode grants, List<JsonNode> publicationRecords) {
    public RelatedResourceData(List<String> pmids, JsonNode grants) {
        this(pmids, grants, List.of());
    }
}

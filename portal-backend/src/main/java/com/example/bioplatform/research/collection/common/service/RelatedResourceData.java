package com.example.bioplatform.research.collection.common.service;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * 资源发现返回PMID及无PMID论文的原始出版信息；队列遍历和访问去重统一处理。
 * @param pmids 待查询或已发现的PMID集合
 * @param grants 原始或合并后的资助记录
 * @param publicationRecords 缺少PMID时保留的原始出版信息集合
 */
public record RelatedResourceData(List<String> pmids, JsonNode grants, List<JsonNode> publicationRecords) {
    /**
     * 构造RelatedResourceData，注入本组件所需依赖并建立初始状态。
     * @param pmids 待查询或已发现的PMID集合
     * @param grants 原始或合并后的资助记录
     */
    public RelatedResourceData(List<String> pmids, JsonNode grants) {
        this(pmids, grants, List.of());
    }
}

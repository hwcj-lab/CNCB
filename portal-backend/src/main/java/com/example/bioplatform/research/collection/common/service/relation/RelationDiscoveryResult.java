package com.example.bioplatform.research.collection.common.service.relation;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 关联发现固定输出：文献PMID与证据地址、无PMID的原始出版记录、原始基金。
 * @param strategy 实际采用的关联发现路径编码；无可用策略时为NONE
 * @param pmids 待查询或已发现的PMID集合
 * @param publicationRecords 缺少PMID时保留的原始出版信息集合
 * @param grants 原始或合并后的资助记录
 */
public record RelationDiscoveryResult(String strategy, Map<String, String> pmids, List<JsonNode> publicationRecords,
                                      JsonNode grants) {
    /**
     * 构造RelationDiscoveryResult，注入本组件所需依赖并建立初始状态。
     * @param strategy 实际采用的关联发现路径编码；无可用策略时为NONE
     * @param pmids 待查询或已发现的PMID集合
     * @param publicationRecords 缺少PMID时保留的原始出版信息集合
     * @param grants 原始或合并后的资助记录
     */
    public RelationDiscoveryResult {
        pmids = Collections.unmodifiableMap(new LinkedHashMap<>(pmids));
        publicationRecords = List.copyOf(publicationRecords);
    }
}

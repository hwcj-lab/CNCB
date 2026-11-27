package com.example.bioplatform.research.collection.common.dto;


/**
 * 持久化关联采集队列中的一个待处理节点。
 * @param id 本对象或持久化记录的主键标识
 * @param jobId 所属持久化任务或作业ID
 * @param identifierType 唯一值类别，例如PMID、DOI或SOURCE_KEY
 * @param identifierValue 与identifierType配套的原始业务唯一值
 * @param depth 关联发现深度；当前只展开根节点一层
 */
public record RelationQueueNode(Long id, Long jobId, String identifierType, String identifierValue, Integer depth) {
}

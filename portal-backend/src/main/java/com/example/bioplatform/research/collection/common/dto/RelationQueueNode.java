package com.example.bioplatform.research.collection.common.dto;


/**
 * 持久化关联采集队列中的一个待处理节点。
 */
public record RelationQueueNode(Long id, Long jobId, String identifierType, String identifierValue, Integer depth) {
}

package com.example.bioplatform.research.collection.common.vo;


import java.time.LocalDateTime;

/**
 * 单层关联作业状态；processedCount为处理次数，discoveredCount为新增队列标识数，relationCount为关系写入累计次数，均不等于新增入库篇数。
 */
public record PublicationRelationJobStatus(
        boolean processRunning,
        boolean stopRequested,
        Long jobId,
        String databaseCode,
        String rootIdentifierType,
        String rootIdentifier,
        String status,
        Integer maxPublications,
        Long requestDelayMs,
        Integer processedCount,
        Integer discoveredCount,
        Integer relationCount,
        Long pendingCount,
        String lastError,
        LocalDateTime startedTime,
        LocalDateTime completedTime
) {
}

package com.example.bioplatform.research.collection.common.vo;


import java.time.LocalDateTime;

/**
 * 关联文献递归采集作业状态。
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

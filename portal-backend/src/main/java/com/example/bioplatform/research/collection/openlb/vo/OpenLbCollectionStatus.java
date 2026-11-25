package com.example.bioplatform.research.collection.openlb.vo;


import java.time.LocalDateTime;
import java.util.Map;

/**
 * OpenLB 采集会话状态及持久化任务队列计数。
 */
public record OpenLbCollectionStatus(
        boolean running,
        boolean stopRequested,
        String message,
        long currentDelayMs,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        Map<String, Long> taskCounts
) {
}

package com.example.bioplatform.research.collection.genbase.vo;


import java.time.LocalDateTime;

/** GenBase最近一次采集作业及当前进程运行状态。 */
public record GenBaseCollectionStatus(
        boolean running,
        boolean stopRequested,
        String message,
        Long jobId,
        String jobStatus,
        Integer nextStart,
        Integer pageSize,
        Integer recordsTotal,
        Integer rowsSeen,
        Integer rowsInserted,
        Integer attemptCount,
        String lastError,
        long currentDelayMs,
        LocalDateTime startedTime,
        LocalDateTime completedTime
) {}


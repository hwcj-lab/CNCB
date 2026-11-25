package com.example.bioplatform.research.collection.genbase.vo;


import java.time.LocalDateTime;

/**
 * Mapper使用的GenBase持久化作业快照，不包含应用进程内状态。
 */
public record GenBaseJobState(
        Long jobId,
        String jobStatus,
        Integer nextStart,
        Integer pageSize,
        Integer recordsTotal,
        Integer rowsSeen,
        Integer rowsInserted,
        Integer attemptCount,
        String lastError,
        LocalDateTime startedTime,
        LocalDateTime completedTime
) {
}


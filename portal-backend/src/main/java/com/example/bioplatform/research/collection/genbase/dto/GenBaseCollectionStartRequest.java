package com.example.bioplatform.research.collection.genbase.dto;


import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * GenBase分页采集的批大小、限速和失败保护配置。
 * @param pageSize 每页记录数
 * @param minDelayMs 最小请求间隔，单位毫秒
 * @param maxAttempts 允许尝试的次数上限
 * @param stopAfterConsecutiveThrottles 连续限流或失败达到此阈值时停止当前采集，保留断点
 */
public record GenBaseCollectionStartRequest(
        @Min(10) @Max(500) Integer pageSize,
        @Min(200) @Max(10000) Long minDelayMs,
        @Min(1) @Max(10) Integer maxAttempts,
        @Min(1) @Max(20) Integer stopAfterConsecutiveThrottles
) {
    /**
     * 默认每页100条，在减少数据库往返和控制响应大小之间折中。
     */
    public int pageSizeOrDefault() {
        return pageSize == null ? 100 : pageSize;
    }

    /**
     * 默认每次请求至少间隔500毫秒。
     */
    public long minDelayMsOrDefault() {
        return minDelayMs == null ? 500L : minDelayMs;
    }

    /**
     * 当前页默认最多尝试3次。
     */
    public int maxAttemptsOrDefault() {
        return maxAttempts == null ? 3 : maxAttempts;
    }

    /**
     * 连续限流或超时默认达到5次后自动停止。
     */
    public int stopThresholdOrDefault() {
        return stopAfterConsecutiveThrottles == null ? 5 : stopAfterConsecutiveThrottles;
    }
}


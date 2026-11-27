package com.example.bioplatform.research.collection.openlb.dto;


import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * OpenLB采集的并发、批次、限速及失败保护参数，不包含统计快照选项。
 * @param concurrency 同时执行上游读取的工作线程数量，由请求及默认配置限定
 * @param batchSize 每批读取或写入的记录数量上限
 * @param minDelayMs 最小请求间隔，单位毫秒
 * @param maxAttempts 允许尝试的次数上限
 * @param stopAfterConsecutiveThrottles 连续限流或失败达到此阈值时停止当前采集，保留断点
 */
public record OpenLbCollectionStartRequest(
        @Min(1) @Max(8) Integer concurrency,
        @Min(1) @Max(200) Integer batchSize,
        @Min(100) @Max(10000) Long minDelayMs,
        @Min(1) @Max(10) Integer maxAttempts,
        @Min(1) @Max(20) Integer stopAfterConsecutiveThrottles
) {
    /**
     * 未传并发数时使用保守的3线程，避免上游限流。
     */
    public int concurrencyOrDefault() {
        return concurrency == null ? 3 : concurrency;
    }

    /**
     * 未传批大小时每次领取60个PMID，用于控制内存和事务大小。
     */
    public int batchSizeOrDefault() {
        return batchSize == null ? 60 : batchSize;
    }

    /**
     * 全局请求最小间隔默认350ms，所有工作线程共享。
     */
    public long minDelayMsOrDefault() {
        return minDelayMs == null ? 350L : minDelayMs;
    }

    /**
     * 单PMID默认最多请求3次，达到上限后转为FAILED。
     */
    public int maxAttemptsOrDefault() {
        return maxAttempts == null ? 3 : maxAttempts;
    }

    /**
     * 连续限流默认达到5次时自动停止整个采集会话。
     */
    public int stopThresholdOrDefault() {
        return stopAfterConsecutiveThrottles == null ? 5 : stopAfterConsecutiveThrottles;
    }
}

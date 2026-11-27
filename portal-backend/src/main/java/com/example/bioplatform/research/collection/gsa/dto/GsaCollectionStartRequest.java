package com.example.bioplatform.research.collection.gsa.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * GSA独立主采集配置；上游为静态全量JSON，batchSize控制本地写入及补全批次。
 * @param batchSize 每批读取或写入的记录数量上限
 * @param minDelayMs 最小请求间隔，单位毫秒
 * @param maxAttempts 允许尝试的次数上限
 */
public record GsaCollectionStartRequest(@Min(1) @Max(200) Integer batchSize,
                                        @Min(350) @Max(10000) Long minDelayMs, @Min(1) @Max(5) Integer maxAttempts) {
    /**
     * 规范化批次大小并限制到本采集器允许的范围，或接收一次资源批次输入。
     * @return 按上述规则计算或读取的int结果
     */
    public int batch() {
        return batchSize == null ? 100 : batchSize;
    }

    /**
     * 规范化请求间隔配置，避免小于采集器的限速下限。
     * @return 按上述规则计算或读取的long结果
     */
    public long delay() {
        return minDelayMs == null ? 800 : minDelayMs;
    }

    /**
     * 规范化失败尝试次数配置，缺失时使用本采集器默认值。
     * @return 按上述规则计算或读取的int结果
     */
    public int attempts() {
        return maxAttempts == null ? 3 : maxAttempts;
    }
}

package com.example.bioplatform.research.collection.gsa.dto;

import jakarta.validation.constraints.*;

/**
 * GSA独立主采集配置；上游为静态全量JSON，batchSize控制本地写入及补全批次。
 */
public record GsaCollectionStartRequest(@Min(1) @Max(200) Integer batchSize,
                                        @Min(350) @Max(10000) Long minDelayMs, @Min(1) @Max(5) Integer maxAttempts) {
    public int batch() {
        return batchSize == null ? 100 : batchSize;
    }

    public long delay() {
        return minDelayMs == null ? 800 : minDelayMs;
    }

    public int attempts() {
        return maxAttempts == null ? 3 : maxAttempts;
    }
}

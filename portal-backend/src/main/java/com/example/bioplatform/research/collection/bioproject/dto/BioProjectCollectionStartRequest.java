package com.example.bioplatform.research.collection.bioproject.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * BIOPROJECT独立主采集配置；上游为静态全量JSON，batchSize控制本地写入及补全批次。
 */
public record BioProjectCollectionStartRequest(@Min(1) @Max(200) Integer batchSize,
                                               @Min(350) @Max(10000) Long minDelayMs,
                                               @Min(1) @Max(5) Integer maxAttempts,
                                               @Size(max = 1000) java.util.List<@Pattern(regexp = "PRJ(?:CA|NA|EB|DA)[A-Z0-9]+") String> projectAccessions) {
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

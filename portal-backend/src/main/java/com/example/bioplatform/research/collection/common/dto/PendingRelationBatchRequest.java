package com.example.bioplatform.research.collection.common.dto;


import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * 从来源表自动领取待关联文献的批量作业参数。
 * 不需要填写 PMID、paperId 或 DOI，唯一值由数据库查询得到。
 */
public record PendingRelationBatchRequest(
        @NotBlank String databaseCode,
        @Min(1) @Max(5000) Integer batchSize,
        Boolean includeFailed,
        @Min(1) @Max(100000) Integer maxPublications,
        @Min(100) @Max(60000) Long requestDelayMs
) {
    public int batchSizeOrDefault() {
        return batchSize == null ? 500 : batchSize;
    }

    public boolean includeFailedOrDefault() {
        return Boolean.TRUE.equals(includeFailed);
    }

    public int maxPublicationsOrDefault() {
        return maxPublications == null ? 1000 : maxPublications;
    }

    public long requestDelayMsOrDefault() {
        return requestDelayMs == null ? 800L : requestDelayMs;
    }
}

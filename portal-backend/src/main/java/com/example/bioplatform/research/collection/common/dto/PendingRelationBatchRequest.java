package com.example.bioplatform.research.collection.common.dto;


import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * 从来源表自动领取待关联文献的批量作业参数。
 * 不需要填写 PMID、paperId 或 DOI，唯一值由数据库查询得到。
 * @param databaseCode 数据库来源编码或目录业务编码
 * @param batchSize 每批读取或写入的记录数量上限
 * @param includeFailed 是否重新领取失败记录
 * @param maxPublications 本轮处理的安全数量预算
 * @param requestDelayMs 作业配置的上游请求间隔，单位毫秒
 */
public record PendingRelationBatchRequest(
        @NotBlank String databaseCode,
        @Min(1) @Max(5000) Integer batchSize,
        Boolean includeFailed,
        @Min(1) @Max(100000) Integer maxPublications,
        @Min(100) @Max(60000) Long requestDelayMs
) {
    /**
     * 读取批次大小；未传时使用默认值，并限制在允许范围内。
     * @return 按上述规则计算或读取的int结果
     */
    public int batchSizeOrDefault() {
        return batchSize == null ? 500 : batchSize;
    }

    /**
     * 读取是否领取失败记录的开关；未传时使用接口默认口径。
     * @return true表示满足上述条件，false表示不满足
     */
    public boolean includeFailedOrDefault() {
        return Boolean.TRUE.equals(includeFailed);
    }

    /**
     * 读取本轮处理安全上限；该值限制处理预算，不表示递归深度。
     * @return 按上述规则计算或读取的int结果
     */
    public int maxPublicationsOrDefault() {
        return maxPublications == null ? 1000 : maxPublications;
    }

    /**
     * 读取上游请求间隔，单位毫秒；未传时使用默认限速配置。
     * @return 按上述规则计算或读取的long结果
     */
    public long requestDelayMsOrDefault() {
        return requestDelayMs == null ? 800L : requestDelayMs;
    }
}

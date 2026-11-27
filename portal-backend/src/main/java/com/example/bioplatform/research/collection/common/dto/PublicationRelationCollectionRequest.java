package com.example.bioplatform.research.collection.common.dto;


import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.Map;

/**
 * 单篇文献重新采集及单层关联发现请求。
 * identifiers允许不同数据库使用不同唯一键，例如OpenLB使用pmid，GenBase可使用paperId、pmid或doi。
 * @param databaseCode 数据库来源编码或目录业务编码
 * @param identifiers 请求提供的唯一值名称与内容映射
 * @param refreshPublication 是否重新读取根文献正文
 * @param refreshRelations 是否重新执行关联发现
 * @param maxPublications 本轮处理的安全数量预算
 * @param requestDelayMs 作业配置的上游请求间隔，单位毫秒
 */
public record PublicationRelationCollectionRequest(
        @NotBlank String databaseCode,
        @NotEmpty Map<String, String> identifiers,
        Boolean refreshPublication,
        Boolean refreshRelations,
        @Min(1) @Max(100000) Integer maxPublications,
        @Min(100) @Max(60000) Long requestDelayMs
) {
    /**
     * 读取是否刷新根文献正文的开关，缺省值由接口约定决定。
     * @return true表示满足上述条件，false表示不满足
     */
    public boolean refreshPublicationOrDefault() {
        return Boolean.TRUE.equals(refreshPublication);
    }

    /**
     * 读取是否重新发现关联文献的开关，与刷新正文分别控制。
     * @return true表示满足上述条件，false表示不满足
     */
    public boolean refreshRelationsOrDefault() {
        return refreshRelations == null || refreshRelations;
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
        return requestDelayMs == null ? 500L : requestDelayMs;
    }
}

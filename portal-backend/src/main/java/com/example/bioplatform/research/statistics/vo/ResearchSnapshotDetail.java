package com.example.bioplatform.research.statistics.vo;


import lombok.Data;

import java.time.LocalDateTime;

/**
 * 一个可审计的科研统计历史版本及其字段覆盖率。
 */
@Data
public class ResearchSnapshotDetail {
    private Long id;
    private Long versionNo;
    private String snapshotName;
    private String status;
    private String sourceCodes;
    private Integer sourceCount;
    private Boolean includeRelated;
    /**
     * true：纳入无原生文献资源；false：只统计文献。
     */
    private Boolean includeResources;
    /**
     * 历史兼容字段：当前口径的统计对象总数。
     */
    private Integer publicationCount;
    private Long resourceCount;

    public long getRecordCount() {
        return publicationCount == null ? 0 : publicationCount;
    }

    public long getLiteratureCount() {
        return getRecordCount() - (resourceCount == null ? 0 : resourceCount);
    }

    private Integer yearCoverageCount;
    private Integer topicCoverageCount;
    private Integer authorCoverageCount;
    private Integer institutionCoverageCount;
    private Integer fundCoverageCount;
    private String errorMessage;
    private LocalDateTime startedTime;
    private LocalDateTime completedTime;
}

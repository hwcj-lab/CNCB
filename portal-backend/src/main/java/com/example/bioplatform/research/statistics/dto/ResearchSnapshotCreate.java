package com.example.bioplatform.research.statistics.dto;


import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建统计快照时传给 Mapper 的可回填主键参数。
 */
@Data
public class ResearchSnapshotCreate {
    private Long id;
    private Long versionNo;
    private String snapshotName;
    private String sourceCodes;
    private Integer sourceCount;
    private Boolean includeRelated;
    /**
     * true：纳入无原生文献资源；false：只统计文献。
     */
    private Boolean includeResources;
    private LocalDateTime startedTime;
}

package com.example.bioplatform.research.statistics.dto;


import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建统计快照时传给 Mapper 的可回填主键参数。
 */
@Data
public class ResearchSnapshotCreate {
    /**
     * id：本对象或持久化记录的主键标识。
     */
    private Long id;
    /**
     * versionNo：对外展示的快照版本序号。
     */
    private Long versionNo;
    /**
     * snapshotName：快照展示名称。
     */
    private String snapshotName;
    /**
     * sourceCodes：参与本次处理的来源编码集合或拼接值。
     */
    private String sourceCodes;
    /**
     * sourceCount：本次快照包含的来源数量。
     */
    private Integer sourceCount;
    /**
     * includeRelated：是否将非本库的关联记录纳入当前处理范围。
     */
    private Boolean includeRelated;
    /**
     * true：纳入无原生文献资源；false：只统计文献。
     */
    private Boolean includeResources;
    /**
     * startedTime：任务开始或恢复执行的时间。
     */
    private LocalDateTime startedTime;
}

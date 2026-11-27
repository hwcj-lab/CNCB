package com.example.bioplatform.research.statistics.vo;


import lombok.Data;

import java.time.LocalDateTime;

/**
 * 一个可审计的科研统计历史版本及其字段覆盖率。
 */
@Data
public class ResearchSnapshotDetail {
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
     * status：当前对象的业务状态，由对应任务或目录状态机定义。
     */
    private String status;
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
     * 历史兼容字段：当前口径的统计对象总数。
     */
    private Integer publicationCount;
    /**
     * resourceCount：纳入当前快照的无文献资源数。
     */
    private Long resourceCount;

    /**
     * 返回快照publicationCount字段代表的统计对象总数，可能同时包含所选无文献资源。
     * @return 按上述规则计算或读取的long结果
     */
    public long getRecordCount() {
        return publicationCount == null ? 0 : publicationCount;
    }

    /**
     * 根据总对象数和资源数计算文献数量，避免把资源混称为文献。
     * @return 按上述规则计算或读取的long结果
     */
    public long getLiteratureCount() {
        return getRecordCount() - (resourceCount == null ? 0 : resourceCount);
    }

    /**
     * yearCoverageCount：具有有效统计年份的去重统计对象数量。
     */
    private Integer yearCoverageCount;
    /**
     * topicCoverageCount：具有主题归类或主题信息的去重统计对象数量。
     */
    private Integer topicCoverageCount;
    /**
     * authorCoverageCount：具有作者信息的去重统计对象数量。
     */
    private Integer authorCoverageCount;
    /**
     * institutionCoverageCount：具有机构信息的去重统计对象数量。
     */
    private Integer institutionCoverageCount;
    /**
     * fundCoverageCount：具有可用资助方信息的去重统计对象数量。
     */
    private Integer fundCoverageCount;
    /**
     * errorMessage：任务失败原因或异常说明。
     */
    private String errorMessage;
    /**
     * startedTime：任务开始或恢复执行的时间。
     */
    private LocalDateTime startedTime;
    /**
     * completedTime：任务完成、停止或失败的时间；进行中可为空。
     */
    private LocalDateTime completedTime;
}

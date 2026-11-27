package com.example.bioplatform.research.statistics.prepared.model;

import lombok.Data;

/**
 * 快照冻结的整理修订、来源名称和总统计分类范围。
 */
@Data
public class PreparedSnapshotConfig {
    /**
     * snapshotId：统计快照主键。
     * preparationRevision：快照冻结使用的成功整理修订号。
     */
    private Long snapshotId, preparationRevision;
    /**
     * sourceNamesJson：快照冻结的来源编码与显示名称映射。
     * topicScopesJson：快照冻结的总分类与来源范围映射。
     */
    private String sourceNamesJson, topicScopesJson;
}

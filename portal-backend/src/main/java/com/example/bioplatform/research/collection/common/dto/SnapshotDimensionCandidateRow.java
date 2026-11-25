package com.example.bioplatform.research.collection.common.dto;


import lombok.Data;

/** 快照构建批次中的单个“维度—文献”候选关系，批量写入暂存表后由数据库汇总。 */
@Data
public class SnapshotDimensionCandidateRow {
    private long snapshotId;
    private String scopeCode;
    private String dimensionType;
    private byte[] dimensionHash;
    private String dimensionKey;
    private String displayName;
    private String authorName;
    private String institutionName;
    private int statYear;
    private String publicationKey;
}

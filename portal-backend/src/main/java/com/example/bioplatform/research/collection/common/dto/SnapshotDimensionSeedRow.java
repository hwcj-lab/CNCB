package com.example.bioplatform.research.collection.common.dto;


import lombok.Data;

/** 快照开始时预建少量年度和主题维度，供扫描期间直接累计正式穿透键。 */
@Data
public class SnapshotDimensionSeedRow {
    private long id;
    private long snapshotId;
    private String scopeCode;
    private String dimensionType;
    private String dimensionKey;
    private byte[] dimensionHash;
    private String displayName;
    private int statYear;
    private long publicationCount;
}

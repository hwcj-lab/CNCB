package com.example.bioplatform.research.collection.common.dto;


import lombok.Data;

/** 年度或主题维度对应的一条正式文献穿透关系。 */
@Data
public class SnapshotDimensionPublicationRow {
    /**
     * dimensionId：快照维度统计点ID，供图表穿透查询使用。
     */
    private long dimensionId;
    /**
     * publicationKey：统一文献身份键，用于统计去重和穿透定位。
     */
    private String publicationKey;
}

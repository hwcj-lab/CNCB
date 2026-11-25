package com.example.bioplatform.research.statistics.vo;


import lombok.Data;

/** 快照中一个可直接用于图表的预聚合数据点。 */
@Data
public class ResearchSnapshotDimension {
    private Long id;
    private String scopeCode;
    private String dimensionType;
    private String dimensionKey;
    private String displayName;
    private String authorName;
    private String institutionName;
    private Integer statYear;
    private Integer publicationCount;
}

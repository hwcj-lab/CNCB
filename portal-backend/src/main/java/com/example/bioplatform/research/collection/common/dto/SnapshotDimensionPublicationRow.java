package com.example.bioplatform.research.collection.common.dto;


import lombok.Data;

/** 年度或主题维度对应的一条正式文献穿透关系。 */
@Data
public class SnapshotDimensionPublicationRow {
    private long dimensionId;
    private String publicationKey;
}

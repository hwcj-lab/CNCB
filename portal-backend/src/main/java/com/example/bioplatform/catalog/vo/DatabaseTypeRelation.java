package com.example.bioplatform.catalog.vo;

import lombok.Data;

/** 批量装配推荐分组时使用的数据库—数据类型轻量关系。 */
@Data
public class DatabaseTypeRelation {
    /**
     * databaseId：本地数据库目录记录的主键。
     */
    private Long databaseId;
    /**
     * dataTypeCode：目录数据类型的稳定编码。
     */
    private String dataTypeCode;
}

package com.example.bioplatform.catalog.vo;

import lombok.Data;

/** 批量装配推荐分组时使用的数据库—数据类型轻量关系。 */
@Data
public class DatabaseTypeRelation {
    private Long databaseId;
    private String dataTypeCode;
}

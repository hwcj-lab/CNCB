package com.example.bioplatform.catalog.vo;

import com.example.bioplatform.catalog.entity.DataType;
import com.example.bioplatform.catalog.entity.DatabaseResource;

import java.util.List;

/**
 * 推荐页面的一级分组：一个数据类型及其非空数据库列表。
 */
public record ResourceRecommendationGroup(DataType dataType, List<DatabaseResource> databases) {
}

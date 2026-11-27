package com.example.bioplatform.catalog.vo;

import com.example.bioplatform.catalog.entity.DataType;
import com.example.bioplatform.catalog.entity.DatabaseResource;

import java.util.List;

/**
 * 推荐页面的一级分组：一个数据类型及其非空数据库列表。
 * @param dataType 目录数据类型编码或对象
 * @param databases 数据库目录结果或数据库目录持久层
 */
public record ResourceRecommendationGroup(DataType dataType, List<DatabaseResource> databases) {
}

package com.example.bioplatform.catalog.service;

import com.example.bioplatform.catalog.dto.SearchDatabaseHit;
import com.example.bioplatform.catalog.entity.DataType;
import com.example.bioplatform.catalog.entity.DatabaseResource;
import com.example.bioplatform.catalog.entity.OnlineTool;
import com.example.bioplatform.catalog.vo.ResourceRecommendationGroup;

import java.util.List;

/**
 * 数据库资源与在线工具目录服务。
 * 负责组合数据类型、数据库及工具关系，不在控制器中硬编码目录数据。
 */
public interface CatalogService {
    /**
     * 查询适合页面一次性展示的推荐分组，空分组不会返回。
     */
    List<ResourceRecommendationGroup> recommendationGroups(String keyword);

    /**
     * 将 CNCB 跨库搜索命中项转换为本平台已有数据库及工具推荐分组。
     */
    List<ResourceRecommendationGroup> recommendationsForSearchResults(List<SearchDatabaseHit> hits);

    /**
     * 查询所有启用且可展示的数据类型。
     */
    List<DataType> types();

    /**
     * 按数据类型和关键词查询数据库目录。
     */
    List<DatabaseResource> databases(String type, String keyword);

    /**
     * 查询单个数据库的完整详情。
     */
    DatabaseResource detail(Long id);

    /**
     * 查询数据库关联的原生工具或兼容推荐工具。
     */
    List<OnlineTool> tools(Long id, String type);

    /**
     * 查询单个在线工具详情。
     */
    OnlineTool tool(Long id);
}

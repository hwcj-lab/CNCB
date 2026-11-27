package com.example.bioplatform.catalog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.bioplatform.catalog.dto.SearchDatabaseHit;
import com.example.bioplatform.catalog.entity.DataType;
import com.example.bioplatform.catalog.entity.DatabaseResource;
import com.example.bioplatform.catalog.entity.OnlineTool;
import com.example.bioplatform.catalog.mapper.DataTypeMapper;
import com.example.bioplatform.catalog.mapper.DatabaseMapper;
import com.example.bioplatform.catalog.mapper.ToolMapper;
import com.example.bioplatform.catalog.service.CatalogService;
import com.example.bioplatform.catalog.vo.DatabaseTypeRelation;
import com.example.bioplatform.catalog.vo.ResourceRecommendationGroup;
import com.example.bioplatform.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 目录服务实现，使用批量关系查询组装页面模型并避免 N+1 查询。 */
@Service
@RequiredArgsConstructor
public class CatalogServiceImpl implements CatalogService {
    /**
     * databases：数据库目录结果或数据库目录持久层。
     */
    private final DatabaseMapper databases;
    /**
     * tools：在线工具结果或工具持久层。
     */
    private final ToolMapper tools;
    /**
     * types：数据类型结果或类型持久层。
     */
    private final DataTypeMapper types;

    /** 查询启用的数据类型，排序由数据库字段控制。 */
    public List<DataType> types() {
        return types.selectList(new QueryWrapper<DataType>().eq("is_visible", true).eq("status", "ENABLED").orderByAsc("sort_order").orderByAsc("id"));
    }

    /** 一次查询并组装全部非空类型分组，同时追加未分类数据库。 */
    public List<ResourceRecommendationGroup> recommendationGroups(String keyword) {
        var visibleTypes = types();
        var rows = databases.findResources(null, keyword);
        attachTools(rows);
        var grouped = groupResourcesByType(rows);
        var result = new ArrayList<>(visibleTypes.stream()
                .map(type -> new ResourceRecommendationGroup(type, grouped.getOrDefault(type.getCode(), List.of())))
                .filter(group -> !group.databases().isEmpty())
                .toList());
        appendOtherGroup(result, rows, grouped);
        return result;
    }

    /** 按上游 dbId 批量匹配搜索结果，忽略无法识别的命中项。 */
    public List<ResourceRecommendationGroup> recommendationsForSearchResults(List<SearchDatabaseHit> hits) {
        var dbIds = hits.stream()
                .map(SearchDatabaseHit::getId)
                .filter(id -> id != null && !id.isBlank())
                .map(id -> id.trim().toLowerCase(Locale.ROOT))
                .distinct()
                .toList();

        if (dbIds.isEmpty()) return List.of();
        var visibleTypes = types();
        var rows = databases.findResourcesByDbIds(null, dbIds);
        attachTools(rows);
        var grouped = groupResourcesByType(rows);
        var result = new ArrayList<>(visibleTypes.stream()
                .map(type -> new ResourceRecommendationGroup(type, grouped.getOrDefault(type.getCode(), List.of())))
                .filter(group -> !group.databases().isEmpty())
                .toList());
        appendOtherGroup(result, rows, grouped);
        return result;
    }

    /** 拒绝不存在或未启用的数据类型编码。 */
    private void validateType(String type) {
        if (type != null && types.selectCount(new QueryWrapper<DataType>().eq("code", type)) == 0)
            throw new BusinessException(HttpStatus.BAD_REQUEST, "UNKNOWN_DATA_TYPE", "未知数据类型");
    }

    /** 查询数据库并批量装配工具列表。 */
    public List<DatabaseResource> databases(String type, String keyword) {
        validateType(type);
        var rows = databases.findResources(type, keyword);
        attachTools(rows);
        return rows;
    }

    /** 将一次批量工具查询结果按数据库主键回填。 */
    private void attachTools(List<DatabaseResource> rows) {
        if (rows.isEmpty()) return;
        var databaseIds = rows.stream().map(DatabaseResource::getId).toList();
        var toolsByDatabase = databases.findToolsByDatabaseIds(databaseIds).stream()
                .collect(Collectors.groupingBy(OnlineTool::getRelationDatabaseId));
        rows.forEach(row -> row.setTools(toolsByDatabase.getOrDefault(row.getId(), List.of())));
    }

    /** 根据数据库—类型关系在内存中构造类型分组。 */
    private Map<String, List<DatabaseResource>> groupResourcesByType(List<DatabaseResource> rows) {
        if (rows.isEmpty()) return Map.of();
        var byId = rows.stream().collect(Collectors.toMap(DatabaseResource::getId, Function.identity()));
        return databases.findTypeRelationsByDatabaseIds(rows.stream().map(DatabaseResource::getId).toList()).stream()
                .filter(relation -> byId.containsKey(relation.getDatabaseId()))
                .collect(Collectors.groupingBy(
                        DatabaseTypeRelation::getDataTypeCode,
                        Collectors.mapping(relation -> byId.get(relation.getDatabaseId()), Collectors.toList())
                ));
    }

    /** 将没有任何标签的数据库放入“其他”，不改变已有四类分组。 */
    private void appendOtherGroup(List<ResourceRecommendationGroup> result,
                                  List<DatabaseResource> rows,
                                  Map<String, List<DatabaseResource>> grouped) {
        var classifiedIds = new HashSet<Long>();
        grouped.values().forEach(resources -> resources.forEach(resource -> classifiedIds.add(resource.getId())));
        var unclassified = rows.stream().filter(resource -> !classifiedIds.contains(resource.getId())).toList();
        if (unclassified.isEmpty()) return;
        var other = new DataType();
        other.setCode("OTHER");
        other.setName("其他");
        other.setDescription("暂未关联到当前可见数据类型的数据库");
        other.setSortOrder(Integer.MAX_VALUE);
        other.setIsVisible(true);
        other.setStatus("ENABLED");
        result.add(new ResourceRecommendationGroup(other, unclassified));
    }

    /** 查询数据库主记录，并补充类型及工具详情。 */
    public DatabaseResource detail(Long id) {
        var row = databases.selectById(id);
        if (row == null) throw new BusinessException(HttpStatus.NOT_FOUND, "DATABASE_NOT_FOUND", "数据库资源不存在");
        row.setDataTypes(databases.findTypes(id));
        row.setTools(databases.findTools(id));
        return row;
    }

    /** 查询数据库工具；type 参数为后续兼容过滤预留。 */
    public List<OnlineTool> tools(Long id, String type) {
        if (databases.selectById(id) == null)
            throw new BusinessException(HttpStatus.NOT_FOUND, "DATABASE_NOT_FOUND", "数据库资源不存在");
        return databases.findTools(id);
    }

    /** 查询工具详情，不存在时返回明确的业务错误。 */
    public OnlineTool tool(Long id) {
        var row = tools.selectById(id);
        if (row == null) throw new BusinessException(HttpStatus.NOT_FOUND, "TOOL_NOT_FOUND", "工具不存在");
        return row;
    }
}

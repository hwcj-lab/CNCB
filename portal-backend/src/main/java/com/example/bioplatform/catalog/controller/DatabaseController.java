package com.example.bioplatform.catalog.controller;

import com.example.bioplatform.common.ApiResponse;
import com.example.bioplatform.catalog.dto.SearchDatabaseHit;
import com.example.bioplatform.catalog.entity.*;
import com.example.bioplatform.catalog.service.CatalogService;
import com.example.bioplatform.catalog.vo.ResourceRecommendationGroup;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
/** 数据资源目录与工具推荐接口。 */
public class DatabaseController {
    private final CatalogService service;

    @Operation(summary = "返回存在关联数据库的可见数据类型、数据库及其关联工具；支持后端关键词过滤，空分类不返回")
    @GetMapping("/resource-recommendations")
    /** 返回页面首屏需要的完整推荐分组。 */
    public ApiResponse<List<ResourceRecommendationGroup>> recommendations(
            @RequestParam(required = false) @Size(max = 100) String keyword) {
        return ApiResponse.ok(service.recommendationGroups(keyword));
    }

    @Operation(summary = "根据 CNCB 跨库搜索的 databases 列表返回有命中数据库的分类及关联工具；空分类不返回")
    @PostMapping("/resource-recommendations/search-results")
    /** 接收 CNCB 搜索结果，并仅返回本地能够识别的数据库。 */
    public ApiResponse<List<ResourceRecommendationGroup>> recommendationsForSearchResults(
            @RequestBody @Size(min = 1, max = 2000) List<@Valid SearchDatabaseHit> hits) {
        return ApiResponse.ok(service.recommendationsForSearchResults(hits));
    }

    @Operation(summary = "查询可见数据类型")
    @GetMapping("/data-types")
    /** 查询可用于导航的数据类型。 */
    public ApiResponse<List<DataType>> types() {
        return ApiResponse.ok(service.types());
    }

    @Operation(summary = "按数据类型和关键词查询数据库，并返回数据库关联工具")
    @GetMapping("/databases")
    /** 查询数据库目录；保留该接口供独立检索与管理页面复用。 */
    public ApiResponse<List<DatabaseResource>> list(@RequestParam(required = false) @Size(max = 40) String dataType, @RequestParam(required = false) @Size(max = 100) String keyword) {
        return ApiResponse.ok(service.databases(dataType, keyword));
    }

    @Operation(summary = "数据库详情和完整证据状态")
    @GetMapping("/databases/{id}")
    /** 查询数据库详情及证据字段。 */
    public ApiResponse<DatabaseResource> detail(@PathVariable @Min(1) Long id) {
        return ApiResponse.ok(service.detail(id));
    }

    @Operation(summary = "查询数据库关联工具")
    @GetMapping("/databases/{id}/tools")
    /** 查询指定数据库关联的在线工具。 */
    public ApiResponse<List<OnlineTool>> tools(@PathVariable @Min(1) Long id) {
        return ApiResponse.ok(service.tools(id, null));
    }
}

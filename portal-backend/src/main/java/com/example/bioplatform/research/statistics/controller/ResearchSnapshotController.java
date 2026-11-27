package com.example.bioplatform.research.statistics.controller;

import com.example.bioplatform.common.ApiResponse;
import com.example.bioplatform.research.collection.common.vo.ResearchPublicationSourceOption;
import com.example.bioplatform.research.statistics.dto.ResearchSnapshotBuildRequest;
import com.example.bioplatform.research.statistics.dto.ResearchSnapshotRenameRequest;
import com.example.bioplatform.research.statistics.service.ResearchSnapshotService;
import com.example.bioplatform.research.statistics.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * OpenLB、GenBase及后续来源的统一统计快照入口。
 */
@RestController
@RequestMapping("/api/research-snapshots")
@RequiredArgsConstructor
@Validated
@Tag(name = "科研统计快照", description = "按需汇总所有已注册文献来源，并保留可追溯历史版本")
public class ResearchSnapshotController {
    /**
     * service：控制器依赖的业务服务，不在控制器中实现持久化逻辑。
     */
    private final ResearchSnapshotService service;

    /**
     * 手工触发一个新版本；后台构建期间旧成功版本始终可查询。
     */
    @PostMapping("/build")
    @Operation(summary = "创建新统计版本")
    public ApiResponse<ResearchSnapshotStatus> build(
            @RequestBody(required = false) @Valid ResearchSnapshotBuildRequest request) {
        return ApiResponse.ok(service.startBuild(request));
    }

    /**
     * 返回系统自动纳入的全部本地文献来源；仅供展示，不再作为快照选择项。
     */
    @GetMapping("/sources")
    @Operation(summary = "查询自动参与统计的来源")
    public ApiResponse<List<ResearchPublicationSourceOption>> sources() {
        return ApiResponse.ok(service.sourceOptions());
    }

    /**
     * 请求安全停止后台构建；不会影响最近成功版本。
     */
    @PostMapping("/stop")
    @Operation(summary = "安全停止统计构建")
    public ApiResponse<ResearchSnapshotStatus> stop() {
        return ApiResponse.ok(service.stopBuild());
    }

    /**
     * 供页面轮询构建进度。
     */
    @GetMapping("/status")
    @Operation(summary = "查询统计构建状态")
    public ApiResponse<ResearchSnapshotStatus> status() {
        return ApiResponse.ok(service.status());
    }

    /**
     * 返回最近100个历史版本，包括失败版本以便排错。
     */
    @GetMapping
    @Operation(summary = "查询统计历史版本")
    public ApiResponse<List<ResearchSnapshotDetail>> versions() {
        return ApiResponse.ok(service.versions());
    }

    /**
     * 页面默认使用最近成功版本，不会读到半成品。
     */
    @GetMapping("/latest")
    @Operation(summary = "查询最近成功统计版本")
    public ApiResponse<ResearchSnapshotDetail> latest() {
        return ApiResponse.ok(service.latest());
    }

    /**
     * 前端首屏总入口：一次返回最近版本及分组后的全部仪表盘数据。
     */
    @GetMapping("/latest/result")
    @Operation(summary = "一次获取最近版本的仪表盘总汇")
    public ApiResponse<ResearchSnapshotResult> latestResult(
            @RequestParam(defaultValue = "50") @Min(1) @Max(1000) int limitPerDimension) {
        return ApiResponse.ok(service.latestResult(limitPerDimension));
    }

    /**
     * 查询指定历史版本的元数据和覆盖率。
     */
    @GetMapping("/{snapshotId}")
    @Operation(summary = "查询指定统计版本")
    public ApiResponse<ResearchSnapshotDetail> detail(@PathVariable long snapshotId) {
        return ApiResponse.ok(service.detail(snapshotId));
    }

    /**
     * 历史版本回放总入口，数据结构与最近版本完全一致。
     */
    @GetMapping("/{snapshotId}/result")
    @Operation(summary = "一次获取指定版本的仪表盘总汇")
    public ApiResponse<ResearchSnapshotResult> result(
            @PathVariable long snapshotId,
            @RequestParam(defaultValue = "50") @Min(1) @Max(1000) int limitPerDimension) {
        return ApiResponse.ok(service.result(snapshotId, limitPerDimension));
    }

    /**
     * 快照内数据库列表由实际注册来源和本轮数据动态产生，不固定为OpenLB/GenBase。
     */
    @GetMapping("/{snapshotId}/databases")
    @Operation(summary = "查询指定快照的数据库统计入口")
    public ApiResponse<List<ResearchSnapshotDatabaseOption>> databases(@PathVariable long snapshotId) {
        return ApiResponse.ok(service.databases(snapshotId));
    }

    /**
     * 从研究主题进入数据库总览时返回属于该主题的实际文献来源。
     */
    @GetMapping("/{snapshotId}/topics/{topicCode}/databases")
    @Operation(summary = "查询研究主题下已接入的数据库")
    public ApiResponse<List<ResearchSnapshotDatabaseOption>> topicDatabases(
            @PathVariable long snapshotId, @PathVariable String topicCode) {
        return ApiResponse.ok(service.topicDatabases(snapshotId, topicCode));
    }

    /**
     * 分数据库接口与总统计接口分离，避免数据库增加后单次响应无限扩大。
     */
    @GetMapping("/{snapshotId}/databases/{databaseCode}/result")
    @Operation(summary = "查询指定数据库在该快照中的五维统计")
    public ApiResponse<ResearchSnapshotResult> databaseResult(
            @PathVariable long snapshotId, @PathVariable String databaseCode,
            @RequestParam(defaultValue = "50") @Min(1) @Max(1000) int limitPerDimension) {
        return ApiResponse.ok(service.databaseResult(snapshotId, databaseCode, limitPerDimension));
    }

    /**
     * 查询YEAR/TOPIC/AUTHOR/INSTITUTION/FUND中的一个维度。
     */
    @GetMapping("/{snapshotId}/dimensions/{type}")
    @Operation(summary = "查询指定版本的图表数据")
    public ApiResponse<List<ResearchSnapshotDimension>> dimensions(
            @PathVariable long snapshotId, @PathVariable String type,
            @RequestParam(defaultValue = "50") @Min(1) @Max(1000) int limit) {
        return ApiResponse.ok(service.dimensions(snapshotId, type, limit));
    }

    /**
     * 从图表维度穿透到构成该计数的具体文献。
     */
    @GetMapping("/{snapshotId}/dimensions/{dimensionId}/publications")
    @Operation(summary = "分页查询某个统计维度的关联文献")
    public ApiResponse<ResearchSnapshotPublicationPage> dimensionPublications(
            @PathVariable long snapshotId, @PathVariable long dimensionId,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return ApiResponse.ok(service.dimensionPublications(snapshotId, dimensionId, page, size));
    }

    /**
     * 删除快照；只级联删除维度和穿透关系，全局文献继续保留。
     */
    @DeleteMapping("/{snapshotId}")
    @Operation(summary = "删除统计快照及全部关联数据")
    public ApiResponse<Void> delete(@PathVariable long snapshotId) {
        service.deleteSnapshot(snapshotId);
        return ApiResponse.ok(null);
    }

    /**
     * 修改快照展示名称，不重新计算统计数据。
     */
    @PatchMapping("/{snapshotId}/name")
    @Operation(summary = "修改统计快照名称")
    public ApiResponse<ResearchSnapshotDetail> rename(
            @PathVariable long snapshotId,
            @RequestBody @Valid ResearchSnapshotRenameRequest request) {
        return ApiResponse.ok(service.renameSnapshot(snapshotId, request.name()));
    }
}

package com.example.bioplatform.research.collection.common.controller;

import com.example.bioplatform.research.collection.common.dto.PendingRelationBatchRequest;
import com.example.bioplatform.research.collection.common.dto.PublicationRelationCollectionRequest;
import com.example.bioplatform.research.collection.common.service.PublicationRelationCollectionService;
import com.example.bioplatform.research.collection.common.vo.PublicationRelationJobStatus;
import com.example.bioplatform.research.collection.common.vo.RelatedPublicationPage;

import com.example.bioplatform.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 人工控制的单文献刷新和单层关联采集接口；不会自动运行全量任务。
 */
@RestController
@RequestMapping("/api/collection/publication-relations")
@RequiredArgsConstructor
@Tag(name = "文献关联采集", description = "按已注册数据库及唯一值人工启动、停止和恢复；资源链接由可扩展解析器处理")
public class PublicationRelationCollectionController {
    private final PublicationRelationCollectionService service;

    @PostMapping("/jobs/start")
    @Operation(summary = "启动单文献刷新/单层关联发现", description = "identifiers示例：GSA={pmid:41028458}；GenBase={paperId:203}或{pmid:42514208}")
    public ApiResponse<PublicationRelationJobStatus> start(@Valid @RequestBody PublicationRelationCollectionRequest request) {
        return ApiResponse.ok(service.start(request));
    }

    @PostMapping("/jobs/start-pending-batch")
    @Operation(summary = "自动批量领取待关联文献",
            description = "无需填写文献唯一值；从指定已注册数据库来源表领取PENDING记录并创建持久化队列，支持停止和断点续做")
    public ApiResponse<PublicationRelationJobStatus> startPendingBatch(
            @Valid @RequestBody PendingRelationBatchRequest request) {
        return ApiResponse.ok(service.startPendingBatch(request));
    }

    @PostMapping("/jobs/{jobId}/resume")
    @Operation(summary = "继续暂停、停止或失败的关联采集作业")
    public ApiResponse<PublicationRelationJobStatus> resume(@PathVariable long jobId) {
        return ApiResponse.ok(service.resume(jobId));
    }

    @PostMapping("/jobs/stop")
    @Operation(summary = "安全停止当前关联采集作业")
    public ApiResponse<PublicationRelationJobStatus> stop() {
        return ApiResponse.ok(service.stop());
    }

    @GetMapping("/jobs/status")
    @Operation(summary = "查询关联采集作业状态")
    public ApiResponse<PublicationRelationJobStatus> status(@RequestParam(required = false) Long jobId) {
        return ApiResponse.ok(service.status(jobId));
    }

    /**
     * 查询参数形式支持含斜杠的DOI，避免URL路径编码限制；与路径形式共用查询服务。
     */
    @GetMapping("/publications")
    public ApiResponse<RelatedPublicationPage> relatedByQuery(
            @RequestParam String databaseCode, @RequestParam String identifierType, @RequestParam String identifier,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.relatedPublications(databaseCode, identifierType, identifier, page, size));
    }

    @GetMapping("/publications/{databaseCode}/{identifierType}/{identifier}")
    @Operation(summary = "分页查看一篇文献的双向关联文献")
    public ApiResponse<RelatedPublicationPage> related(
            @PathVariable String databaseCode, @PathVariable String identifierType, @PathVariable String identifier,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.ok(service.relatedPublications(databaseCode, identifierType, identifier, page, size));
    }
}

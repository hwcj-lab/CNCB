package com.example.bioplatform.research.collection.openlb.controller;

import com.example.bioplatform.common.ApiResponse;
import com.example.bioplatform.research.collection.openlb.dto.OpenLbCollectionStartRequest;
import com.example.bioplatform.research.collection.openlb.service.OpenLbCollectionService;
import com.example.bioplatform.research.collection.openlb.vo.OpenLbCollectionStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * OpenLB主采集管理入口；导入PMID、启动/停止和失败重试分别由独立接口控制。
 */
@RestController
@RequestMapping("/api/collection/openlb")
@RequiredArgsConstructor
@Tag(name = "OpenLB文献采集", description = "独立的OpenLB离线采集流程，不归属任务一目录推荐或任务二统计查询")
public class OpenLbCollectionController {
    /**
     * service：控制器依赖的业务服务，不在控制器中实现持久化逻辑。
     */
    private final OpenLbCollectionService service;

    /**
     * 将服务端配置的CSV转换为可断点续传的PMID任务队列。
     */
    @PostMapping("/tasks/import-csv")
    @Operation(summary = "导入PMID任务", description = "从配置的openlb_pmid.csv导入；PMID唯一，已有任务不覆盖。")
    public ApiResponse<OpenLbCollectionStatus> importCsv() {
        return ApiResponse.ok(service.importCsv());
    }

    /**
     * 启动后台任务并立即返回，不占用HTTP连接等待全量采集。
     */
    @PostMapping("/jobs/start")
    @Operation(summary = "启动后台采集", description = "立即返回；后台只处理未完成任务，支持限速、重试和自动停止。")
    public ApiResponse<OpenLbCollectionStatus> start(@Valid @RequestBody(required = false) OpenLbCollectionStartRequest request) {
        return ApiResponse.ok(service.start(request == null ? new OpenLbCollectionStartRequest(null, null, null, null, null) : request));
    }

    /**
     * 设置停止标记，让当前批次收尾后安全退出。
     */
    @PostMapping("/jobs/stop")
    @Operation(summary = "请求停止", description = "当前已发出请求完成后停止，未完成PMID保持可续传状态。")
    public ApiResponse<OpenLbCollectionStatus> stop() {
        return ApiResponse.ok(service.stop());
    }

    /**
     * 人工将最终失败任务重置为待执行，不影响成功和空结果。
     */
    @PostMapping("/tasks/retry-failed")
    @Operation(summary = "重置失败任务", description = "将FAILED重置为PENDING，不会重置SUCCESS或EMPTY。")
    public ApiResponse<OpenLbCollectionStatus> retryFailed() {
        return ApiResponse.ok(service.retryFailed());
    }

    /**
     * 返回运行信息和持久化任务数量，不返回全量PMID明细。
     */
    @GetMapping("/status")
    @Operation(summary = "查询采集进度")
    public ApiResponse<OpenLbCollectionStatus> status() {
        return ApiResponse.ok(service.status());
    }
}

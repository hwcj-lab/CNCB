package com.example.bioplatform.research.collection.bioproject.controller;

import com.example.bioplatform.research.collection.bioproject.dto.BioProjectCollectionStartRequest;
import com.example.bioplatform.research.collection.bioproject.service.BioProjectCollectionService;
import com.example.bioplatform.common.ApiResponse;
import com.example.bioplatform.research.collection.bioproject.dto.BioProjectCollectionStartRequest;
import com.example.bioplatform.research.collection.bioproject.service.BioProjectCollectionService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * BIOPROJECT独立主采集入口；读取项目出版文献全列表并补元数据，关联发现沿用公共关联Controller。
 */
@RestController
@RequestMapping("/api/collection/bioproject")
@RequiredArgsConstructor
public class BioProjectCollectionController {
    private final BioProjectCollectionService service;

    /**
     * 首次采集、停止/失败续做；上次SUCCESS时创建新的完整列表扫描。
     */
    @PostMapping("/jobs/start")
    public ApiResponse<?> start(@Valid @RequestBody(required = false) BioProjectCollectionStartRequest request) {
        return ApiResponse.ok(service.start(config(request), false));
    }

    /**
     * 明确放弃旧扫描游标并下载新快照；不删除文献、成功补全内容或OpenLB任务。
     */
    @PostMapping("/jobs/restart")
    public ApiResponse<?> restart(@Valid @RequestBody(required = false) BioProjectCollectionStartRequest request) {
        return ApiResponse.ok(service.start(config(request), true));
    }

    /**
     * 安全停止当前BIOPROJECT作业。
     */
    @PostMapping("/jobs/stop")
    public ApiResponse<?> stop() {
        return ApiResponse.ok(service.stop());
    }

    /**
     * 查询BIOPROJECT下载、导入、补全阶段及持久化断点。
     */
    @GetMapping("/status")
    public ApiResponse<?> status() {
        return ApiResponse.ok(service.status());
    }

    /**
     * 保留文献，只重置元数据补全标记。
     */
    @PostMapping("/enrichment/reset")
    public ApiResponse<?> reset() {
        return ApiResponse.ok(service.resetEnrichment());
    }

    private BioProjectCollectionStartRequest config(BioProjectCollectionStartRequest r) {
        return r == null ? new BioProjectCollectionStartRequest(null, null, null, null) : r;
    }
}

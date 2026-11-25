package com.example.bioplatform.research.collection.genbase.controller;
import com.example.bioplatform.research.collection.genbase.dto.GenBaseCollectionStartRequest;
import com.example.bioplatform.research.collection.genbase.service.GenBaseCollectionService;
import com.example.bioplatform.research.collection.genbase.vo.GenBaseCollectionStatus;

import com.example.bioplatform.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** GenBase支持文章的独立采集管理接口。 */
@RestController
@RequestMapping("/api/collection/genbase")
@RequiredArgsConstructor
@Tag(name = "GenBase文献采集", description = "分页读取GenBase支持文章接口，支持断点续采和批量入库")
public class GenBaseCollectionController {
    private final GenBaseCollectionService service;

    /** 后台启动全量分页扫描；失败或停止的作业从持久化断点恢复。 */
    @PostMapping("/jobs/start")
    @Operation(summary = "启动或继续GenBase采集")
    public ApiResponse<GenBaseCollectionStatus> start(@Valid @RequestBody(required = false) GenBaseCollectionStartRequest request) {
        return ApiResponse.ok(service.start(request == null ? new GenBaseCollectionStartRequest(null, null, null, null) : request));
    }

    /** 当前HTTP页处理完成后停止，不丢失已经写入的数据。 */
    @PostMapping("/jobs/stop")
    @Operation(summary = "安全停止GenBase采集")
    public ApiResponse<GenBaseCollectionStatus> stop() { return ApiResponse.ok(service.stop()); }

    /** 仅重置GenBase来源表主列表文献元数据补全标记，不删除或重新导入OpenLB CSV任务。 */
    @PostMapping("/enrichment/reset")
    @Operation(summary = "重置GenBase元数据补全状态", description = "保留已采集文献，仅将GenBase主列表文献（包括无PMID记录）重新标为待元数据补全；随后调用/jobs/start执行。不会改动OpenLB导入任务表。")
    public ApiResponse<GenBaseCollectionStatus> resetEnrichment() {
        return ApiResponse.ok(service.resetEnrichment());
    }

    /** 返回最近作业的偏移量、总量、新增数量和错误。 */
    @GetMapping("/status")
    @Operation(summary = "查询GenBase采集进度")
    public ApiResponse<GenBaseCollectionStatus> status() { return ApiResponse.ok(service.status()); }
}


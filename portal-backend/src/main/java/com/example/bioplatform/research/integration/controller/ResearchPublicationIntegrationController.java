package com.example.bioplatform.research.integration.controller;

import com.example.bioplatform.common.ApiResponse;
import com.example.bioplatform.research.collection.common.vo.PublicationIntegrationStatus;
import com.example.bioplatform.research.integration.service.ResearchPublicationIntegrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 全局去重文献整合表的运行状态和人工全量重建接口。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/research-publications/integration")
@Tag(name = "全局文献整合", description = "采集变化自动增量同步；提供人工全量重建入口")
public class ResearchPublicationIntegrationController {
    private final ResearchPublicationIntegrationService service;

    /**
     * 查询全量重建状态；运行时快照生成入口会拒绝并发启动。
     */
    @GetMapping("/status")
    @Operation(summary = "查询全局文献整合状态")
    public ApiResponse<PublicationIntegrationStatus> status() {
        return ApiResponse.ok(service.status());
    }

    /**
     * 触发全量同步；后台只重新激活当前来源数据并保留历史引用主键。
     */
    @PostMapping("/rebuild")
    @Operation(summary = "全量重建全局去重文献整合表")
    public ApiResponse<PublicationIntegrationStatus> rebuild() {
        return ApiResponse.ok(service.startFullRebuild());
    }
}

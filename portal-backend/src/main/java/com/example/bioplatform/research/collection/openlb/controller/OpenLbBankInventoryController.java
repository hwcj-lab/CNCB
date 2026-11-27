package com.example.bioplatform.research.collection.openlb.controller;

import com.example.bioplatform.common.ApiResponse;
import com.example.bioplatform.research.collection.openlb.mapper.OpenLbRoutingMapper;
import com.example.bioplatform.research.collection.openlb.service.impl.OpenLbBankInventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * OpenLB来源盘点独立接口：不修改采集任务状态，不生成统计快照。
 */
@RestController
@RequestMapping("/api/collection/openlb/banks")
@RequiredArgsConstructor
public class OpenLbBankInventoryController {
    private final com.example.bioplatform.research.collection.openlb.service.impl.OpenLbRoutingService routing;
    private final com.example.bioplatform.research.integration.service.ResearchPublicationIntegrationService integration;
    private final OpenLbBankInventoryService service;
    private final OpenLbRoutingMapper mapper;

    @PostMapping("/redistribute-existing")
    public ApiResponse<?> redistribute(@RequestParam(defaultValue = "0") long afterId) {
        var result = routing.redistribute(Math.max(0, afterId));
        integration.syncIdentifiers((java.util.List<String>) result.get("pmids"), java.util.List.of());
        return ApiResponse.ok(result);
    }

    @PostMapping("/start")
    public ApiResponse<?> start() {
        return ApiResponse.ok(service.start());
    }

    @PostMapping("/stop")
    public ApiResponse<?> stop() {
        return ApiResponse.ok(service.stop());
    }

    @GetMapping("/status")
    public ApiResponse<?> status() {
        return ApiResponse.ok(service.status());
    }

    @GetMapping("/summary")
    public ApiResponse<?> summary() {
        return ApiResponse.ok(mapper.summary());
    }

    @GetMapping("/records")
    public ApiResponse<?> records(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "100") int size) {
        int n = Math.max(1, Math.min(size, 500));
        return ApiResponse.ok(mapper.inventoryRows((Math.max(1, page) - 1) * n, n));
    }
}

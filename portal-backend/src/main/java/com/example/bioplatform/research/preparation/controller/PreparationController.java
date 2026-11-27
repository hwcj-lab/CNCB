package com.example.bioplatform.research.preparation.controller;

import com.example.bioplatform.common.ApiResponse;
import com.example.bioplatform.research.preparation.model.PreparationRequest;
import com.example.bioplatform.research.preparation.service.PreparationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 数据整理管理入口。仅调度本地原始记录的整理，不负责采集网络数据或自动生成快照。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/research-preparation")
public class PreparationController {
    private final PreparationService service;

    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> status() {
        return ApiResponse.ok(service.status());
    }

    @PostMapping("/start")
    public ApiResponse<Map<String, Object>> start(@RequestBody(required = false) PreparationRequest request) {
        return ApiResponse.ok(service.start(request, false));
    }

    @PostMapping("/rebuild")
    public ApiResponse<Map<String, Object>> rebuild(@RequestBody(required = false) PreparationRequest request) {
        return ApiResponse.ok(service.start(request, true));
    }

    @PostMapping("/retry")
    public ApiResponse<Map<String, Object>> retry() {
        return ApiResponse.ok(service.start(null, false));
    }

    @PostMapping("/stop")
    public ApiResponse<Map<String, Object>> stop() {
        return ApiResponse.ok(service.stop());
    }

    @PostMapping("/recover")
    public ApiResponse<Map<String, Object>> recover() {
        return ApiResponse.ok(service.recover());
    }

    @PostMapping("/discard")
    public ApiResponse<Map<String, Object>> discard() {
        return ApiResponse.ok(service.discard());
    }

    @PostMapping("/incremental")
    public ApiResponse<Map<String, Object>> incremental(@RequestBody(required = false) PreparationRequest request) {
        return ApiResponse.ok(service.incremental(request));
    }
}

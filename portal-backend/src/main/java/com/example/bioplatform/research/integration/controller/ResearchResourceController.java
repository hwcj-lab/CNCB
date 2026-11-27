package com.example.bioplatform.research.integration.controller;

import com.example.bioplatform.research.integration.dto.ResearchResourceInput;
import com.example.bioplatform.research.integration.service.impl.ResearchResourceService;
import com.example.bioplatform.common.ApiResponse;
import com.example.bioplatform.research.integration.dto.ResearchResourceInput;
import com.example.bioplatform.research.integration.service.impl.ResearchResourceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 公共原库资源导入；用于已知资源API/导出文件的分批接入，不会自行请求上游。
 */
@RestController
@RequestMapping("/api/collection/resources")
@RequiredArgsConstructor
@org.springframework.validation.annotation.Validated
public class ResearchResourceController {
    private final ResearchResourceService service;

    @PostMapping("/batch")
    public ApiResponse<?> batch(@RequestBody @Size(min = 1, max = 200) List<@Valid ResearchResourceInput> rows) {
        return ApiResponse.ok(Map.of("processed", rows.size(), "affected", service.observe(rows)));
    }
}

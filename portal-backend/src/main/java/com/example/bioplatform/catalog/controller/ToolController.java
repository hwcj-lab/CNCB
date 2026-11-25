package com.example.bioplatform.catalog.controller;

import com.example.bioplatform.common.ApiResponse;
import com.example.bioplatform.catalog.entity.OnlineTool;
import com.example.bioplatform.catalog.service.CatalogService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tools")
@RequiredArgsConstructor
@Validated
/** 在线工具目录接口。 */
public class ToolController {
    private final CatalogService service;

    @Operation(summary = "工具目录详情")
    @GetMapping("/{id}")
    /** 按内部主键查询工具详情及官方跳转地址。 */
    public ApiResponse<OnlineTool> detail(@PathVariable @Min(1) Long id) {
        return ApiResponse.ok(service.tool(id));
    }
}

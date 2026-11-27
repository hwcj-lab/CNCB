package com.example.bioplatform.research.integration.controller;

import com.example.bioplatform.common.ApiResponse;
import com.example.bioplatform.research.integration.dto.ResearchResourceInput;
import com.example.bioplatform.research.integration.service.impl.ResearchResourceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 公共原库资源导入；用于已知资源API/导出文件的分批接入，不会自行请求上游。
 */
@RestController
@RequestMapping("/api/collection/resources")
@RequiredArgsConstructor
@org.springframework.validation.annotation.Validated
public class ResearchResourceController {
    /**
     * service：控制器依赖的业务服务，不在控制器中实现持久化逻辑。
     */
    private final ResearchResourceService service;

    /**
     * 规范化批次大小并限制到本采集器允许的范围，或接收一次资源批次输入。
     * @param rows 本批待处理记录
     * @return 按上述规则计算或读取的ApiResponse<?>结果
     */
    @PostMapping("/batch")
    public ApiResponse<?> batch(@RequestBody @Size(min = 1, max = 200) List<@Valid ResearchResourceInput> rows) {
        return ApiResponse.ok(Map.of("processed", rows.size(), "affected", service.observe(rows)));
    }
}

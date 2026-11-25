package com.example.bioplatform.catalog.controller;

import com.example.bioplatform.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.util.Map;

import com.example.bioplatform.catalog.mapper.DatabaseMapper;
import com.example.bioplatform.catalog.mapper.ToolMapper;

@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
/** 系统运行状态接口，用于前端判断数据库连接及数据模式。 */
public class SystemController {
    private final DataSource dataSource;
    private final DatabaseMapper databases;
    private final ToolMapper tools;
    @Value("${app.demo-enabled:false}")
    private boolean demoEnabled;

    @Operation(summary = "检查应用与数据库连接，不泄露连接凭证")
    @GetMapping("/status")
    /**
     * 获取不含密码、JDBC 地址等敏感信息的健康摘要。
     * 该接口同时返回目录数量，供资源页面顶部统计使用。
     */
    public ApiResponse<Map<String, Object>> status() throws java.sql.SQLException {
        try (var connection = dataSource.getConnection()) {
            return ApiResponse.ok(Map.of("application", "Bio Resource Portal", "database", connection.getMetaData().getDatabaseProductName(), "demoEnabled", demoEnabled, "researchDate", "2026-11-11", "cncbbankStatus", "PENDING", "catalogProvenance", "CNCB_BIODB_AND_SEARCH_APIS", "databaseCount", databases.selectCount(null), "toolEntryCount", tools.selectCount(null)));
        }
    }
}

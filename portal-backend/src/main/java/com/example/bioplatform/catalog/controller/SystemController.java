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

/** 系统运行状态接口，用于前端判断数据库连接及数据模式。 */
@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemController {
    /**
     * dataSource：系统数据源，用于连接状态或数据库访问。
     */
    private final DataSource dataSource;
    /**
     * databases：数据库目录结果或数据库目录持久层。
     */
    private final DatabaseMapper databases;
    /**
     * tools：在线工具结果或工具持久层。
     */
    private final ToolMapper tools;
    /**
     * demoEnabled：是否启用本地演示数据模式。
     */
    @Value("${app.demo-enabled:false}")
    private boolean demoEnabled;

    /**
     * 获取不含密码、JDBC 地址等敏感信息的健康摘要。
     * 该接口同时返回目录数量，供资源页面顶部统计使用。
     */
    @Operation(summary = "检查应用与数据库连接，不泄露连接凭证")
    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> status() throws java.sql.SQLException {
        try (var connection = dataSource.getConnection()) {
            return ApiResponse.ok(Map.of("application", "Bio Resource Portal", "database", connection.getMetaData().getDatabaseProductName(), "demoEnabled", demoEnabled, "researchDate", "2026-11-11", "cncbbankStatus", "PENDING", "catalogProvenance", "CNCB_BIODB_AND_SEARCH_APIS", "databaseCount", databases.selectCount(null), "toolEntryCount", tools.selectCount(null)));
        }
    }
}

package com.example.bioplatform.research.statistics.dto;


import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建快照时只设置名称和关联文献口径；全部已注册数据库固定参与统计。
 */
@Data
public class ResearchSnapshotBuildRequest {
    @Size(max = 100, message = "快照名称不能超过100个字符")
    private String name;

    /**
     * false/空：仅原始文献；true：同时纳入纯关联发现文献。
     */
    private Boolean includeRelated;
    /**
     * true：纳入无原生文献资源；false：只统计文献。
     */
    private Boolean includeResources;
}


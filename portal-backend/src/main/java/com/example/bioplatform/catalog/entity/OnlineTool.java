package com.example.bioplatform.catalog.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

/** 在线分析工具及其调用入口；URL 来自已记录的官方目录。 */
@Data
@TableName("online_tool")
public class OnlineTool {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String btId;
    private String name;
    private String platform;
    private String btEnDescription;
    private String btEnTitle;
    private String btZhDescription;
    private String btZhTitle;
    private String btUrl;
    private String httpMethod;
    private Boolean isVisible;
    private String status;
    private String createBy;
    private String updateBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableField(exist = false)
    private String relationType;
    @TableField(exist = false)
    private String relationBasis;
    /**
     * 批量查询关联工具时用于按数据库分组，不是 online_tool 表字段。
     */
    @JsonIgnore
    @TableField(exist = false)
    private Long relationDatabaseId;
}

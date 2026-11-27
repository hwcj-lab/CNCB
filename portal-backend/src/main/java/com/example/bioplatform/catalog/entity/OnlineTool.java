package com.example.bioplatform.catalog.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

/** 在线分析工具及其调用入口；URL 来自已记录的官方目录。 */
@Data
@TableName("online_tool")
public class OnlineTool {
    /**
     * id：本对象或持久化记录的主键标识。
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * btId：工具在上游目录中的稳定编号。
     */
    private String btId;
    /**
     * name：当前对象或资源的展示名称。
     */
    private String name;
    /**
     * platform：在线工具所属平台或运行环境。
     */
    private String platform;
    /**
     * btEnDescription：在线工具英文介绍。
     */
    private String btEnDescription;
    /**
     * btEnTitle：在线工具英文标题。
     */
    private String btEnTitle;
    /**
     * btZhDescription：在线工具中文介绍。
     */
    private String btZhDescription;
    /**
     * btZhTitle：在线工具中文标题。
     */
    private String btZhTitle;
    /**
     * btUrl：在线工具的官方访问入口。
     */
    private String btUrl;
    /**
     * httpMethod：访问入口使用的HTTP方法。
     */
    private String httpMethod;
    /**
     * isVisible：目录或工具是否允许对外展示。
     */
    private Boolean isVisible;
    /**
     * status：当前对象的业务状态，由对应任务或目录状态机定义。
     */
    private String status;
    /**
     * createBy：创建记录的操作人或导入来源标记。
     */
    private String createBy;
    /**
     * updateBy：最近更新记录的操作人或导入来源标记。
     */
    private String updateBy;
    /**
     * createTime：记录创建时间。
     */
    private LocalDateTime createTime;
    /**
     * updateTime：记录最近更新时间。
     */
    private LocalDateTime updateTime;
    /**
     * relationType：关联关系类型。
     */
    @TableField(exist = false)
    private String relationType;
    /**
     * relationBasis：数据库与工具匹配的依据或证据说明。
     */
    @TableField(exist = false)
    private String relationBasis;
    /**
     * 批量查询关联工具时用于按数据库分组，不是 online_tool 表字段。
     */
    @JsonIgnore
    @TableField(exist = false)
    private Long relationDatabaseId;
}

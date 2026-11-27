package com.example.bioplatform.catalog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 可扩展的数据类型标签，用于推荐页面导航与数据库归类。 */
@Data
@TableName("data_type")
public class DataType {
    /**
     * id：本对象或持久化记录的主键标识。
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * code：当前来源、策略或业务响应使用的编码。
     */
    private String code;
    /**
     * name：当前对象或资源的展示名称。
     */
    private String name;
    /**
     * description：资源或分类的说明文本。
     */
    private String description;
    /**
     * sortOrder：展示排序值。
     */
    private Integer sortOrder;
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
     * relationRole：数据库与类型关系所扮演的角色。
     */
    @TableField(exist = false)
    private String relationRole;
    /**
     * relationNote：类型关系的人工说明与证据补充。
     */
    @TableField(exist = false)
    private String relationNote;
}

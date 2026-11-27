package com.example.bioplatform.catalog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 可扩展的数据类型标签，用于推荐页面导航与数据库归类。 */
@Data
@TableName("data_type")
public class DataType {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    private String description;
    private Integer sortOrder;
    private Boolean isVisible;
    private String status;
    private String createBy;
    private String updateBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableField(exist = false)
    private String relationRole;
    @TableField(exist = false)
    private String relationNote;
}

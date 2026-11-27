package com.example.bioplatform.catalog.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** CNCB 或合作方数据库资源，以及仅用于返回的类型和工具关联信息。 */
@Data
@TableName("database_resource")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DatabaseResource {
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * CNCB数据库目录接口外层数值id，仅用于同步定位。
     */
    private Long catalogId;
    /**
     * CNCB目录dbId；同时作为跨库搜索返回结果的业务匹配标识。
     */
    private String dbId;
    private String name;
    private Long dbCategoriesId;
    private String dbUrl;
    private String httpMethod;
    private String dbEnDescription;
    private String dbEnTitle;
    private String dbZhDescription;
    private String dbZhTitle;
    private Long organizationId;
    private Long typeId;
    private Boolean isVisible;
    private String status;
    private String createBy;
    private String updateBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableField(exist = false)
    private List<DataType> dataTypes;
    @TableField(exist = false)
    private List<OnlineTool> tools;
}

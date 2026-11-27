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
    /**
     * id：本对象或持久化记录的主键标识。
     */
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
    /**
     * name：当前对象或资源的展示名称。
     */
    private String name;
    /**
     * dbCategoriesId：上游数据库目录分类ID。
     */
    private Long dbCategoriesId;
    /**
     * dbUrl：数据库资源的官方访问地址。
     */
    private String dbUrl;
    /**
     * httpMethod：访问入口使用的HTTP方法。
     */
    private String httpMethod;
    /**
     * dbEnDescription：数据库英文介绍。
     */
    private String dbEnDescription;
    /**
     * dbEnTitle：数据库英文标题。
     */
    private String dbEnTitle;
    /**
     * dbZhDescription：数据库中文介绍。
     */
    private String dbZhDescription;
    /**
     * dbZhTitle：数据库中文标题。
     */
    private String dbZhTitle;
    /**
     * organizationId：来源提供的机构ID。
     */
    private Long organizationId;
    /**
     * typeId：资源对应的数据类型或原始分类ID。
     */
    private Long typeId;
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
     * dataTypes：数据库关联的数据类型列表。
     */
    @TableField(exist = false)
    private List<DataType> dataTypes;
    /**
     * tools：在线工具结果或工具持久层。
     */
    @TableField(exist = false)
    private List<OnlineTool> tools;
}

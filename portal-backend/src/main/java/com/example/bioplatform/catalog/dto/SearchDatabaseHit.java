package com.example.bioplatform.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
/** CNCB 跨库搜索 databases 数组中本平台实际使用的字段。 */
public class SearchDatabaseHit {
    /**
     * 上游数据库业务标识，对应本地 database_resource.db_id。
     */
    @NotBlank(message = "搜索结果数据库 id 不能为空")
    private String id;
    /**
     * 以下字段用于展示与诊断，匹配过程不以名称或 URL 作为唯一依据。
     */
    private String name;
    private String description;
    private String url;
    private Long hitCount;
    private List<String> categories;
}

package com.example.bioplatform.research.collection.common.dto;


import lombok.Data;

/** 快照主题统计使用的已确认数据库标签映射行。 */
@Data
public class ResearchDatabaseTopicMapping {
    /** 数据库目录短名或上游数据库别名。 */
    private String databaseCode;
    /** 标签稳定代码，例如 SEQUENCE、VARIATION。 */
    private String topicCode;
    /** 面向页面的中文标签名。 */
    private String topicName;
}

package com.example.bioplatform.research.statistics.vo;


import lombok.Data;

/**
 * 快照中一个可直接用于图表的预聚合数据点。
 */
@Data
public class ResearchSnapshotDimension {
    /**
     * id：本对象或持久化记录的主键标识。
     */
    private Long id;
    /**
     * scopeCode：统计结果所属范围；TOTAL或具体来源编码。
     */
    private String scopeCode;
    /**
     * dimensionType：维度类型，例如YEAR、AUTHOR、INSTITUTION、FUND或TOPIC。
     */
    private String dimensionType;
    /**
     * dimensionKey：维度成员的业务键或规范化摘要。
     */
    private String dimensionKey;
    /**
     * displayName：维度图表或列表使用的展示文本。
     */
    private String displayName;
    /**
     * authorName：解析后的作者姓名；不能单独作为全局自然人标识。
     */
    private String authorName;
    /**
     * institutionName：该作者对应的一个机构；未知时允许为空。
     */
    private String institutionName;
    /**
     * statYear：年度维度使用的年份值。
     */
    private Integer statYear;
    /**
     * publicationCount：当前统计范围的文献或统计对象计数，具体口径由快照选项决定。
     */
    private Integer publicationCount;
}

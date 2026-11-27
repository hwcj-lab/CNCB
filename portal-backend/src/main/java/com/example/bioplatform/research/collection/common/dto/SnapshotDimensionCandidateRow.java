package com.example.bioplatform.research.collection.common.dto;


import lombok.Data;

/**
 * 快照构建批次中的单个“维度—文献”候选关系，批量写入暂存表后由数据库汇总。
 */
@Data
public class SnapshotDimensionCandidateRow {
    /**
     * snapshotId：统计快照主键。
     */
    private long snapshotId;
    /**
     * scopeCode：统计结果所属范围；TOTAL或具体来源编码。
     */
    private String scopeCode;
    /**
     * dimensionType：维度类型，例如YEAR、AUTHOR、INSTITUTION、FUND或TOPIC。
     */
    private String dimensionType;
    /**
     * dimensionHash：维度键的摘要，供旧版统计索引与去重使用。
     */
    private byte[] dimensionHash;
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
    private int statYear;
    /**
     * publicationKey：统一文献身份键，用于统计去重和穿透定位。
     */
    private String publicationKey;
}

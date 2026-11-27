package com.example.bioplatform.research.collection.common.dto;


import lombok.Data;

/** 快照开始时预建少量年度和主题维度，供扫描期间直接累计正式穿透键。 */
@Data
public class SnapshotDimensionSeedRow {
    /**
     * id：本对象或持久化记录的主键标识。
     */
    private long id;
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
     * dimensionKey：维度成员的业务键或规范化摘要。
     */
    private String dimensionKey;
    /**
     * dimensionHash：维度键的摘要，供旧版统计索引与去重使用。
     */
    private byte[] dimensionHash;
    /**
     * displayName：维度图表或列表使用的展示文本。
     */
    private String displayName;
    /**
     * statYear：年度维度使用的年份值。
     */
    private int statYear;
    /**
     * publicationCount：当前统计范围的文献或统计对象计数，具体口径由快照选项决定。
     */
    private long publicationCount;
}

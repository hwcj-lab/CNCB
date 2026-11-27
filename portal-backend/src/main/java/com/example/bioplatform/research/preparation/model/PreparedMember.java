package com.example.bioplatform.research.preparation.model;

import lombok.Data;

/**
 * 一篇来源记录内的单个维度成员；五类成员复用此结构，各自独立保存和去重。
 */
@Data
public class PreparedMember {
    /**
     * documentId：所属research_prepared_document记录版本ID。
     */
    private Long documentId;
    /**
     * sourceCode：数据库来源编码，用于区分各库记录。
     * documentKey：文献去重候选键；跨来源统计还可能应用DOI与PMID桥接。
     * pmid：已有PubMed标识；为空时不通过标题猜补。
     * doi：DOI标识；匹配前按调用链统一规范化。
     * sourceUrl：该来源记录对应的原始访问地址。
     */
    private String sourceCode, documentKey, pmid, doi, sourceUrl;
    /**
     * memberKey：规范化维度成员摘要；同一文献同一成员据此去重。
     * displayName：维度图表或列表使用的展示文本。
     * authorName：解析后的作者姓名；不能单独作为全局自然人标识。
     * institutionName：该作者对应的一个机构；未知时允许为空。
     */
    private String memberKey, displayName, authorName, institutionName;
    /**
     * authorOrder：解析后的作者署名顺序，从1开始；多机构拆分保持同序号。
     * statYear：年度维度使用的年份值。
     */
    private Integer authorOrder, statYear;
}

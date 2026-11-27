package com.example.bioplatform.research.collection.common.dto;


import lombok.Data;

/**
 * 各文献来源适配器输出的统一记录，不要求来源提供全部字段。
 */
@Data
public class SourcePublicationRecord {
    /**
     * 来源表自增主键，仅用作游标分页，不作为业务穿透键。
     */
    private Long sourceRowId;
    /**
     * sourceUpdatedTime：来源记录的更新时间，与扫描ID共同构成增量游标。
     */
    private java.time.LocalDateTime sourceUpdatedTime;
    /**
     * sourceCode：数据库来源编码，用于区分各库记录。
     */
    private String sourceCode;
    /**
     * sourceRecordKey：原库记录业务唯一值，不要求一定具有PMID。
     */
    private String sourceRecordKey;
    /**
     * recordKind：记录类型：PUBLICATION文献或RESOURCE无文献资源。
     */
    private String recordKind = "PUBLICATION";
    /**
     * resourceAccession：原库资源登录号，无文献资源也据此定位。
     */
    private String resourceAccession;
    /**
     * resourceUrl：无文献资源可直接跳转的原库地址。
     */
    private String resourceUrl;
    /**
     * sourceUrl：该来源记录对应的原始访问地址。
     */
    private String sourceUrl;
    /**
     * recordActive：来源记录是否有效；失效观察用于关闭旧整理版本。
     */
    private Boolean recordActive = true;
    /**
     * pmid：已有PubMed标识；为空时不通过标题猜补。
     */
    private String pmid;
    /**
     * doi：DOI标识；匹配前按调用链统一规范化。
     */
    private String doi;
    /**
     * 原始来源资源编号，用于缺少论文标识时跳转资源详情。
     */
    private String gsaAccessions;
    /**
     * bioprojectAccession：BioProject项目编号信息。
     */
    private String bioprojectAccession;
    /**
     * title：文献、资源或展示对象的标题。
     */
    private String title;
    /**
     * pubYear：解析后的文献发表年份，缺失时为空。
     */
    private Integer pubYear;
    /**
     * journalTitle：文献期刊名称。
     */
    private String journalTitle;
    /**
     * authorsJson：逐作者原始信息及作者所属机构的JSON载荷。
     */
    private String authorsJson;
    /**
     * affiliationInfo：来源提供的机构汇总原文，不应随意分配给所有作者。
     */
    private String affiliationInfo;
    /**
     * grantsJson：保留完整资助记录的JSON载荷，便于整理时重新解析。
     */
    private String grantsJson;
    /**
     * keywordList：来源提供的论文关键词载荷。
     */
    private String keywordList;
    /**
     * meshHeadingsJson：PubMed MeSH主题词的原始JSON。
     */
    private String meshHeadingsJson;
    /**
     * 文献关联的CNCB数据库及资源编号，用于总统计映射研究主题。
     */
    private String cncbBanksJson;
    /**
     * 是否为来源数据库中的原始/主文献；避免使用MySQL关键字作为别名。
     */
    private Boolean primaryFlag;
}

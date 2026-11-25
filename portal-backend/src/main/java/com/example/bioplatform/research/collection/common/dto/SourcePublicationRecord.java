package com.example.bioplatform.research.collection.common.dto;


import lombok.Data;

/** 各文献来源适配器输出的统一记录，不要求来源提供全部字段。 */
@Data
public class SourcePublicationRecord {
    /** 来源表自增主键，仅用作游标分页，不作为业务穿透键。 */
    private Long sourceRowId;
    private String sourceCode;
    private String sourceRecordKey;
    private String recordKind = "PUBLICATION";
    private String resourceAccession;
    private String resourceUrl;
    private Boolean recordActive = true;
    private String pmid;
    private String doi;
    /** 原始来源资源编号，用于缺少论文标识时跳转资源详情。 */
    private String gsaAccessions;
    private String bioprojectAccession;
    private String title;
    private Integer pubYear;
    private String journalTitle;
    private String authorsJson;
    private String affiliationInfo;
    private String grantsJson;
    private String keywordList;
    private String meshHeadingsJson;
    /** 文献关联的CNCB数据库及资源编号，用于总统计映射研究主题。 */
    private String cncbBanksJson;
    /** 是否为来源数据库中的原始/主文献；避免使用MySQL关键字作为别名。 */
    private Boolean primaryFlag;
}

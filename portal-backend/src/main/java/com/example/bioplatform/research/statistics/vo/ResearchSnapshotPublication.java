package com.example.bioplatform.research.statistics.vo;


import lombok.Data;

/** 统计快照中可供前端追溯的去重文献摘要。 */
@Data
public class ResearchSnapshotPublication {
    /** 当前穿透维度的统计范围；TOTAL为总统计，其余为来源数据库代码。 */
    private String scopeCode;
    /** 当前统计范围使用的业务唯一键：TOTAL为全局键，分库为该来源表的原生键。 */
    private String publicationKey;
    /** 各来源自己的业务标识，例如OPENLB=PMID:xxx、GENBASE=PAPER:xxx。 */
    private String sourceRecordKeys;
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
    private String sourceCodes;
    private String sourceUrl;
    private String cncbBanksJson;
    private String authorsJson;
    private String affiliationInfo;
    private String grantsJson;
}

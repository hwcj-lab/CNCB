package com.example.bioplatform.research.statistics.vo;


import lombok.Data;

/**
 * 统计快照中可供前端追溯的去重文献摘要。
 */
@Data
public class ResearchSnapshotPublication {
    /**
     * 当前穿透维度的统计范围；TOTAL为总统计，其余为来源数据库代码。
     */
    private String scopeCode;
    /**
     * 当前统计范围使用的业务唯一键：TOTAL为全局键，分库为该来源表的原生键。
     */
    private String publicationKey;
    /**
     * 各来源自己的业务标识，例如OPENLB=PMID:xxx、GENBASE=PAPER:xxx。
     */
    private String sourceRecordKeys;
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
     * sourceCodes：参与本次处理的来源编码集合或拼接值。
     */
    private String sourceCodes;
    /**
     * sourceUrl：该来源记录对应的原始访问地址。
     */
    private String sourceUrl;
    /**
     * cncbBanksJson：CNCB资源关联数组，包含db、accession和link等。
     */
    private String cncbBanksJson;
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
}

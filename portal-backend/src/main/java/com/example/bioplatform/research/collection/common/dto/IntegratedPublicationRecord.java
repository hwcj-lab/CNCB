package com.example.bioplatform.research.collection.common.dto;


import lombok.Data;

/**
 * 旧全量整合表的文献读取模型；新快照使用prepared整理表，本对象仍用于兼容整合与旧统计。
 */
@Data
public class IntegratedPublicationRecord {
    /**
     * id：本对象或持久化记录的主键标识。
     */
    private Long id;
    /**
     * publicationKey：统一文献身份键，用于统计去重和穿透定位。
     */
    private String publicationKey;
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
     * sourceRecordKeys：各来源编码对应的业务唯一键映射。
     */
    private String sourceRecordKeys;
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
     * 整合来源后保留的CNCB资源关联数组。
     */
    private String cncbBanksJson;
    /**
     * hasPrimarySource：是否至少有一个来源明确认定为本库记录。
     */
    private Boolean hasPrimarySource;
}

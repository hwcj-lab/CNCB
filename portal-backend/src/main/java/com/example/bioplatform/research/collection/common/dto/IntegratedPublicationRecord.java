package com.example.bioplatform.research.collection.common.dto;


import lombok.Data;

/**
 * 旧全量整合表的文献读取模型；新快照使用prepared整理表，本对象仍用于兼容整合与旧统计。
 */
@Data
public class IntegratedPublicationRecord {
    private Long id;
    private String publicationKey;
    private String recordKind = "PUBLICATION";
    private String resourceAccession;
    private String resourceUrl;
    private Boolean recordActive = true;
    private String pmid;
    private String doi;
    private String title;
    private Integer pubYear;
    private String journalTitle;
    private String sourceCodes;
    private String sourceRecordKeys;
    private String authorsJson;
    private String affiliationInfo;
    private String grantsJson;
    private String keywordList;
    private String meshHeadingsJson;
    /**
     * 整合来源后保留的CNCB资源关联数组。
     */
    private String cncbBanksJson;
    private Boolean hasPrimarySource;
}

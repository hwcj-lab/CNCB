package com.example.bioplatform.research.collection.common.dto;


import lombok.Data;

/**
 * 已全局去重的整合文献；快照生成直接读取此对象，不在快照流程中重写整合表。
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

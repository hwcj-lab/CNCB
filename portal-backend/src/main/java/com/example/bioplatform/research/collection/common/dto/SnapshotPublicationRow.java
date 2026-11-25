package com.example.bioplatform.research.collection.common.dto;


import lombok.Builder;
import lombok.Data;

/**
 * 批量写入全局去重文献主表的数据。
 */
@Data
@Builder
public class SnapshotPublicationRow {
    private String publicationKey;
    private String contentHash;
    private String recordKind;
    private String resourceAccession;
    private String resourceUrl;
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

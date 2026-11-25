package com.example.bioplatform.research.collection.common.dto;

import lombok.Data;

/**
 * 数据库来源表本身充当持久化补全队列；原有JSON参与增量合并。
 */
@Data
public class PublicationEnrichmentTask {
    private Long id;
    private String recordKey, pmid, doi, title, journalTitle, authorsJson, affiliationInfo, grantsJson, cncbBanksJson, meshHeadingsJson, keywordList, abstractText, bioprojectAccession;
    private Integer pubYear;
}

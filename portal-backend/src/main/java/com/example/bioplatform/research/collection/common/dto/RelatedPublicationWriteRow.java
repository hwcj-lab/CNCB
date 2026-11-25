package com.example.bioplatform.research.collection.common.dto;


/** 由关联链路发现并通过OpenLB PMID接口补全的文献批量写入模型。 */
public record RelatedPublicationWriteRow(
        String pmid,
        String doi,
        Integer pubYear,
        String journalTitle,
        String title,
        String abstractText,
        String affiliationInfo,
        String keywordList,
        String authorsJson,
        String meshHeadingsJson,
        String grantsJson,
        String cncbBanksJson,
        String rawPayload,
        String sourceUrl,
        String sourceHash,
        boolean related
) {}

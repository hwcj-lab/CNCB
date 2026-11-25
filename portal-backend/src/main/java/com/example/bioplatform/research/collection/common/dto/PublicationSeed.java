package com.example.bioplatform.research.collection.common.dto;

import lombok.Data;

/**
 * 数据库来源支持文章列表的轻量原始记录；不把项目提交者误作论文作者。
 */
@Data
public class PublicationSeed {
    private String recordKey, pmid, doi, title, journalTitle, sourceUrl, accessions, bioprojectAccession, rawPayload, sourceHash, cncbBanksJson;
    private Integer pubYear;
    private boolean primary = true, related = false;
    private String grantsJson = "[]";
}

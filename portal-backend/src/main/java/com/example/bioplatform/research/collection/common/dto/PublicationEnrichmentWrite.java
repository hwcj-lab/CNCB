package com.example.bioplatform.research.collection.common.dto;

import lombok.Data;
/** 单条数据库来源补全结果，整批通过Mapper持久化后再同步整合表。 */
@Data public class PublicationEnrichmentWrite {
 private Long id; private String pmid,doi,title,journalTitle,authorsJson,affiliationInfo,grantsJson,cncbBanksJson,meshHeadingsJson,keywordList,abstractText,rawPayload,sourceHash,status,error;
 private Integer pubYear;
}

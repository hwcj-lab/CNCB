package com.example.bioplatform.research.collection.common.vo;


/**
 * 文献详情下钻使用的一条双向关联文献摘要。
 */
public record RelatedPublicationView(Long relationId, String identifierType, String identifierValue,
                                     String pmid, String doi, String title, Integer pubYear,
                                     String evidenceUrl, Integer discoveryDepth, Boolean active, String sourceUrl,
                                     String gsaAccessions, String bioprojectAccession, String cncbBanksJson,
                                     String recordKind, String resourceAccession, String resourceUrl) {
}

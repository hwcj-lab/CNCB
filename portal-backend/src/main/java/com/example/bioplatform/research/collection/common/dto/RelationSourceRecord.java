package com.example.bioplatform.research.collection.common.dto;


/**
 * 从OpenLB或GenBase来源表读取的关联发现所需最小字段。
 */
public record RelationSourceRecord(String identifierType, String identifierValue, String pmid,
                                   String bioprojectAccession, String grantsJson,
                                   String cncbBanksJson, String sourceHash, String relationStatus) {
}

package com.example.bioplatform.research.collection.common.service.relation;

/**
 * 已读取的来源元数据；PMID详情获取、关联发现、入库互不混用职责。
 */
public record RelationDiscoveryInput(String databaseCode, String identifierType, String identifierValue, String pmid,
                                     String cncbBanksJson, String bioprojectAccession,
                                     boolean acceptsPublicationReferences) {
}

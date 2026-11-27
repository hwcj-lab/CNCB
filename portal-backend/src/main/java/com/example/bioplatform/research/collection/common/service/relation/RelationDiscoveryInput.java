package com.example.bioplatform.research.collection.common.service.relation;

/**
 * 已读取的来源元数据；PMID详情获取、关联发现、入库互不混用职责。
 * @param databaseCode 数据库来源编码或目录业务编码
 * @param identifierType 唯一值类别，例如PMID、DOI或SOURCE_KEY
 * @param identifierValue 与identifierType配套的原始业务唯一值
 * @param pmid 已有PubMed标识；为空时不通过标题猜补
 * @param cncbBanksJson CNCB资源关联数组，包含db、accession和link等
 * @param bioprojectAccession BioProject项目编号信息
 * @param acceptsPublicationReferences 当前来源是否支持接收并写入缺少PMID的原始出版记录
 */
public record RelationDiscoveryInput(String databaseCode, String identifierType, String identifierValue, String pmid,
                                     String cncbBanksJson, String bioprojectAccession,
                                     boolean acceptsPublicationReferences) {
}

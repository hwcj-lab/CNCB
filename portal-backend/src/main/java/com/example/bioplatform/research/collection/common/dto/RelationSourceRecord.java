package com.example.bioplatform.research.collection.common.dto;


/**
 * 从OpenLB或GenBase来源表读取的关联发现所需最小字段。
 * @param identifierType 唯一值类别，例如PMID、DOI或SOURCE_KEY
 * @param identifierValue 与identifierType配套的原始业务唯一值
 * @param pmid 已有PubMed标识；为空时不通过标题猜补
 * @param bioprojectAccession BioProject项目编号信息
 * @param grantsJson 保留完整资助记录的JSON载荷，便于整理时重新解析
 * @param cncbBanksJson CNCB资源关联数组，包含db、accession和link等
 * @param sourceHash 来源业务键或内容的稳定摘要，具体生成规则见写入方
 * @param relationStatus 来源记录关联检查状态：PENDING、RUNNING、COMPLETED或FAILED
 */
public record RelationSourceRecord(String identifierType, String identifierValue, String pmid,
                                   String bioprojectAccession, String grantsJson,
                                   String cncbBanksJson, String sourceHash, String relationStatus) {
}

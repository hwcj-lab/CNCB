package com.example.bioplatform.research.collection.common.vo;


/**
 * 文献详情下钻使用的一条双向关联文献摘要。
 * @param relationId 持久化关联边ID，不是目标文献的内部主键
 * @param identifierType 唯一值类别，例如PMID、DOI或SOURCE_KEY
 * @param identifierValue 与identifierType配套的原始业务唯一值
 * @param pmid 已有PubMed标识；为空时不通过标题猜补
 * @param doi DOI标识；匹配前按调用链统一规范化
 * @param title 文献、资源或展示对象的标题
 * @param pubYear 解析后的文献发表年份，缺失时为空
 * @param evidenceUrl 证明关联关系的官方资源地址
 * @param discoveryDepth 记录该关联首次或最短发现深度；当前采集只展开根文献一层
 * @param active 关联边当前是否有效；历史记录可保留但不再作为有效关联返回
 * @param sourceUrl 该来源记录对应的原始访问地址
 * @param gsaAccessions GSA等来源归档编号信息
 * @param bioprojectAccession BioProject项目编号信息
 * @param cncbBanksJson CNCB资源关联数组，包含db、accession和link等
 * @param recordKind 记录类型：PUBLICATION文献或RESOURCE无文献资源
 * @param resourceAccession 原库资源登录号，无文献资源也据此定位
 * @param resourceUrl 无文献资源可直接跳转的原库地址
 */
public record RelatedPublicationView(Long relationId, String identifierType, String identifierValue,
                                     String pmid, String doi, String title, Integer pubYear,
                                     String evidenceUrl, Integer discoveryDepth, Boolean active, String sourceUrl,
                                     String gsaAccessions, String bioprojectAccession, String cncbBanksJson,
                                     String recordKind, String resourceAccession, String resourceUrl) {
}

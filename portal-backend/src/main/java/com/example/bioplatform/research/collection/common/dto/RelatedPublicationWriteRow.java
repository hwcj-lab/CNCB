package com.example.bioplatform.research.collection.common.dto;


/**
 * 由关联链路发现并通过OpenLB PMID接口补全的文献批量写入模型。
 * @param pmid 已有PubMed标识；为空时不通过标题猜补
 * @param doi DOI标识；匹配前按调用链统一规范化
 * @param pubYear 解析后的文献发表年份，缺失时为空
 * @param journalTitle 文献期刊名称
 * @param title 文献、资源或展示对象的标题
 * @param abstractText 论文摘要文本
 * @param affiliationInfo 来源提供的机构汇总原文，不应随意分配给所有作者
 * @param keywordList 来源提供的论文关键词载荷
 * @param authorsJson 逐作者原始信息及作者所属机构的JSON载荷
 * @param meshHeadingsJson PubMed MeSH主题词的原始JSON
 * @param grantsJson 保留完整资助记录的JSON载荷，便于整理时重新解析
 * @param cncbBanksJson CNCB资源关联数组，包含db、accession和link等
 * @param rawPayload 来源原始响应或单条原始记录，用于追溯和重解析
 * @param sourceUrl 该来源记录对应的原始访问地址
 * @param sourceHash 来源业务键或内容的稳定摘要，具体生成规则见写入方
 * @param related 是否作为关联记录处理，或关联查询结果
 */
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
) {
}

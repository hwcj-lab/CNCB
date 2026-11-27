package com.example.bioplatform.research.collection.genbase.dto;


/**
 * GenBase接口记录的批量写入模型。
 * @param paperId GenBase支持文章的原库paper_id
 * @param pmid 已有PubMed标识；为空时不通过标题猜补
 * @param doi DOI标识；匹配前按调用链统一规范化
 * @param bioprojectAccession BioProject项目编号信息
 * @param journalName GenBase原始文章列表中的期刊名称
 * @param paperTitle GenBase原始文章列表中的论文标题
 * @param publishYear GenBase原始文章解析出的发表年份
 * @param genbaseAccessions GenBase原始记录提供的序列登录号或编号范围
 * @param sourceUrl 该来源记录对应的原始访问地址
 * @param rawPayload 来源原始响应或单条原始记录，用于追溯和重解析
 * @param sourceHash 来源业务键或内容的稳定摘要，具体生成规则见写入方
 */
public record GenBasePublicationWriteRow(
        Long paperId,
        String pmid,
        String doi,
        String bioprojectAccession,
        String journalName,
        String paperTitle,
        Integer publishYear,
        String genbaseAccessions,
        String sourceUrl,
        String rawPayload,
        String sourceHash
) {
}


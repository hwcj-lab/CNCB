package com.example.bioplatform.research.collection.common.dto;

import lombok.Data;
/** 单条数据库来源补全结果，整批通过Mapper持久化后再同步整合表。 */
@Data public class PublicationEnrichmentWrite {
 /**
  * id：本对象或持久化记录的主键标识。
  */
 private Long id; /**
  * pmid：已有PubMed标识；为空时不通过标题猜补。
  * doi：DOI标识；匹配前按调用链统一规范化。
  * title：文献、资源或展示对象的标题。
  * journalTitle：文献期刊名称。
  * authorsJson：逐作者原始信息及作者所属机构的JSON载荷。
  * affiliationInfo：来源提供的机构汇总原文，不应随意分配给所有作者。
  * grantsJson：保留完整资助记录的JSON载荷，便于整理时重新解析。
  * cncbBanksJson：CNCB资源关联数组，包含db、accession和link等。
  * meshHeadingsJson：PubMed MeSH主题词的原始JSON。
  * keywordList：来源提供的论文关键词载荷。
  * abstractText：论文摘要文本。
  * rawPayload：来源原始响应或单条原始记录，用于追溯和重解析。
  * sourceHash：来源业务键或内容的稳定摘要，具体生成规则见写入方。
  * status：当前对象的业务状态，由对应任务或目录状态机定义。
  * error：本次处理产生的错误信息。
  */
 private String pmid,doi,title,journalTitle,authorsJson,affiliationInfo,grantsJson,cncbBanksJson,meshHeadingsJson,keywordList,abstractText,rawPayload,sourceHash,status,error;
 /**
  * pubYear：解析后的文献发表年份，缺失时为空。
  */
 private Integer pubYear;
}

package com.example.bioplatform.research.collection.common.dto;

import lombok.Data;

/**
 * 数据库来源表本身充当持久化补全队列；原有JSON参与增量合并。
 */
@Data
public class PublicationEnrichmentTask {
    /**
     * id：本对象或持久化记录的主键标识。
     */
    private Long id;
    /**
     * recordKey：原始记录的稳定业务键。
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
     * bioprojectAccession：BioProject项目编号信息。
     */
    private String recordKey, pmid, doi, title, journalTitle, authorsJson, affiliationInfo, grantsJson, cncbBanksJson, meshHeadingsJson, keywordList, abstractText, bioprojectAccession;
    /**
     * pubYear：解析后的文献发表年份，缺失时为空。
     */
    private Integer pubYear;
}

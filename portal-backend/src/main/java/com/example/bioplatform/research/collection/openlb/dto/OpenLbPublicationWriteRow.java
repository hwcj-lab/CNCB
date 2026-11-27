package com.example.bioplatform.research.collection.openlb.dto;


import lombok.Builder;
import lombok.Data;

/**
 * OpenLB API 数据的批量写入模型。
 * 保留规范化检索字段和原始 JSON，便于后续补充解析而无需重新采集。
 */
@Data
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@Builder
public class OpenLbPublicationWriteRow {
    /**
     * id：本对象或持久化记录的主键标识。
     */
    private Long id;
    /**
     * pmid：已有PubMed标识；为空时不通过标题猜补。
     */
    private String pmid;
    /**
     * pubYear：解析后的文献发表年份，缺失时为空。
     */
    private Integer pubYear;
    /**
     * journalTitle：文献期刊名称。
     */
    private String journalTitle;
    /**
     * title：文献、资源或展示对象的标题。
     */
    private String title;
    /**
     * abstractText：论文摘要文本。
     */
    private String abstractText;
    /**
     * affiliationInfo：来源提供的机构汇总原文，不应随意分配给所有作者。
     */
    private String affiliationInfo;
    /**
     * keywordList：来源提供的论文关键词载荷。
     */
    private String keywordList;
    /**
     * doi：DOI标识；匹配前按调用链统一规范化。
     */
    private String doi;
    /**
     * sourceUrl：该来源记录对应的原始访问地址。
     */
    private String sourceUrl;
    /**
     * authorsJson：逐作者原始信息及作者所属机构的JSON载荷。
     */
    private String authorsJson;
    /**
     * meshHeadingsJson：PubMed MeSH主题词的原始JSON。
     */
    private String meshHeadingsJson;
    /**
     * grantsJson：保留完整资助记录的JSON载荷，便于整理时重新解析。
     */
    private String grantsJson;
    /**
     * cncbBanksJson：CNCB资源关联数组，包含db、accession和link等。
     */
    private String cncbBanksJson;
    /**
     * rawPayload：来源原始响应或单条原始记录，用于追溯和重解析。
     */
    private String rawPayload;
    /**
     * 规范化来源内容SHA-256；相同则跳过字段覆盖，变化则重新标记待关联。
     */
    private String sourceHash;
}

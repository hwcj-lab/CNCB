package com.example.bioplatform.research.collection.common.dto;

import lombok.Data;

/**
 * 数据库来源支持文章列表的轻量原始记录；不把项目提交者误作论文作者。
 */
@Data
public class PublicationSeed {
    /**
     * recordKey：原始记录的稳定业务键。
     * pmid：已有PubMed标识；为空时不通过标题猜补。
     * doi：DOI标识；匹配前按调用链统一规范化。
     * title：文献、资源或展示对象的标题。
     * journalTitle：文献期刊名称。
     * sourceUrl：该来源记录对应的原始访问地址。
     * accessions：当前解析结果涉及的资源登录号集合。
     * bioprojectAccession：BioProject项目编号信息。
     * rawPayload：来源原始响应或单条原始记录，用于追溯和重解析。
     * sourceHash：来源业务键或内容的稳定摘要，具体生成规则见写入方。
     * cncbBanksJson：CNCB资源关联数组，包含db、accession和link等。
     */
    private String recordKey, pmid, doi, title, journalTitle, sourceUrl, accessions, bioprojectAccession, rawPayload, sourceHash, cncbBanksJson;
    /**
     * pubYear：解析后的文献发表年份，缺失时为空。
     */
    private Integer pubYear;
    /**
     * primary：是否属于原始或本库记录，具体归属由来源判定。
     * related：是否作为关联记录处理，或关联查询结果。
     */
    private boolean primary = true, related = false;
    /**
     * grantsJson：保留完整资助记录的JSON载荷，便于整理时重新解析。
     */
    private String grantsJson = "[]";
}

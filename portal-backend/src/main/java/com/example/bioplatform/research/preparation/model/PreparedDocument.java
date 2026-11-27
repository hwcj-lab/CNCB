package com.example.bioplatform.research.preparation.model;

import lombok.Data;

/**
 * 某来源一条文献或资源的整理版本；所有维度通过document_id引用该版本。
 */
@Data
public class PreparedDocument {
    /**
     * sourceRowId：来源适配器提供的稳定扫描ID，可能包含资源类型编码。
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Long sourceRowId;
    /**
     * id：本对象或持久化记录的主键标识。
     * validFrom：记录版本生效的整理修订号，包含此边界。
     * validUntil：记录版本失效的整理修订号，不含此边界；空表示当前有效。
     * seenRun：最近一次扫描确认记录存在的整理任务ID。
     */
    private Long id, validFrom, validUntil, seenRun;
    /**
     * sourceCode：数据库来源编码，用于区分各库记录。
     * sourceKey：来源内的稳定业务唯一键。
     * sourceHash：来源业务键或内容的稳定摘要，具体生成规则见写入方。
     * documentKey：文献去重候选键；跨来源统计还可能应用DOI与PMID桥接。
     * contentHash：解析内容摘要，用于判断记录是否发生变化。
     */
    private String sourceCode, sourceKey, sourceHash, documentKey, contentHash;
    /**
     * pmid：已有PubMed标识；为空时不通过标题猜补。
     * doi：DOI标识；匹配前按调用链统一规范化。
     * title：文献、资源或展示对象的标题。
     * journalTitle：文献期刊名称。
     * sourceUrl：该来源记录对应的原始访问地址。
     * recordKind：记录类型：PUBLICATION文献或RESOURCE无文献资源。
     * resourceAccession：原库资源登录号，无文献资源也据此定位。
     */
    private String pmid, doi, title, journalTitle, sourceUrl, recordKind, resourceAccession;
    /**
     * gsaAccessions：GSA等来源归档编号信息。
     * bioprojectAccession：BioProject项目编号信息。
     */
    private String gsaAccessions, bioprojectAccession;
    /**
     * pubYear：解析后的文献发表年份，缺失时为空。
     */
    private Integer pubYear;
    /**
     * primaryFlag：相对当前来源是否属于本库记录。
     */
    private Boolean primaryFlag;
}

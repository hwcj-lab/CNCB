package com.example.bioplatform.research.integration.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * 原库资源观察；hasPublication=true表示资源已有自己的文章，只保留文献统计。
 */
@Data
public class ResearchResourceInput {
    /**
     * databaseCode：数据库来源编码或目录业务编码。
     */
    @NotBlank
    @Size(max = 40)
    private String databaseCode;
    /**
     * accession：资源在原始数据库中的登录号。
     */
    @NotBlank
    @Size(max = 320)
    private String accession;
    /**
     * title：文献、资源或展示对象的标题。
     */
    @Size(max = 2000)
    private String title;
    /**
     * sourceUrl：该来源记录对应的原始访问地址。
     */
    @NotBlank
    @Size(max = 1500)
    private String sourceUrl;
    /**
     * releaseYear：资源公开或发布年份。
     */
    @Min(1500)
    @Max(2200)
    private Integer releaseYear;
    /**
     * hasPublication：该资源是否已有自己的文献，用于避免同时重复计入资源与文献。
     */
    @NotNull
    private Boolean hasPublication;
    /**
     * primaryRecord：是否为本库原始记录，而非仅由关联链路发现。
     */
    private Boolean primaryRecord = true;
    /**
     * affiliationInfo：来源提供的机构汇总原文，不应随意分配给所有作者。
     * authorsJson：逐作者原始信息及作者所属机构的JSON载荷。
     * grantsJson：保留完整资助记录的JSON载荷，便于整理时重新解析。
     * cncbBanksJson：CNCB资源关联数组，包含db、accession和link等。
     * keywordList：来源提供的论文关键词载荷。
     * rawPayload：来源原始响应或单条原始记录，用于追溯和重解析。
     */
    private String affiliationInfo, authorsJson, grantsJson, cncbBanksJson, keywordList, rawPayload;
}

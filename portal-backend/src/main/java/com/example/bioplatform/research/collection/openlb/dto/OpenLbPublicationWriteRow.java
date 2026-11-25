package com.example.bioplatform.research.collection.openlb.dto;


import lombok.Builder;
import lombok.Data;

@Data
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@Builder
/**
 * OpenLB API 数据的批量写入模型。
 * 保留规范化检索字段和原始 JSON，便于后续补充解析而无需重新采集。
 */
public class OpenLbPublicationWriteRow {
    private Long id;
    private String pmid;
    private Integer pubYear;
    private String journalTitle;
    private String title;
    private String abstractText;
    private String affiliationInfo;
    private String keywordList;
    private String doi;
    private String sourceUrl;
    private String authorsJson;
    private String meshHeadingsJson;
    private String grantsJson;
    private String cncbBanksJson;
    private String rawPayload;
    /**
     * 规范化来源内容SHA-256；相同则跳过字段覆盖，变化则重新标记待关联。
     */
    private String sourceHash;
}

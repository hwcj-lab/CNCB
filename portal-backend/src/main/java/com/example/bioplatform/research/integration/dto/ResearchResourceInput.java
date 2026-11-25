package com.example.bioplatform.research.integration.dto;

import jakarta.validation.constraints.*;import lombok.Data;
/** 原库资源观察；hasPublication=true表示资源已有自己的文章，只保留文献统计。 */
@Data public class ResearchResourceInput {
 @NotBlank @Size(max=40) private String databaseCode;
 @NotBlank @Size(max=320) private String accession;
 @Size(max=2000) private String title;
 @NotBlank @Size(max=1500) private String sourceUrl;
 @Min(1500) @Max(2200) private Integer releaseYear;
 @NotNull private Boolean hasPublication;
 private Boolean primaryRecord=true;
 private String affiliationInfo,authorsJson,grantsJson,cncbBanksJson,keywordList,rawPayload;
}

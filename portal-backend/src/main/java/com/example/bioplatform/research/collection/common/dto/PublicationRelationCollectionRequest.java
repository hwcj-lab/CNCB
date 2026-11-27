package com.example.bioplatform.research.collection.common.dto;


import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.Map;

/**
 * 单篇文献重新采集及单层关联发现请求。
 * identifiers允许不同数据库使用不同唯一键，例如OpenLB使用pmid，GenBase可使用paperId、pmid或doi。
 */
public record PublicationRelationCollectionRequest(
        @NotBlank String databaseCode,
        @NotEmpty Map<String, String> identifiers,
        Boolean refreshPublication,
        Boolean refreshRelations,
        @Min(1) @Max(100000) Integer maxPublications,
        @Min(100) @Max(60000) Long requestDelayMs
) {
    public boolean refreshPublicationOrDefault() { return Boolean.TRUE.equals(refreshPublication); }
    public boolean refreshRelationsOrDefault() { return refreshRelations == null || refreshRelations; }
    public int maxPublicationsOrDefault() { return maxPublications == null ? 1000 : maxPublications; }
    public long requestDelayMsOrDefault() { return requestDelayMs == null ? 500L : requestDelayMs; }
}

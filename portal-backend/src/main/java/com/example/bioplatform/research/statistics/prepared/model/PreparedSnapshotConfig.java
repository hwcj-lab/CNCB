package com.example.bioplatform.research.statistics.prepared.model;
import lombok.Data;
@Data
public class PreparedSnapshotConfig {
    private Long snapshotId,preparationRevision;
    private String sourceNamesJson,topicScopesJson;
}

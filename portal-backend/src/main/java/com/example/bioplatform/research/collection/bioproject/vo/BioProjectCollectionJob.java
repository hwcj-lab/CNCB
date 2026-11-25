package com.example.bioplatform.research.collection.bioproject.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * BIOPROJECT一次静态快照扫描的持久化阶段和游标，SUCCESS后再启动会建立新扫描。
 */
@Data
public class BioProjectCollectionJob {
    private Long id, nextIndex, rowsImported, metadataProcessed;
    private String status, phase, cacheFile, lastError;
    private LocalDateTime startedTime, completedTime;
}

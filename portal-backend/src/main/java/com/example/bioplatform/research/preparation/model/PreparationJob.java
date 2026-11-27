package com.example.bioplatform.research.preparation.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PreparationJob {
    private String scanMode;
    private LocalDateTime afterUpdateTime;
    private Long id;
    private String status, sourceCodesJson, errorMessage;
    private Integer sourceIndex, batchSize;
    private Long afterId, rowsScanned, rowsChanged;
    private Boolean rebuild, allSources;
    private LocalDateTime startedTime, completedTime;
}

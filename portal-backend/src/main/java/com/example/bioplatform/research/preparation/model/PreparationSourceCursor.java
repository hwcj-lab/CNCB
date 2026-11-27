package com.example.bioplatform.research.preparation.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * One source's frozen scan window and durable last committed tuple, published only with a SUCCESS job.
 */
@Data
public class PreparationSourceCursor {
    private Long jobId;
    private String sourceCode, scanMode, status;
    private LocalDateTime fromTime, untilTime, afterUpdateTime;
    private Long afterId;
}

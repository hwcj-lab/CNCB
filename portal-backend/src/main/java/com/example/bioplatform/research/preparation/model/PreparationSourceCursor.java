package com.example.bioplatform.research.preparation.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * One source's frozen scan window and durable last committed tuple, published only with a SUCCESS job.
 */
@Data
public class PreparationSourceCursor {
    /**
     * jobId：所属持久化任务或作业ID。
     */
    private Long jobId;
    /**
     * sourceCode：数据库来源编码，用于区分各库记录。
     * scanMode：扫描模式：全量FULL、增量INCREMENTAL，或强制重建入口标记。
     * status：当前对象的业务状态，由对应任务或目录状态机定义。
     */
    private String sourceCode, scanMode, status;
    /**
     * fromTime：本次增量窗口的时间下界，可能包含重叠区间。
     * untilTime：本次冻结的时间上界，不包含此时间。
     * afterUpdateTime：最后已提交记录的更新时间，与afterId一起用于增量分页。
     */
    private LocalDateTime fromTime, untilTime, afterUpdateTime;
    /**
     * afterId：最后已提交的扫描ID；不是已处理条数。
     */
    private Long afterId;
}

package com.example.bioplatform.research.preparation.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 独立整理任务的持久化状态、选定来源及最后已提交断点。
 */
@Data
public class PreparationJob {
    /**
     * scanMode：扫描模式：全量FULL、增量INCREMENTAL，或强制重建入口标记。
     */
    private String scanMode;
    /**
     * afterUpdateTime：最后已提交记录的更新时间，与afterId一起用于增量分页。
     */
    private LocalDateTime afterUpdateTime;
    /**
     * id：本对象或持久化记录的主键标识。
     */
    private Long id;
    /**
     * status：当前对象的业务状态，由对应任务或目录状态机定义。
     * sourceCodesJson：本次任务冻结的来源编码有序JSON数组。
     * errorMessage：任务失败原因或异常说明。
     */
    private String status, sourceCodesJson, errorMessage;
    /**
     * sourceIndex：当前处理来源在冻结列表中的下标，从0开始。
     * batchSize：每批读取或写入的记录数量上限。
     */
    private Integer sourceIndex, batchSize;
    /**
     * afterId：最后已提交的扫描ID；不是已处理条数。
     * rowsScanned：本任务已提交扫描的来源记录数量。
     * rowsChanged：本任务新建、替换或失效的来源记录数量。
     */
    private Long afterId, rowsScanned, rowsChanged;
    /**
     * rebuild：是否强制重新解析已有记录。
     * allSources：本次是否选择全部已启用来源。
     */
    private Boolean rebuild, allSources;
    /**
     * startedTime：任务开始或恢复执行的时间。
     * completedTime：任务完成、停止或失败的时间；进行中可为空。
     */
    private LocalDateTime startedTime, completedTime;
}

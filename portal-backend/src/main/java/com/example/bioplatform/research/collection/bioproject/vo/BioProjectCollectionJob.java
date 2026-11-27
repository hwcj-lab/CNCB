package com.example.bioplatform.research.collection.bioproject.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * BIOPROJECT一次静态快照扫描的持久化阶段和游标，SUCCESS后再启动会建立新扫描。
 */
@Data
public class BioProjectCollectionJob {
    /**
     * id：本对象或持久化记录的主键标识。
     * nextIndex：下一条待处理列表记录的索引。
     * rowsImported：已提交导入的原始列表记录数量。
     * metadataProcessed：已处理的元数据补全记录数量。
     */
    private Long id, nextIndex, rowsImported, metadataProcessed;
    /**
     * status：当前对象的业务状态，由对应任务或目录状态机定义。
     * phase：任务所处的执行阶段。
     * cacheFile：当前采集使用的本地快照文件路径。
     * lastError：最近一次任务错误信息。
     */
    private String status, phase, cacheFile, lastError;
    /**
     * startedTime：任务开始或恢复执行的时间。
     * completedTime：任务完成、停止或失败的时间；进行中可为空。
     */
    private LocalDateTime startedTime, completedTime;
}

package com.example.bioplatform.research.collection.common.vo;


import java.time.LocalDateTime;

/**
 * 单层关联作业状态；processedCount为处理次数，discoveredCount为新增队列标识数，relationCount为关系写入累计次数，均不等于新增入库篇数。
 * @param processRunning 当前应用进程是否实际执行该关联作业，区别于数据库保存的状态
 * @param stopRequested 是否收到人工停止请求
 * @param jobId 所属持久化任务或作业ID
 * @param databaseCode 数据库来源编码或目录业务编码
 * @param rootIdentifierType 作业根标识类型，单篇为PMID等，批次作业为BATCH
 * @param rootIdentifier 单篇根文献唯一值，或自动批次的说明标记
 * @param status 当前对象的业务状态，由对应任务或目录状态机定义
 * @param maxPublications 本轮处理的安全数量预算
 * @param requestDelayMs 作业配置的上游请求间隔，单位毫秒
 * @param processedCount 累计根节点处理次数，可能包含失败重试，不等于去重文献篇数
 * @param discoveredCount 累计新登记的目标队列标识数，不等于新增来源表文献行数
 * @param relationCount 关联关系写入累计次数，包含重复更新，不等于不同关联文献数量
 * @param pendingCount 当前PENDING队列项数量，不包含正在执行和已失败的项
 * @param lastError 最近一次任务错误信息
 * @param startedTime 任务开始或恢复执行的时间
 * @param completedTime 任务完成、停止或失败的时间；进行中可为空
 */
public record PublicationRelationJobStatus(
        boolean processRunning,
        boolean stopRequested,
        Long jobId,
        String databaseCode,
        String rootIdentifierType,
        String rootIdentifier,
        String status,
        Integer maxPublications,
        Long requestDelayMs,
        Integer processedCount,
        Integer discoveredCount,
        Integer relationCount,
        Long pendingCount,
        String lastError,
        LocalDateTime startedTime,
        LocalDateTime completedTime
) {
}

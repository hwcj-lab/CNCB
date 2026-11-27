package com.example.bioplatform.research.collection.genbase.vo;


import java.time.LocalDateTime;

/**
 * GenBase最近一次采集作业及当前进程运行状态。
 * @param running 当前进程是否正在执行对应后台任务
 * @param stopRequested 是否收到人工停止请求
 * @param message 响应说明、当前阶段说明或错误提示
 * @param jobId 所属持久化任务或作业ID
 * @param jobStatus 来源采集作业的持久化状态，不等同于当前进程是否仍在执行
 * @param nextStart 上游分页的下一页偏移量
 * @param pageSize 每页记录数
 * @param recordsTotal 上游报告的记录总数
 * @param rowsSeen 当前来源采集已读取的原始列表行数
 * @param rowsInserted 当前来源采集新增入库记录数，重复记录不重复算新增
 * @param attemptCount 当前页或当前补全记录的请求尝试次数，具体范围由所属作业决定
 * @param lastError 最近一次任务错误信息
 * @param currentDelayMs 当前实际请求间隔，单位毫秒
 * @param startedTime 任务开始或恢复执行的时间
 * @param completedTime 任务完成、停止或失败的时间；进行中可为空
 */
public record GenBaseCollectionStatus(
        boolean running,
        boolean stopRequested,
        String message,
        Long jobId,
        String jobStatus,
        Integer nextStart,
        Integer pageSize,
        Integer recordsTotal,
        Integer rowsSeen,
        Integer rowsInserted,
        Integer attemptCount,
        String lastError,
        long currentDelayMs,
        LocalDateTime startedTime,
        LocalDateTime completedTime
) {
}


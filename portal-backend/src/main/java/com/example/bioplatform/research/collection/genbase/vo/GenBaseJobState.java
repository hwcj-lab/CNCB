package com.example.bioplatform.research.collection.genbase.vo;


import java.time.LocalDateTime;

/**
 * Mapper使用的GenBase持久化作业快照，不包含应用进程内状态。
 * @param jobId 所属持久化任务或作业ID
 * @param jobStatus 来源采集作业的持久化状态，不等同于当前进程是否仍在执行
 * @param nextStart 上游分页的下一页偏移量
 * @param pageSize 每页记录数
 * @param recordsTotal 上游报告的记录总数
 * @param rowsSeen 当前来源采集已读取的原始列表行数
 * @param rowsInserted 当前来源采集新增入库记录数，重复记录不重复算新增
 * @param attemptCount 当前页或当前补全记录的请求尝试次数，具体范围由所属作业决定
 * @param lastError 最近一次任务错误信息
 * @param startedTime 任务开始或恢复执行的时间
 * @param completedTime 任务完成、停止或失败的时间；进行中可为空
 */
public record GenBaseJobState(
        Long jobId,
        String jobStatus,
        Integer nextStart,
        Integer pageSize,
        Integer recordsTotal,
        Integer rowsSeen,
        Integer rowsInserted,
        Integer attemptCount,
        String lastError,
        LocalDateTime startedTime,
        LocalDateTime completedTime
) {
}


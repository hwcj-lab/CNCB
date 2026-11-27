package com.example.bioplatform.research.collection.openlb.vo;


import java.time.LocalDateTime;
import java.util.Map;

/**
 * OpenLB 采集会话状态及持久化任务队列计数。
 * @param running 当前进程是否正在执行对应后台任务
 * @param stopRequested 是否收到人工停止请求
 * @param message 响应说明、当前阶段说明或错误提示
 * @param currentDelayMs 当前实际请求间隔，单位毫秒
 * @param startedAt 当前采集会话开始时间
 * @param finishedAt 当前采集会话结束时间
 * @param taskCounts 按PENDING、RUNNING、SUCCESS、EMPTY、FAILED等状态汇总的持久化任务数
 */
public record OpenLbCollectionStatus(
        boolean running,
        boolean stopRequested,
        String message,
        long currentDelayMs,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        Map<String, Long> taskCounts
) {
}

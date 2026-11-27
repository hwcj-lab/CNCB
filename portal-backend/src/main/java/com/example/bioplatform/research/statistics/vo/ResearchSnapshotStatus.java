package com.example.bioplatform.research.statistics.vo;


/**
 * 统计构建运行状态和最近可用版本。
 * @param running 当前进程是否正在执行对应后台任务
 * @param message 响应说明、当前阶段说明或错误提示
 * @param current 当前正在构建或最近执行的快照详情
 * @param latestSuccess 最近成功发布的快照；没有成功版本时为空
 */
public record ResearchSnapshotStatus(boolean running, String message, ResearchSnapshotDetail current,
                                     ResearchSnapshotDetail latestSuccess) {
}

package com.example.bioplatform.research.collection.common.vo;


/**
 * 全局文献整合状态；运行标记供前端避免与快照构建并发。
 * @param running 当前进程是否正在执行对应后台任务
 * @param message 响应说明、当前阶段说明或错误提示
 * @param processed 本次处理的记录数或累计处理次数
 * @param total 当前范围内的总量
 */
public record PublicationIntegrationStatus(boolean running, String message, long processed, long total) {
}

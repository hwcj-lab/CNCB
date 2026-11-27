package com.example.bioplatform.research.collection.common.vo;


/**
 * 采集任务按状态聚合后的计数。
 * @param status 当前对象的业务状态，由对应任务或目录状态机定义
 * @param total 当前范围内的总量
 */
public record TaskStatusCount(String status, long total) {
}

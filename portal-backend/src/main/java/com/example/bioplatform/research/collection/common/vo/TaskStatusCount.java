package com.example.bioplatform.research.collection.common.vo;


/** 采集任务按状态聚合后的计数。 */
public record TaskStatusCount(String status, long total) {}

package com.example.bioplatform.research.collection.common.vo;


/** 全局文献整合状态；运行标记供前端避免与快照构建并发。 */
public record PublicationIntegrationStatus(boolean running, String message, long processed, long total) { }

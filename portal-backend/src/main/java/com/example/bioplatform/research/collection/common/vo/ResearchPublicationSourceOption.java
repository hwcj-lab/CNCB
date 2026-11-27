package com.example.bioplatform.research.collection.common.vo;


/**
 * 可参与快照统计的已注册本地文献来源。
 * @param code 当前来源、策略或业务响应使用的编码
 * @param name 当前对象或资源的展示名称
 */
public record ResearchPublicationSourceOption(String code, String name) {
}

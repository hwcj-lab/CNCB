package com.example.bioplatform.research.statistics.vo;


/**
 * 一个快照内实际存在的数据库分统计入口。
 * @param code 当前来源、策略或业务响应使用的编码
 * @param name 当前对象或资源的展示名称
 * @param publicationCount 当前统计范围的文献或统计对象计数，具体口径由快照选项决定
 */
public record ResearchSnapshotDatabaseOption(String code, String name, int publicationCount) {
}

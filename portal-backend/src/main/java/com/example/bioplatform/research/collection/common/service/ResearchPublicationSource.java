package com.example.bioplatform.research.collection.common.service;

import com.example.bioplatform.research.collection.common.dto.SourcePublicationRecord;

import java.util.List;

/**
 * 科研文献来源扩展点。
 * 新增数据库时只需增加一个实现类，快照构建器会通过 Spring 自动发现，无需修改汇总入口。
 */
public interface ResearchPublicationSource {
    /**
     * 返回稳定、唯一的来源代码。
     */
    String sourceCode();

    /**
     * 页面展示名称；新增来源由适配器自行提供，统计核心不维护数据库名称分支。
     */
    default String sourceName() {
        return sourceCode();
    }

    /**
     * 是否参与新快照；停用来源无需修改快照构建器。
     */
    default boolean enabled() {
        return true;
    }

    /**
     * 使用来源表的稳定游标分页读取；快照任务只保留当前批次。
     */
    List<SourcePublicationRecord> loadRecordsPage(boolean includeRelated, long afterId, int pageSize);

    /**
     * 按PMID/DOI批量查询本来源记录，供跨库整合时补齐同一文献字段。
     */
    List<SourcePublicationRecord> loadRecordsByIdentifiers(List<String> pmids, List<String> dois);

    /**
     * 统计本来源参与整合的行数，不读取正文。
     */
    long countRecords(boolean includeRelated);

    /**
     * 查询来源中实际存在的年份，快照构建据此预建少量年度维度。
     */
    List<Integer> distinctYears(boolean includeRelated);

    /**
     * 按该来源自己的业务唯一值批量读取记录，供分库统计穿透使用。
     */
    List<SourcePublicationRecord> loadRecordsBySourceKeys(List<String> sourceKeys);

    /**
     * Optional ordered change stream; the existing collection/full-scan contract remains unchanged.
     */
    default boolean supportsIncremental() {
        return false;
    }

    default List<SourcePublicationRecord> loadChangedRecordsPage(boolean related,
                                                                 java.time.LocalDateTime time, long after, java.time.LocalDateTime until, int size) {
        throw new UnsupportedOperationException("Source does not provide update_time/id pagination: " + sourceCode());
    }
}

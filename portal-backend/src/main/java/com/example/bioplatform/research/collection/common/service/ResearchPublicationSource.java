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
     * 按来源稳定游标分页读取；新流程供数据整理使用，旧整合流程也可复用；只保留当前批次。
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
     * 查询来源实际存在的年份；供来源读取和旧版快照使用，新版年度统计读取prepared_year。
     */
    List<Integer> distinctYears(boolean includeRelated);

    /**
     * 按该来源自己的业务唯一值批量读取记录，供分库统计穿透使用。
     */
    List<SourcePublicationRecord> loadRecordsBySourceKeys(List<String> sourceKeys);

    /**
     * 可选的增量读取能力；不支持时保持原采集及全量扫描契约，由整理任务退回全量。
     */
    default boolean supportsIncremental() {
        return false;
    }

    /**
     * 按更新时间和ID组成的游标读取变更，严格排除时间上界；读取顺序必须与整理游标校验一致。
     */
    default List<SourcePublicationRecord> loadChangedRecordsPage(boolean related,
                                                                 java.time.LocalDateTime time, long after, java.time.LocalDateTime until, int size) {
        throw new UnsupportedOperationException("Source does not provide update_time/id pagination: " + sourceCode());
    }
}

package com.example.bioplatform.research.integration.service.impl;

import com.example.bioplatform.research.collection.common.dto.SourcePublicationRecord;
import com.example.bioplatform.research.collection.common.service.ResearchPublicationSource;
import com.example.bioplatform.research.integration.mapper.ResearchResourceMapper;

import java.util.*;

/**
 * 同库文献/无文献资源合并为一条有序分页流；偶数游标为原文献，奇数为资源。
 */
final class ResourceAwareResearchPublicationSource implements ResearchPublicationSource {
    private final ResearchPublicationSource papers;
    private final ResearchResourceMapper resources;

    ResourceAwareResearchPublicationSource(ResearchPublicationSource papers, ResearchResourceMapper resources) {
        this.papers = papers;
        this.resources = resources;
    }

    /**
     * 返回适配器稳定的数据库编码，用于路由、整理范围和分库统计。
     */
    public String sourceCode() {
        return papers.sourceCode();
    }

    /**
     * 返回来源展示名称；统计内部仍使用sourceCode作为身份。
     */
    public String sourceName() {
        return papers.sourceName();
    }

    /**
     * 返回该来源是否参与注册和统计选择；停用不代表删除已有采集数据。
     */
    public boolean enabled() {
        return papers.enabled();
    }

    /**
     * 把本库文献和无文献资源合并为稳定分页流；适配器以偶数/奇数编码区分两种扫描ID。
     */
    public List<SourcePublicationRecord> loadRecordsPage(boolean related, long after, int size) {
        List<SourcePublicationRecord> result = new ArrayList<>();
        long base = after / 2;
        for (var original : papers.loadRecordsPage(related, base, size)) {
            var row = new SourcePublicationRecord();
            org.springframework.beans.BeanUtils.copyProperties(original, row);
            row.setSourceRowId(Math.multiplyExact(row.getSourceRowId(), 2));
            if (row.getSourceRowId() > after) result.add(row);
        }
        for (var original : resources.page(sourceCode(), related, Math.max(0, base - (after % 2 == 0 ? 1 : 0)), size)) {
            var row = new SourcePublicationRecord();
            org.springframework.beans.BeanUtils.copyProperties(original, row);
            row.setSourceRowId(Math.addExact(Math.multiplyExact(row.getSourceRowId(), 2), 1));
            if (row.getSourceRowId() > after) result.add(row);
        }
        return result.stream().sorted(Comparator.comparing(SourcePublicationRecord::getSourceRowId)).limit(size).toList();
    }

    /**
     * 按已有PMID或DOI批量读取本来源记录；不会通过标题搜索并补写PMID。
     */
    public List<SourcePublicationRecord> loadRecordsByIdentifiers(List<String> pmids, List<String> dois) {
        return papers.loadRecordsByIdentifiers(pmids, dois);
    }

    /**
     * 按本来源及关联记录口径计数；此处不是跨数据库的全局去重总量。
     */
    public long countRecords(boolean related) {
        return papers.countRecords(related) + resources.count(sourceCode(), related);
    }

    /**
     * 读取当前来源实际存在的发表年份，缺失年份不凭空推断。
     */
    public List<Integer> distinctYears(boolean related) {
        Set<Integer> years = new TreeSet<>(papers.distinctYears(related));
        years.addAll(resources.years(sourceCode(), related));
        return List.copyOf(years);
    }

    /**
     * 按来源自身的业务唯一键批量定位记录，兼容没有PMID的文献或资源。
     */
    public List<SourcePublicationRecord> loadRecordsBySourceKeys(List<String> keys) {
        var resourceIds = keys.stream().filter(key -> key.startsWith("RESOURCE:")).map(key -> key.substring(9)).distinct().toList();
        var paperKeys = keys.stream().filter(key -> !key.startsWith("RESOURCE:")).toList();
        List<SourcePublicationRecord> result = new ArrayList<>();
        if (!paperKeys.isEmpty()) result.addAll(papers.loadRecordsBySourceKeys(paperKeys));
        if (!resourceIds.isEmpty()) result.addAll(resources.keys(sourceCode(), resourceIds));
        return result;
    }

    /**
     * 声明来源是否实现按更新时间加ID的增量读取；不支持时由整理阶段使用全量扫描。
     */
    @Override
    public boolean supportsIncremental() {
        return papers.supportsIncremental();
    }

    /**
     * 分别读取文献及资源变更流，按更新时间和编码后的ID归并，分页上界不包含until。
     */
    @Override
    public List<SourcePublicationRecord> loadChangedRecordsPage(boolean related,
                                                                java.time.LocalDateTime time, long after, java.time.LocalDateTime until, int size) {
        List<SourcePublicationRecord> result = new ArrayList<>();
        for (var original : papers.loadChangedRecordsPage(related, time, after / 2, until, size)) {
            var row = new SourcePublicationRecord();
            org.springframework.beans.BeanUtils.copyProperties(original, row);
            row.setSourceRowId(Math.multiplyExact(row.getSourceRowId(), 2));
            result.add(row);
        }
        long resourceAfter = after % 2 == 0 ? Math.max(0, after / 2 - 1) : after / 2;
        for (var original : resources.changedPage(sourceCode(), related, time, resourceAfter, until, size)) {
            var row = new SourcePublicationRecord();
            org.springframework.beans.BeanUtils.copyProperties(original, row);
            row.setSourceRowId(Math.addExact(Math.multiplyExact(row.getSourceRowId(), 2), 1));
            result.add(row);
        }
        return result.stream().filter(row -> row.getSourceUpdatedTime().isAfter(time)
                        || row.getSourceUpdatedTime().equals(time) && row.getSourceRowId() > after)
                .sorted(Comparator.comparing(SourcePublicationRecord::getSourceUpdatedTime)
                        .thenComparing(SourcePublicationRecord::getSourceRowId)).limit(size).toList();
    }
}

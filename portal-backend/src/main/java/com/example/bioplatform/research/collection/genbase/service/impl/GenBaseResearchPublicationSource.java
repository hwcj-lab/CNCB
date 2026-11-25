package com.example.bioplatform.research.collection.genbase.service.impl;

import com.example.bioplatform.research.collection.common.dto.SourcePublicationRecord;
import com.example.bioplatform.research.statistics.mapper.ResearchSnapshotMapper;
import com.example.bioplatform.research.collection.common.service.ResearchPublicationSource;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * GenBase支持文章表的统计来源适配器。
 */
@Component
@RequiredArgsConstructor
public class GenBaseResearchPublicationSource implements ResearchPublicationSource {
    private final ResearchSnapshotMapper mapper;

    @Override
    public String sourceCode() {
        return "GENBASE";
    }

    @Override
    public String sourceName() {
        return "GenBase支持文章";
    }

    /**
     * 使用ID游标返回有限批次，避免快照阶段全量加载GenBase记录。
     */
    @Override
    public List<SourcePublicationRecord> loadRecordsPage(boolean includeRelated, long afterId, int pageSize) {
        return mapper.selectGenBaseRecordPage(includeRelated, afterId, pageSize);
    }

    /**
     * 按PMID或DOI返回本来源记录，供跨库整合字段互补。
     */
    @Override
    public List<SourcePublicationRecord> loadRecordsByIdentifiers(List<String> pmids, List<String> dois) {
        List<String> safePmids = pmids == null ? List.of() : pmids.stream().filter(java.util.Objects::nonNull).distinct().toList();
        List<String> safeDois = dois == null ? List.of() : dois.stream().filter(java.util.Objects::nonNull).distinct().toList();
        return safePmids.isEmpty() && safeDois.isEmpty() ? List.of()
                : mapper.selectGenBaseRecordsByIdentifiers(safePmids, safeDois);
    }

    @Override
    public long countRecords(boolean includeRelated) {
        return mapper.countGenBaseSourceRecords(includeRelated);
    }

    @Override
    public List<Integer> distinctYears(boolean includeRelated) {
        return mapper.selectGenBaseYears(includeRelated);
    }

    /**
     * 按GenBase自身paper_id优先、PMID/DOI兜底解析业务键，并分组批量查询。
     */
    @Override
    public List<SourcePublicationRecord> loadRecordsBySourceKeys(List<String> keys) {
        List<Long> paperIds = new ArrayList<>();
        List<String> pmids = new ArrayList<>();
        List<String> dois = new ArrayList<>();
        if (keys != null) for (String key : keys) {
            if (key == null) continue;
            int separator = key.indexOf(':');
            if (separator < 1) continue;
            String type = key.substring(0, separator).toUpperCase(java.util.Locale.ROOT);
            String value = key.substring(separator + 1);
            try {
                if ("PAPER".equals(type)) paperIds.add(Long.valueOf(value));
                else if ("PMID".equals(type)) pmids.add(value);
                else if ("DOI".equals(type)) dois.add(value);
            } catch (NumberFormatException ignored) { /* 跳过格式错误的单个来源键 */ }
        }
        Map<String, SourcePublicationRecord> records = new LinkedHashMap<>();
        if (!paperIds.isEmpty()) mapper.selectGenBaseRecordsByPaperIds(paperIds.stream().distinct().toList())
                .forEach(row -> records.put(row.getSourceRecordKey(), row));
        if (!pmids.isEmpty() || !dois.isEmpty()) mapper.selectGenBaseRecordsByIdentifiers(
                        pmids.stream().distinct().toList(), dois.stream().distinct().toList())
                .forEach(row -> records.put(row.getSourceRecordKey(), row));
        return records.values().stream().toList();
    }
}

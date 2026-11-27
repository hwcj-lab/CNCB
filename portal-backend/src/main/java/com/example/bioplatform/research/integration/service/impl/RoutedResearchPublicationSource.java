package com.example.bioplatform.research.integration.service.impl;

import com.example.bioplatform.research.collection.common.dto.SourcePublicationRecord;
import com.example.bioplatform.research.collection.common.mapper.RoutedPublicationMapper;
import com.example.bioplatform.research.collection.common.service.ResearchPublicationSource;
import com.example.bioplatform.research.collection.openlb.service.impl.OpenLbRoutingService;

import java.util.List;

/**
 * 配置驱动的实际数据库来源；新增同结构来源无需修改快照算法。
 */
public class RoutedResearchPublicationSource implements ResearchPublicationSource {
    private final String code, name, table;
    private final RoutedPublicationMapper mapper;

    public RoutedResearchPublicationSource(String code, String name, RoutedPublicationMapper mapper) {
        this.code = code;
        this.name = name;
        this.mapper = mapper;
        this.table = OpenLbRoutingService.table(code);
    }

    public String sourceCode() {
        return code;
    }

    public String sourceName() {
        return name;
    }

    private List<SourcePublicationRecord> tag(List<SourcePublicationRecord> rows) {
        rows.forEach(r -> r.setSourceCode(code));
        return rows;
    }

    public List<SourcePublicationRecord> loadRecordsPage(boolean related, long after, int size) {
        return tag(mapper.page(table, related, after, size));
    }

    public List<SourcePublicationRecord> loadRecordsByIdentifiers(List<String> pmids, List<String> dois) {
        return tag(mapper.identifiers(table, pmids == null ? List.of() : pmids, dois == null ? List.of() : dois));
    }

    public long countRecords(boolean related) {
        return mapper.count(table, related);
    }

    public List<Integer> distinctYears(boolean related) {
        return mapper.years(table, related);
    }

    public List<SourcePublicationRecord> loadRecordsBySourceKeys(List<String> keys) {
        return loadRecordsByIdentifiers(keys.stream().filter(k -> k.startsWith("PMID:")).map(k -> k.substring(5)).toList(), List.of());
    }

    @Override
    public boolean supportsIncremental() {
        return true;
    }

    @Override
    public List<SourcePublicationRecord> loadChangedRecordsPage(boolean related,
                                                                java.time.LocalDateTime time, long after, java.time.LocalDateTime until, int size) {
        return tag(mapper.changedPage(table, related, time, after, until, size));
    }
}

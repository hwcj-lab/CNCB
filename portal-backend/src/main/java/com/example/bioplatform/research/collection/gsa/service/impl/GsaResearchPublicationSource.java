package com.example.bioplatform.research.collection.gsa.service.impl;

import com.example.bioplatform.research.collection.common.dto.SourcePublicationRecord;
import com.example.bioplatform.research.collection.common.service.ResearchPublicationSource;
import com.example.bioplatform.research.collection.gsa.mapper.GsaSourceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * GSA专用统计适配器：同时支持列表无PMID文献及OpenLB分流记录。
 */
@Component
@RequiredArgsConstructor
public class GsaResearchPublicationSource implements ResearchPublicationSource {
    private final GsaSourceMapper mapper;
    private final com.example.bioplatform.research.collection.openlb.mapper.OpenLbRoutingMapper routes;

    public boolean enabled() {
        return routes.routes().stream().anyMatch(r -> "GSA".equals(r.get("source_code")));
    }

    public String sourceCode() {
        return "GSA";
    }

    public String sourceName() {
        return "GSA支持文章";
    }

    public List<SourcePublicationRecord> loadRecordsPage(boolean related, long after, int size) {
        return mapper.page(related, after, size);
    }

    public List<SourcePublicationRecord> loadRecordsByIdentifiers(List<String> pmids, List<String> dois) {
        return mapper.identifiers(pmids == null ? List.of() : pmids, dois == null ? List.of() : dois);
    }

    public long countRecords(boolean related) {
        return mapper.count(related);
    }

    public List<Integer> distinctYears(boolean related) {
        return mapper.years(related);
    }

    public List<SourcePublicationRecord> loadRecordsBySourceKeys(List<String> keys) {
        if (keys == null || keys.isEmpty()) return List.of();
        var rows = mapper.keys(keys);
        var result = new ArrayList<SourcePublicationRecord>();
        for (String key : keys)
            for (var row : rows) {
                if (key.equals(row.getSourceRecordKey()) || key.equals("PMID:" + row.getPmid()) || key.equals("DOI:" + row.getDoi()) || key.equals("LOCAL_ID:" + row.getSourceRowId())) {
                    var copy = new SourcePublicationRecord();
                    org.springframework.beans.BeanUtils.copyProperties(row, copy);
                    copy.setSourceRecordKey(key);
                    result.add(copy);
                    break;
                }
            }
        return result;
    }

    @Override
    public boolean supportsIncremental() {
        return true;
    }

    @Override
    public List<SourcePublicationRecord> loadChangedRecordsPage(boolean related,
                                                                java.time.LocalDateTime time, long after, java.time.LocalDateTime until, int size) {
        return mapper.changedPage(related, time, after, until, size);
    }
}

package com.example.bioplatform.research.collection.openlb.service.impl;

import com.example.bioplatform.research.collection.common.dto.SourcePublicationRecord;
import com.example.bioplatform.research.collection.common.service.ResearchPublicationSource;
import com.example.bioplatform.research.statistics.mapper.ResearchSnapshotMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * OpenLB原始采集表的统计来源适配器。
 */
@Component
@RequiredArgsConstructor
public class OpenLbResearchPublicationSource implements ResearchPublicationSource {
    /**
     * mapper：本组件使用的持久层接口，SQL由对应XML维护。
     */
    private final ResearchSnapshotMapper mapper;

    /**
     * 返回该来源是否参与注册和统计选择；停用不代表删除已有采集数据。
     */
    @Override
    public boolean enabled() {
        return false;
    }

    /**
     * 返回适配器稳定的数据库编码，用于路由、整理范围和分库统计。
     */
    @Override
    public String sourceCode() {
        return "OPENLB";
    }

    /**
     * 返回来源展示名称；统计内部仍使用sourceCode作为身份。
     */
    @Override
    public String sourceName() {
        return "OpenLB文献库";
    }

    /**
     * 使用ID游标返回有限批次，避免快照阶段全量加载OpenLB记录。
     */
    @Override
    public List<SourcePublicationRecord> loadRecordsPage(boolean includeRelated, long afterId, int pageSize) {
        return mapper.selectOpenLbRecordPage(includeRelated, afterId, pageSize);
    }

    /**
     * 按PMID或DOI返回本来源可匹配的文献，用于跨来源补充完整字段。
     */
    @Override
    public List<SourcePublicationRecord> loadRecordsByIdentifiers(List<String> pmids, List<String> dois) {
        List<String> safePmids = pmids == null ? List.of() : pmids.stream().filter(Objects::nonNull).distinct().toList();
        List<String> safeDois = dois == null ? List.of() : dois.stream().filter(Objects::nonNull).distinct().toList();
        return safePmids.isEmpty() && safeDois.isEmpty() ? List.of()
                : mapper.selectOpenLbRecordsByIdentifiers(safePmids, safeDois);
    }

    /**
     * 按本来源及关联记录口径计数；此处不是跨数据库的全局去重总量。
     */
    @Override
    public long countRecords(boolean includeRelated) {
        return mapper.countOpenLbSourceRecords(includeRelated);
    }

    /**
     * 读取当前来源实际存在的发表年份，缺失年份不凭空推断。
     */
    @Override
    public List<Integer> distinctYears(boolean includeRelated) {
        return mapper.selectOpenLbYears(includeRelated);
    }

    /**
     * 解析OPENLB PMID业务键并通过PMID索引批量取回来源原始记录。
     */
    @Override
    public List<SourcePublicationRecord> loadRecordsBySourceKeys(List<String> keys) {
        List<String> pmids = keys == null ? List.of() : keys.stream().filter(Objects::nonNull)
                .filter(key -> key.startsWith("PMID:")).map(key -> key.substring("PMID:".length()))
                .filter(value -> !value.isBlank()).distinct().toList();
        return pmids.isEmpty() ? List.of() : mapper.selectOpenLbRecordsByPmids(pmids);
    }
}

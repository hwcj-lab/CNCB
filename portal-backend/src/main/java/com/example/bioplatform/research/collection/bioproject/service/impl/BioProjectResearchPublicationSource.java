package com.example.bioplatform.research.collection.bioproject.service.impl;

import com.example.bioplatform.research.collection.bioproject.mapper.BioProjectSourceMapper;
import com.example.bioplatform.research.collection.common.dto.SourcePublicationRecord;
import com.example.bioplatform.research.collection.common.service.ResearchPublicationSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * BIOPROJECT专用统计适配器：同时支持列表无PMID文献及OpenLB分流记录。
 */
@Component
@RequiredArgsConstructor
public class BioProjectResearchPublicationSource implements ResearchPublicationSource {
    private final BioProjectSourceMapper mapper;
    private final com.example.bioplatform.research.collection.openlb.mapper.OpenLbRoutingMapper routes;

    /**
     * 返回该来源是否参与注册和统计选择；停用不代表删除已有采集数据。
     */
    public boolean enabled() {
        return routes.routes().stream().anyMatch(r -> "BIOPROJECT".equals(r.get("source_code")));
    }

    /**
     * 返回适配器稳定的数据库编码，用于路由、整理范围和分库统计。
     */
    public String sourceCode() {
        return "BIOPROJECT";
    }

    /**
     * 返回来源展示名称；统计内部仍使用sourceCode作为身份。
     */
    public String sourceName() {
        return "BIOPROJECT项目出版文献";
    }

    /**
     * 按来源适配器的稳定行ID读取一批记录；includeRelated控制是否包含非本库关联记录。
     */
    public List<SourcePublicationRecord> loadRecordsPage(boolean related, long after, int size) {
        return mapper.page(related, after, size);
    }

    /**
     * 按已有PMID或DOI批量读取本来源记录；不会通过标题搜索并补写PMID。
     */
    public List<SourcePublicationRecord> loadRecordsByIdentifiers(List<String> pmids, List<String> dois) {
        return mapper.identifiers(pmids == null ? List.of() : pmids, dois == null ? List.of() : dois);
    }

    /**
     * 按本来源及关联记录口径计数；此处不是跨数据库的全局去重总量。
     */
    public long countRecords(boolean related) {
        return mapper.count(related);
    }

    /**
     * 读取当前来源实际存在的发表年份，缺失年份不凭空推断。
     */
    public List<Integer> distinctYears(boolean related) {
        return mapper.years(related);
    }

    /**
     * 按来源自身的业务唯一键批量定位记录，兼容没有PMID的文献或资源。
     */
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

    /**
     * 声明来源是否实现按更新时间加ID的增量读取；不支持时由整理阶段使用全量扫描。
     */
    @Override
    public boolean supportsIncremental() {
        return true;
    }

    /**
     * 按更新时间和ID组成的游标读取变更，严格排除时间上界；读取顺序必须与整理游标校验一致。
     */
    @Override
    public List<SourcePublicationRecord> loadChangedRecordsPage(boolean related,
                                                                java.time.LocalDateTime time, long after, java.time.LocalDateTime until, int size) {
        return mapper.changedPage(related, time, after, until, size);
    }
}

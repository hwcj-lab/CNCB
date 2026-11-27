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
    /**
     * code：当前来源、策略或业务响应使用的编码。
     * name：当前对象或资源的展示名称。
     * table：服务端白名单提供的目标表名，不接收任意前端表名。
     */
    private final String code, name, table;
    /**
     * mapper：本组件使用的持久层接口，SQL由对应XML维护。
     */
    private final RoutedPublicationMapper mapper;

    /**
     * 构造RoutedResearchPublicationSource，注入本组件所需依赖并建立初始状态。
     * @param code 当前来源、策略或业务响应使用的编码
     * @param name 当前对象或资源的展示名称
     * @param mapper 本组件使用的持久层接口，SQL由对应XML维护
     */
    public RoutedResearchPublicationSource(String code, String name, RoutedPublicationMapper mapper) {
        this.code = code;
        this.name = name;
        this.mapper = mapper;
        this.table = OpenLbRoutingService.table(code);
    }

    /**
     * 返回适配器稳定的数据库编码，用于路由、整理范围和分库统计。
     */
    public String sourceCode() {
        return code;
    }

    /**
     * 返回来源展示名称；统计内部仍使用sourceCode作为身份。
     */
    public String sourceName() {
        return name;
    }

    /**
     * 返回当前来源的内部标记，用于来源记录构造和范围区分。
     * @param rows 本批待处理记录
     * @return 按上述规则计算或读取的List<SourcePublicationRecord>结果
     */
    private List<SourcePublicationRecord> tag(List<SourcePublicationRecord> rows) {
        rows.forEach(r -> r.setSourceCode(code));
        return rows;
    }

    /**
     * 按来源适配器的稳定行ID读取一批记录；includeRelated控制是否包含非本库关联记录。
     */
    public List<SourcePublicationRecord> loadRecordsPage(boolean related, long after, int size) {
        return tag(mapper.page(table, related, after, size));
    }

    /**
     * 按已有PMID或DOI批量读取本来源记录；不会通过标题搜索并补写PMID。
     */
    public List<SourcePublicationRecord> loadRecordsByIdentifiers(List<String> pmids, List<String> dois) {
        return tag(mapper.identifiers(table, pmids == null ? List.of() : pmids, dois == null ? List.of() : dois));
    }

    /**
     * 按本来源及关联记录口径计数；此处不是跨数据库的全局去重总量。
     */
    public long countRecords(boolean related) {
        return mapper.count(table, related);
    }

    /**
     * 读取当前来源实际存在的发表年份，缺失年份不凭空推断。
     */
    public List<Integer> distinctYears(boolean related) {
        return mapper.years(table, related);
    }

    /**
     * 按来源自身的业务唯一键批量定位记录，兼容没有PMID的文献或资源。
     */
    public List<SourcePublicationRecord> loadRecordsBySourceKeys(List<String> keys) {
        return loadRecordsByIdentifiers(keys.stream().filter(k -> k.startsWith("PMID:")).map(k -> k.substring(5)).toList(), List.of());
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
        return tag(mapper.changedPage(table, related, time, after, until, size));
    }
}

package com.example.bioplatform.research.statistics.mapper;

import com.example.bioplatform.research.collection.common.dto.IntegratedPublicationRecord;
import com.example.bioplatform.research.collection.common.dto.ResearchDatabaseTopicMapping;
import com.example.bioplatform.research.collection.common.dto.ResearchFundDictionaryEntry;
import com.example.bioplatform.research.statistics.dto.ResearchSnapshotCreate;
import com.example.bioplatform.research.collection.common.dto.SnapshotDimensionCandidateRow;
import com.example.bioplatform.research.collection.common.dto.SnapshotDimensionPublicationRow;
import com.example.bioplatform.research.collection.common.dto.SnapshotDimensionSeedRow;
import com.example.bioplatform.research.collection.common.dto.SnapshotPublicationRow;
import com.example.bioplatform.research.collection.common.dto.SourcePublicationRecord;
import com.example.bioplatform.research.statistics.vo.ResearchSnapshotDetail;
import com.example.bioplatform.research.statistics.vo.ResearchSnapshotDimension;
import com.example.bioplatform.research.statistics.vo.ResearchSnapshotPublication;


import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 科研统计来源读取、快照落库和历史结果查询 Mapper。
 */
@Mapper
public interface ResearchSnapshotMapper {
    /**
     * 查询本方法指定的记录；涉及research_stat_publication表。
     * SQL定义见 ResearchSnapshotMapper.xml 中同名语句。
     */
    List<IntegratedPublicationRecord> selectIntegratedIdentityMatches(@Param("keys") List<String> keys, @Param("pmids") List<String> pmids, @Param("dois") List<String> dois);

    /**
     * 更新本方法指定的记录；涉及research_stat_publication表。
     * SQL定义见 ResearchSnapshotMapper.xml 中同名语句。
     */
    int deactivateIntegratedKeys(@Param("keys") List<String> keys);

    /**
     * 查询本方法指定的记录；涉及research_stat_publication表。
     * SQL定义见 ResearchSnapshotMapper.xml 中同名语句。
     */
    List<IntegratedPublicationRecord> selectIntegratedRecordPageForMode(@Param("includeRelated") boolean related, @Param("includeResources") boolean resources, @Param("afterId") long after, @Param("pageSize") int size);

    /**
     * 查询本方法指定的记录；涉及research_stat_publication表。
     * SQL定义见 ResearchSnapshotMapper.xml 中同名语句。
     */
    long countIntegratedRecordsForMode(@Param("includeRelated") boolean related, @Param("includeResources") boolean resources);

    /**
     * 查询本方法指定的记录；涉及research_stat_publication表。
     * SQL定义见 ResearchSnapshotMapper.xml 中同名语句。
     */
    List<Integer> selectIntegratedYearsForMode(@Param("includeRelated") boolean related, @Param("includeResources") boolean resources);

    /**
     * 根据OpenLB采集批次的PMID读取变化记录及其关系字段。
     */
    List<SourcePublicationRecord> selectOpenLbRecordsByPmids(@Param("pmids") List<String> pmids);

    /**
     * 根据GenBase采集批次的paper_id读取变化记录及其关系字段。
     */
    List<SourcePublicationRecord> selectGenBaseRecordsByPaperIds(@Param("paperIds") List<Long> paperIds);

    /**
     * 根据PMID/DOI读取跨来源同一文献，合并补足字段。
     */
    List<SourcePublicationRecord> selectOpenLbRecordsByIdentifiers(@Param("pmids") List<String> pmids,
                                                                   @Param("dois") List<String> dois);

    /**
     * 根据PMID/DOI读取跨来源同一文献，合并补足字段。
     */
    List<SourcePublicationRecord> selectGenBaseRecordsByIdentifiers(@Param("pmids") List<String> pmids,
                                                                    @Param("dois") List<String> dois);

    /**
     * 按整合表主键游标分页读取快照输入，避免将全量文献一次装入堆内存。
     */
    List<IntegratedPublicationRecord> selectIntegratedRecordPage(@Param("includeRelated") boolean includeRelated,
                                                                 @Param("afterId") long afterId,
                                                                 @Param("pageSize") int pageSize);

    /**
     * OpenLB适配器使用的游标分页读取；新增来源应由自己的适配器提供对应分页SQL。
     */
    List<SourcePublicationRecord> selectOpenLbRecordPage(@Param("includeRelated") boolean includeRelated,
                                                         @Param("afterId") long afterId,
                                                         @Param("pageSize") int pageSize);

    /**
     * GenBase适配器使用的游标分页读取；新增来源应由自己的适配器提供对应分页SQL。
     */
    List<SourcePublicationRecord> selectGenBaseRecordPage(@Param("includeRelated") boolean includeRelated,
                                                          @Param("afterId") long afterId,
                                                          @Param("pageSize") int pageSize);

    /**
     * 查询OpenLB来源文献总量，用于全量整合进度估算。
     */
    long countOpenLbSourceRecords(@Param("includeRelated") boolean includeRelated);

    /**
     * 查询GenBase来源文献总量，用于全量整合进度估算。
     */
    long countGenBaseSourceRecords(@Param("includeRelated") boolean includeRelated);

    /**
     * 查询OpenLB实际年份，避免为不存在的年份创建维度。
     */
    List<Integer> selectOpenLbYears(@Param("includeRelated") boolean includeRelated);

    /**
     * 查询GenBase实际年份，避免为不存在的年份创建维度。
     */
    List<Integer> selectGenBaseYears(@Param("includeRelated") boolean includeRelated);

    /**
     * 将解析后的维度关联候选批量写入快照暂存表，主键负责跨批去重。
     */
    int insertDimensionCandidates(@Param("rows") List<SnapshotDimensionCandidateRow> rows);

    /**
     * 预建少量年度、主题和数据库维度；年度/主题之后直接写正式穿透关系。
     */
    int insertDimensionSeeds(@Param("rows") List<SnapshotDimensionSeedRow> rows);

    /**
     * 批量读取年度、主题维度与ID，用于边扫记录边批量落穿透关系。
     */
    List<SnapshotDimensionSeedRow> selectDimensionSeeds(@Param("snapshotId") long snapshotId);

    /**
     * 将年度、主题的文献唯一键直接写入正式穿透关系表。
     */
    int insertDimensionPublications(@Param("rows") List<SnapshotDimensionPublicationRow> rows);

    /**
     * 由正式关系表回算年度、主题计数并删除没有关联文献的种子维度。
     */
    int refreshSeedDimensionCounts(@Param("snapshotId") long snapshotId);

    /**
     * 删除没有关联记录的年度/主题维度。
     */
    int deleteEmptySeedDimensions(@Param("snapshotId") long snapshotId);

    /**
     * 数据库端聚合作者、机构、基金候选并只物化各范围Top 50。
     */
    int materializeDimensionsFromCandidates(@Param("snapshotId") long snapshotId);

    /**
     * 从候选表批量建立作者、机构、基金穿透关系；只关联已物化的维度。
     */
    int materializeDimensionPublications(@Param("snapshotId") long snapshotId);

    /**
     * 清理某个快照的中间候选数据。
     */
    int deleteDimensionCandidates(@Param("snapshotId") long snapshotId);

    /**
     * 清理快照失败或主动停止时已生成的维度，外键级联清理对应穿透键。
     */
    int deleteSnapshotDimensions(@Param("snapshotId") long snapshotId);

    /**
     * 清理非构建中快照遗留的暂存行，处理进程异常退出。
     */
    int deleteOrphanDimensionCandidates();

    /**
     * 启动恢复时清理失败/停止构建遗留维度，其穿透键由外键级联删除。
     */
    int deleteIncompleteSnapshotDimensions();

    /**
     * 统计全局整合表当前可统计记录数。
     */
    long countIntegratedRecords(@Param("includeRelated") boolean includeRelated);

    /**
     * 读取全局整合表中实际存在的年份，用于精确预建总统计年度维度。
     */
    List<Integer> selectIntegratedYears(@Param("includeRelated") boolean includeRelated);

    /**
     * 读取已确认且启用的资源数据库到研究主题映射；快照构建时一次读取，避免逐文献查询。
     */
    List<com.example.bioplatform.research.collection.common.dto.ResearchDatabaseTopicMapping> selectConfirmedDatabaseTopics();

    /**
     * 读取全部启用的主题标签，包含当前没有文献的标签。
     */
    List<com.example.bioplatform.research.collection.common.dto.ResearchDatabaseTopicMapping> selectEnabledTopicCategories();

    /**
     * 批量读取本页缺少agency的基金编号全称；无有效字典名称的基金不计入统计。
     */
    List<ResearchFundDictionaryEntry> selectFundDictionaryByGrantIds(@Param("grantIds") List<String> grantIds);

    /**
     * 开始全量重建时，将当前整合记录逻辑标记为非活动；历史快照引用仍保留。
     */
    int deactivateAllIntegratedPublications();

    /**
     * 获取下一个人工统计版本号。
     */
    long selectNextVersionNo();

    /**
     * 创建BUILDING状态快照并回填主键。
     */
    int insertSnapshot(ResearchSnapshotCreate row);

    /**
     * 统计数据库中仍处于BUILDING状态的快照；只要大于0就禁止再次创建。
     */
    long countBuildingSnapshots();

    /**
     * 读取任意一条正在构建的快照，兼容异常情况下存在多条BUILDING记录。
     */
    ResearchSnapshotDetail selectAnyBuildingSnapshot();

    /**
     * 批量新增或按内容摘要更新全局去重文献。
     */
    int insertPublications(@Param("rows") List<SnapshotPublicationRow> rows);

    /**
     * 按全局文献业务键批量读取总统计穿透详情。
     */
    List<ResearchSnapshotPublication> selectIntegratedPublicationDetails(@Param("publicationKeys") List<String> publicationKeys);

    /**
     * 成功完成快照并写入覆盖率。
     */
    int markSuccess(@Param("id") long id, @Param("publicationCount") int publicationCount, @Param("resourceCount") long resourceCount,
                    @Param("yearCoverage") int yearCoverage, @Param("topicCoverage") int topicCoverage,
                    @Param("authorCoverage") int authorCoverage, @Param("institutionCoverage") int institutionCoverage,
                    @Param("fundCoverage") int fundCoverage);

    /**
     * 标记构建失败，旧的成功版本仍保持可用。
     */
    int markFailed(@Param("id") long id, @Param("error") String error);

    /**
     * 将主动停止的快照标记为STOPPED。
     */
    int markStopped(@Param("id") long id, @Param("message") String message);

    /**
     * 应用重启时关闭上一次进程遗留的BUILDING版本。
     */
    int failAbandonedBuilding();

    /**
     * 查询指定快照。
     */
    ResearchSnapshotDetail selectDetail(@Param("id") long id);

    /**
     * 查询最近成功快照。
     */
    ResearchSnapshotDetail selectLatestSuccess();

    /**
     * 查询历史快照。
     */
    List<ResearchSnapshotDetail> selectVersions();

    /**
     * 按数量倒序读取指定统计维度。
     */
    List<ResearchSnapshotDimension> selectDimensions(@Param("snapshotId") long snapshotId,
                                                     @Param("scopeCode") String scopeCode,
                                                     @Param("type") String type, @Param("limit") int limit);

    /**
     * 统计指定维度关联的快照文献数量。
     */
    long countDimensionPublications(@Param("snapshotId") long snapshotId,
                                    @Param("dimensionId") long dimensionId);

    /**
     * 查询穿透维度所属范围，决定后续按全局键还是来源键回查。
     */
    String selectDimensionScope(@Param("snapshotId") long snapshotId, @Param("dimensionId") long dimensionId);

    /**
     * 按唯一键稳定分页读取穿透关系；不关联全局数值主键。
     */
    List<String> selectDimensionPublicationKeys(@Param("snapshotId") long snapshotId,
                                                @Param("dimensionId") long dimensionId,
                                                @Param("offset") int offset,
                                                @Param("size") int size);

    /**
     * 删除快照；数据库外键只清理快照维度和穿透关系，全局文献继续保留。
     */
    int deleteSnapshot(@Param("id") long id);

    /**
     * 修改快照展示名称。
     */
    int updateSnapshotName(@Param("id") long id, @Param("name") String name);

    /**
     * 查询本方法指定的记录；涉及genbase_publication表。
     * SQL定义见 ResearchSnapshotMapper.xml 中同名语句。
     */
    List<SourcePublicationRecord> selectGenBaseChangedPage(@Param("related") boolean related, @Param("time") java.time.LocalDateTime time, @Param("after") long after, @Param("until") java.time.LocalDateTime until, @Param("size") int size);
}

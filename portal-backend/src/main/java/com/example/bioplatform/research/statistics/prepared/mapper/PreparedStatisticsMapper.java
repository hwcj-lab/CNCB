package com.example.bioplatform.research.statistics.prepared.mapper;

import com.example.bioplatform.research.statistics.prepared.model.PreparedScope;
import com.example.bioplatform.research.statistics.prepared.model.PreparedSnapshotConfig;
import com.example.bioplatform.research.statistics.vo.ResearchSnapshotDetail;
import com.example.bioplatform.research.statistics.vo.ResearchSnapshotDimension;
import com.example.bioplatform.research.statistics.vo.ResearchSnapshotPublication;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 从已整理且属于指定修订的文献与成员聚合排名、覆盖量和穿透列表；不解析原始JSON。
 */
@Mapper
public interface PreparedStatisticsMapper {
    /**
     * 保存本快照固定使用的整理修订、来源展示名称和主题范围，防止历史结果随配置变化。
     */
    int saveConfig(PreparedSnapshotConfig config);

    /**
     * 按快照ID读取冻结的整理修订、来源名称和总统计分类范围。
     * SQL定义见 PreparedStatisticsMapper.xml 中同名语句。
     */
    PreparedSnapshotConfig config(@Param("id") long id);

    /**
     * 读取新整理模式的快照版本列表；不把缺少prepared配置的旧格式快照混入。
     * SQL定义见 PreparedStatisticsMapper.xml 中同名语句。
     */
    List<ResearchSnapshotDetail> versions();

    /**
     * 返回具有prepared配置且状态SUCCESS的最新快照，排除构建中和失败版本。
     * SQL定义见 PreparedStatisticsMapper.xml 中同名语句。
     */
    ResearchSnapshotDetail latestSuccess();

    /**
     * 在固定整理修订、来源和关联口径下，分别聚合单个维度；COUNT DISTINCT文献键避免同篇多作者或跨来源重复计数。
     */
    List<ResearchSnapshotDimension> rank(@Param("q") PreparedScope scope, @Param("table") String table);

    /**
     * 按版本、来源及统计选项筛选document，并按统一文献键去重计数；TOTAL跨来源，分库限定来源。
     * SQL定义见 PreparedStatisticsMapper.xml 中同名语句。
     */
    long count(@Param("q") PreparedScope scope);

    /**
     * 在相同统计范围下只计算RESOURCE记录，资源身份保持来源业务键。
     * SQL定义见 PreparedStatisticsMapper.xml 中同名语句。
     */
    long resources(@Param("q") PreparedScope scope);

    /**
     * 计算有该维度成员的去重文献数量，不是作者数、机构数或基金数。
     */
    long coverage(@Param("q") PreparedScope scope, @Param("table") String table);

    /**
     * 写入本方法指定的记录；涉及research_stat_dimension表。
     * SQL定义见 PreparedStatisticsMapper.xml 中同名语句。
     */
    int insertDimension(@Param("snapshotId") long snapshotId, @Param("row") ResearchSnapshotDimension row);

    /**
     * 写入本方法指定的记录；涉及research_stat_dimension表。
     * SQL定义见 PreparedStatisticsMapper.xml 中同名语句。
     */
    int insertDimensions(@Param("snapshotId") long snapshotId, @Param("rows") List<ResearchSnapshotDimension> rows);

    /**
     * 查询本方法指定的记录；涉及research_stat_dimension表。
     * SQL定义见 PreparedStatisticsMapper.xml 中同名语句。
     */
    ResearchSnapshotDimension dimension(@Param("snapshotId") long snapshotId, @Param("id") long id);

    /**
     * 按快照修订及维度成员筛选来源文献，按统一文献键合并来源后分页，供排名穿透。
     */
    List<ResearchSnapshotPublication> publications(@Param("q") PreparedScope scope, @Param("table") String table, @Param("offset") long offset, @Param("size") int size);

    /**
     * 删除符合条件的目标记录；涉及research_prepared_snapshot表，关联表仅用于筛选，不表示同时删除。
     * SQL定义见 PreparedStatisticsMapper.xml 中同名语句。
     */
    int deleteConfig(@Param("id") long id);

    /**
     * 尝试取得快照创建命名锁，跨应用进程协调创建操作；获取失败时不等待其他构建结束。
     * SQL定义见 PreparedStatisticsMapper.xml 中同名语句。
     */
    int lock();

    /**
     * 释放当前连接持有的快照创建命名锁，供后续创建请求使用。
     * SQL定义见 PreparedStatisticsMapper.xml 中同名语句。
     */
    int unlock();
}

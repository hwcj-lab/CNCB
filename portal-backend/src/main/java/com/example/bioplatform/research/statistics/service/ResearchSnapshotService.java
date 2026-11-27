package com.example.bioplatform.research.statistics.service;

import com.example.bioplatform.research.collection.common.vo.ResearchPublicationSourceOption;
import com.example.bioplatform.research.statistics.dto.ResearchSnapshotBuildRequest;
import com.example.bioplatform.research.statistics.vo.*;

import java.util.List;

/**
 * 科研统计快照的构建、版本查询与结果读取服务。
 */
public interface ResearchSnapshotService {
    /**
     * 后台启动一次全来源汇总；同一时间只允许一个构建任务。
     */
    ResearchSnapshotStatus startBuild(ResearchSnapshotBuildRequest request);

    /**
     * 返回所有已注册、可由用户选择的统计来源。
     */
    List<ResearchPublicationSourceOption> sourceOptions();

    /**
     * 请求安全停止；当前批次结束后标记STOPPED。
     */
    ResearchSnapshotStatus stopBuild();

    /**
     * 查询当前构建状态以及最近版本。
     */
    ResearchSnapshotStatus status();

    /**
     * 列出历史版本，按版本号倒序。
     */
    List<ResearchSnapshotDetail> versions();

    /**
     * 获取最近一个成功版本。
     */
    ResearchSnapshotDetail latest();

    /**
     * 获取指定版本。
     */
    ResearchSnapshotDetail detail(long snapshotId);

    /**
     * 一次返回最近成功版本及所有仪表盘维度分组。
     */
    ResearchSnapshotResult latestResult(int limitPerDimension);

    /**
     * 一次返回指定历史版本及所有仪表盘维度分组。
     */
    ResearchSnapshotResult result(long snapshotId, int limitPerDimension);

    /**
     * 返回该快照实际包含的数据库，来源随适配器动态扩展。
     */
    List<com.example.bioplatform.research.statistics.vo.ResearchSnapshotDatabaseOption> databases(long snapshotId);

    /**
     * 按已确认的研究主题映射筛选快照中的数据库入口。
     */
    List<com.example.bioplatform.research.statistics.vo.ResearchSnapshotDatabaseOption> topicDatabases(long snapshotId, String topicCode);

    /**
     * 返回指定数据库在同一快照中的五维统计。
     */
    ResearchSnapshotResult databaseResult(long snapshotId, String databaseCode, int limitPerDimension);

    /**
     * 获取指定版本的预聚合维度。
     */
    List<ResearchSnapshotDimension> dimensions(long snapshotId, String type, int limit);

    /**
     * 分页读取某个统计维度下的具体文献。
     */
    ResearchSnapshotPublicationPage dimensionPublications(long snapshotId, long dimensionId, int page, int size);

    /**
     * 删除指定快照及其全部关联数据。
     */
    void deleteSnapshot(long snapshotId);

    /**
     * 修改已生成快照的展示名称。
     */
    ResearchSnapshotDetail renameSnapshot(long snapshotId, String name);
}

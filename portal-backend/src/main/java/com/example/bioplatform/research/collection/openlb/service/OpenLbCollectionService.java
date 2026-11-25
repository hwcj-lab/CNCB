package com.example.bioplatform.research.collection.openlb.service;

import com.example.bioplatform.research.collection.openlb.dto.OpenLbCollectionStartRequest;
import com.example.bioplatform.research.collection.openlb.vo.OpenLbCollectionStatus;


/**
 * OpenLB 文献采集的管理服务。
 * <p>
 * 该服务只负责 OpenLB 原始数据的导入和采集，不将数据同步到科研统计大表。
 * 采集任务状态持久化在 openlb_pmid_task，因此支持进程重启后继续执行。
 */
public interface OpenLbCollectionService {
    /**
     * 从配置的 CSV 文件导入 PMID 任务。
     * 已存在的 PMID 由唯一索引和 INSERT IGNORE 忽略，不覆盖原有采集状态。
     *
     * @return 导入后的采集状态和任务数量
     */
    OpenLbCollectionStatus importCsv();

    /**
     * 启动一个后台采集任务并立即返回。
     * 同一应用实例同时只允许一个 OpenLB 采集任务运行。
     *
     * @param request 并发数、批大小、限速与重试配置
     * @return 启动后的即时状态
     */
    OpenLbCollectionStatus start(OpenLbCollectionStartRequest request);

    /**
     * 请求安全停止当前采集。
     * 已发出的 HTTP 请求允许结束，未处理任务保留为可续传状态。
     *
     * @return 停止请求提交后的状态
     */
    OpenLbCollectionStatus stop();

    /**
     * 将已达最大重试次数的 FAILED 任务重置为 PENDING。
     * SUCCESS 和 EMPTY 不会被重置，避免重复请求。
     *
     * @return 重置后的采集状态
     */
    OpenLbCollectionStatus retryFailed();

    /**
     * 查询运行状态、当前限速间隔和各持久化任务状态的数量。
     *
     * @return OpenLB 采集状态快照
     */
    OpenLbCollectionStatus status();
}

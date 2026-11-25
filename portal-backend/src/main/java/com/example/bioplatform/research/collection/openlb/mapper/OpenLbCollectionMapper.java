package com.example.bioplatform.research.collection.openlb.mapper;

import com.example.bioplatform.research.collection.openlb.dto.OpenLbPublicationWriteRow;
import com.example.bioplatform.research.collection.common.vo.TaskStatusCount;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * OpenLB 采集持久化 Mapper。
 * <p>
 * 所有 SQL 集中在 OpenLbCollectionMapper.xml；方法优先接收集合，
 * 以多行 INSERT 或集合 UPDATE 减少数据库往返。
 */
@Mapper
public interface OpenLbCollectionMapper {
    /**
     * 批量插入 PMID 任务，已存在的唯一 PMID 直接忽略。
     */
    int insertTasksIgnore(@Param("pmids") List<String> pmids);

    /**
     * 将所有 FAILED 任务重置为可执行的 PENDING。
     */
    int retryFailed();

    /**
     * 按任务状态分组统计，用于状态接口。
     */
    List<TaskStatusCount> countByStatus();

    /**
     * 以文献表为准校正任务状态，修复“文献已入库但状态未提交”的异常窗口。
     */
    int reconcileCompletedTasks();

    /**
     * 将上次异常中断遗留的 RUNNING 任务恢复为 PENDING。
     */
    int recoverRunningTasks(@Param("reason") String reason);

    /**
     * 按主键顺序获取一批已到重试时间的 PENDING PMID。
     */
    List<String> findPendingPmids(@Param("limit") int limit);

    /**
     * 使用单条集合 UPDATE 将选中的 PMID 标记为 RUNNING。
     */
    int markRunning(@Param("pmids") List<String> pmids);

    /**
     * 使用多行 INSERT IGNORE 批量写入 cncbBanks 非空的 OpenLB 文献。
     */
    int insertPublicationsIgnore(@Param("rows") List<OpenLbPublicationWriteRow> rows);

    /**
     * 批量将已入库的 PMID 标记为 SUCCESS。
     */
    int markSuccess(@Param("pmids") List<String> pmids);

    /**
     * 批量标记 OpenLB 响应中 cncbBanks 为空的 PMID，这些任务不再重试。
     */
    int markEmpty(@Param("pmids") List<String> pmids);

    /**
     * 批量累加失败次数并写入每条错误信息。
     * 达到 maxAttempts 时改为 FAILED，否则保持 PENDING 并设置下次重试时间。
     */
    int markFailedOrPending(@Param("rows") List<FailureRow> rows, @Param("maxAttempts") int maxAttempts);

    /**
     * 一条失败任务的差异化更新数据，用于 SQL CASE WHEN 批量写入错误。
     */
    record FailureRow(String pmid, String error) {
    }
}

package com.example.bioplatform.research.collection.genbase.mapper;
import com.example.bioplatform.research.collection.genbase.dto.GenBaseOpenLbWriteRow;
import com.example.bioplatform.research.collection.genbase.dto.GenBasePmidTask;
import com.example.bioplatform.research.collection.genbase.dto.GenBasePublicationWriteRow;
import com.example.bioplatform.research.collection.genbase.vo.GenBaseJobState;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** GenBase采集持久层；分页进度和文献批量写入SQL均位于XML。 */
@Mapper
public interface GenBaseCollectionMapper {
    /** 将异常退出遗留的RUNNING作业改为STOPPED，使其可以继续。 */
    int recoverInterruptedJobs(@Param("reason") String reason);

    /** 查询最近一个可恢复的STOPPED或FAILED作业。 */
    GenBaseJobState findResumableJob();

    /** 创建一次从start=0开始的完整扫描作业。 */
    int createJob(@Param("pageSize") int pageSize);

    /** 获取刚创建的作业主键。 */
    Long lastInsertId();

    /** 恢复作业并按本次请求更新页大小。 */
    int resumeJob(@Param("jobId") long jobId, @Param("pageSize") int pageSize);

    /** 查询最近作业，用于状态接口。 */
    GenBaseJobState findLatestJob();

    /** 一条多行INSERT IGNORE保存整页数据，依靠唯一索引去重。 */
    int insertPublicationsIgnore(@Param("rows") List<GenBasePublicationWriteRow> rows);

    /** 当前页入库后原子推进断点和累计数量。 */
    int advanceJob(@Param("jobId") long jobId,
                   @Param("nextStart") int nextStart,
                   @Param("recordsTotal") int recordsTotal,
                   @Param("seen") int seen,
                   @Param("inserted") int inserted);

    /** 记录当前页失败次数和错误，但不推进断点。 */
    int recordFailure(@Param("jobId") long jobId, @Param("error") String error);

    /** 将作业置为最终状态。 */
    int finishJob(@Param("jobId") long jobId, @Param("status") String status, @Param("error") String error);

    /** 恢复进程中断遗留的OpenLB补全任务。 */
    int recoverRunningEnrichment(@Param("reason") String reason);

    /** 批量将GenBase中主列表文献（包含无PMID记录）重置为待OpenLB补全状态。 */
    int resetPmidEnrichment();

    /** 批量领取待补全主文献；PMID可空，成功/NO_PMID结果不自动重复查询。 */
    List<GenBasePmidTask> findPendingEnrichment(@Param("limit") int limit);

    /** 将领取的一批记录标记为RUNNING。 */
    int markEnrichmentRunning(@Param("paperIds") List<Long> paperIds);

    List<GenBasePmidTask> lockEnrichment(@Param("paperIds") List<Long> paperIds);

    /** 一条CASE UPDATE批量回填不同文献的OpenLB详情。 */
    int updateEnrichmentSuccess(@Param("rows") List<GenBaseOpenLbWriteRow> rows);

    /** 批量标记OpenLB没有返回记录的PMID。 */
    int markEnrichmentEmpty(@Param("paperIds") List<Long> paperIds);

    /** 批量记录不同错误；达到上限后FAILED，否则回到PENDING。 */
    int markEnrichmentFailedOrPending(@Param("rows") List<EnrichmentFailure> rows, @Param("maxAttempts") int maxAttempts);

    /** 一条补全失败记录。 */
    record EnrichmentFailure(Long paperId, String error) {}
}


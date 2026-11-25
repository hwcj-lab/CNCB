package com.example.bioplatform.research.collection.common.mapper;

import com.example.bioplatform.research.collection.common.dto.PendingRelationRoot;
import com.example.bioplatform.research.collection.common.dto.RelatedPublicationWriteRow;
import com.example.bioplatform.research.collection.common.dto.RelationQueueNode;
import com.example.bioplatform.research.collection.common.dto.RelationSourceRecord;
import com.example.bioplatform.research.collection.common.vo.PublicationRelationJobStatus;
import com.example.bioplatform.research.collection.common.vo.RelatedPublicationView;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * OpenLB/GenBase关联文献递归采集的作业、队列、文献与无序关系持久层。
 */
@Mapper
public interface PublicationRelationCollectionMapper {
    int updateBioProjectSourceGrants(@Param("identifierType") String type, @Param("identifier") String key, @Param("grantsJson") String grants);

    /**
     * GSA没有PMID时按DOI或稳定列表键回填资源基金。
     */
    int updateGsaSourceGrants(@Param("identifierType") String type, @Param("identifier") String identifier, @Param("grantsJson") String grantsJson);

    /**
     * 人工恢复失败作业时，重新领取其失败节点；成功节点不重复执行。
     */
    @org.apache.ibatis.annotations.Update("UPDATE research_relation_crawl_queue SET status='PENDING',last_error=NULL,update_time=NOW() WHERE job_id=#{jobId} AND status='FAILED'")
    int retryFailedQueue(@Param("jobId") long jobId);

    /**
     * 创建人工触发的关联采集作业。
     */
    int createJob(@Param("databaseCode") String databaseCode, @Param("identifierType") String identifierType,
                  @Param("identifier") String identifier, @Param("refreshPublication") boolean refreshPublication,
                  @Param("refreshRelations") boolean refreshRelations, @Param("maxPublications") int maxPublications,
                  @Param("requestDelayMs") long requestDelayMs);

    /**
     * 获取当前数据库连接最后生成的自增ID。
     */
    Long lastInsertId();

    /**
     * 根文献作为深度0节点写入持久化队列。
     */
    int insertRootQueue(@Param("jobId") long jobId, @Param("identifierType") String identifierType,
                        @Param("identifier") String identifier);

    /**
     * 按状态和主键顺序批量领取来源表中的根文献；查询利用relation_status调度索引。
     */
    List<PendingRelationRoot> selectPendingRoots(@Param("databaseCode") String databaseCode,
                                                 @Param("includeFailed") boolean includeFailed,
                                                 @Param("limit") int limit);

    /**
     * 将一批根文献一次性写入持久化队列，唯一键防止同一作业内重复。
     */
    int insertRootQueueBatch(@Param("jobId") long jobId, @Param("roots") List<PendingRelationRoot> roots);

    /**
     * 查询指定或最近作业状态。
     */
    PublicationRelationJobStatus selectJob(@Param("jobId") Long jobId);

    /**
     * 统计尚未完成的队列节点。
     */
    long countPendingQueue(@Param("jobId") long jobId);

    /**
     * 将作业恢复为运行态。
     */
    int resumeJob(@Param("jobId") long jobId);

    /**
     * 将异常退出遗留的RUNNING队列节点恢复为PENDING。
     */
    int recoverRunningQueue(@Param("jobId") long jobId);

    /**
     * 按深度和ID领取下一个节点。
     */
    RelationQueueNode findNextPending(@Param("jobId") long jobId);

    /**
     * 将队列节点标记为RUNNING。
     */
    int markQueueRunning(@Param("id") long id);

    /**
     * 将队列节点标记为成功或无可下钻关系。
     */
    int finishQueue(@Param("id") long id, @Param("status") String status);

    /**
     * 标记队列节点失败，保留错误供人工检查。
     */
    int failQueue(@Param("id") long id, @Param("error") String error);

    /**
     * 将新发现的文献加入本作业；唯一键同时承担visited集合。
     */
    int insertQueueIgnore(@Param("jobId") long jobId, @Param("identifierType") String identifierType,
                          @Param("identifier") String identifier, @Param("depth") int depth);

    /**
     * 按数据库和唯一值读取来源文献；GenBase支持paperId、pmid和doi。
     */
    RelationSourceRecord selectSourceRecord(@Param("databaseCode") String databaseCode,
                                            @Param("identifierType") String identifierType,
                                            @Param("identifier") String identifier);

    /**
     * OpenLB来源表内批量upsert关联文献；同PMID已存在时只补充关联身份和变化字段。
     */
    int upsertOpenLbRelated(@Param("databaseCode") String databaseCode, @Param("rows") List<RelatedPublicationWriteRow> rows);

    /**
     * GenBase来源表内批量upsert关联文献；关联记录没有paperId也可入库。
     */
    int upsertGenBaseRelated(@Param("rows") List<RelatedPublicationWriteRow> rows);

    /**
     * 新增或更新数据库范围内的无序双向关系。
     */
    int upsertRelation(@Param("databaseCode") String databaseCode,
                       @Param("leftType") String leftType, @Param("leftValue") String leftValue,
                       @Param("rightType") String rightType, @Param("rightValue") String rightValue,
                       @Param("evidenceUrl") String evidenceUrl, @Param("depth") int depth,
                       @Param("jobId") long jobId);

    /**
     * 节点完整刷新成功后，将本轮没有再次发现的旧关系标记失效。
     */
    int deactivateUnseenRelations(@Param("databaseCode") String databaseCode,
                                  @Param("identifierType") String identifierType,
                                  @Param("identifier") String identifier, @Param("jobId") long jobId);

    /**
     * 设置一篇来源文献的关联状态。
     */
    int updatePublicationRelationStatus(@Param("databaseCode") String databaseCode,
                                        @Param("identifierType") String identifierType,
                                        @Param("identifier") String identifier,
                                        @Param("status") String status, @Param("error") String error);

    /**
     * 将资源详情中发现的基金层级信息写回该数据库来源文献。
     */
    int updateSourceGrants(@Param("databaseCode") String databaseCode, @Param("pmid") String pmid,
                           @Param("grantsJson") String grantsJson);

    /**
     * 原子累计作业处理、新发现和关系数量。
     */
    int incrementJob(@Param("jobId") long jobId, @Param("processed") int processed,
                     @Param("discovered") int discovered, @Param("relations") int relations);

    /**
     * 写入作业最终或暂停状态。
     */
    int finishJob(@Param("jobId") long jobId, @Param("status") String status, @Param("error") String error);

    /**
     * 统计一篇文献的有效双向关联数量。
     */
    long countRelations(@Param("databaseCode") String databaseCode, @Param("identifierType") String identifierType,
                        @Param("identifier") String identifier);

    /**
     * 分页读取关联另一端及其来源表文献摘要。
     */
    List<RelatedPublicationView> selectRelations(@Param("databaseCode") String databaseCode,
                                                 @Param("identifierType") String identifierType,
                                                 @Param("identifier") String identifier,
                                                 @Param("offset") int offset, @Param("size") int size);
}

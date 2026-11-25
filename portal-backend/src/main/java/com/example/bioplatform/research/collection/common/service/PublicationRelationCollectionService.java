package com.example.bioplatform.research.collection.common.service;
import com.example.bioplatform.research.collection.common.dto.PendingRelationBatchRequest;
import com.example.bioplatform.research.collection.common.dto.PublicationRelationCollectionRequest;
import com.example.bioplatform.research.collection.common.vo.PublicationRelationJobStatus;
import com.example.bioplatform.research.collection.common.vo.RelatedPublicationPage;


/** OpenLB和GenBase单篇文献重新采集及递归关联发现服务。 */
public interface PublicationRelationCollectionService {
    /** 人工启动一个后台作业；本方法立即返回，不在HTTP线程中执行递归采集。 */
    PublicationRelationJobStatus start(PublicationRelationCollectionRequest request);
    /** 从指定来源表自动领取PENDING（可选FAILED）文献，分批创建持久化关联作业。 */
    PublicationRelationJobStatus startPendingBatch(PendingRelationBatchRequest request);
    /** 请求当前节点处理结束后停止，持久化队列保留以便继续。 */
    PublicationRelationJobStatus stop();
    /** 继续最近一个PAUSED、STOPPED或FAILED作业。 */
    PublicationRelationJobStatus resume(long jobId);
    /** 查询指定作业；jobId为空时查询最近作业。 */
    PublicationRelationJobStatus status(Long jobId);
    /** 按双向关系分页查询某篇文献的全部有效关联，供详情页按钮使用。 */
    RelatedPublicationPage relatedPublications(String databaseCode, String identifierType, String identifier, int page, int size);
}

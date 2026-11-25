package com.example.bioplatform.research.integration.service;
import com.example.bioplatform.research.collection.common.vo.PublicationIntegrationStatus;

import java.util.List;

/** 更新全局去重文献整合表的服务；采集批次增量调用，人工操作可触发全量重建。 */
public interface ResearchPublicationIntegrationService {
    /** 按刚写入或更新的PMID同步对应来源文献。 */
    void syncOpenLbPmids(List<String> pmids);
    /** 按GenBase paper_id同步对应来源文献。 */
    void syncGenBasePaperIds(List<Long> paperIds);
    /** 按PMID/DOI同步相关来源记录，用于关联采集写入。 */
    void syncIdentifiers(List<String> pmids, List<String> dois);
    /** 按来源稳定业务键同步一批文献，也支持暂时没有PMID/DOI的原始记录。 */
    void syncSourceKeys(String sourceCode, List<String> sourceKeys);
    /** 异步启动一次全量重建，保持文献主键以兼容历史快照穿透。 */
    PublicationIntegrationStatus startFullRebuild();
    /** 查询整合表重建状态。 */
    PublicationIntegrationStatus status();
}

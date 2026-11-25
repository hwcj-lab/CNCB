package com.example.bioplatform.research.collection.genbase.service;
import com.example.bioplatform.research.collection.genbase.dto.GenBaseCollectionStartRequest;
import com.example.bioplatform.research.collection.genbase.vo.GenBaseCollectionStatus;


/** 独立的GenBase支持文章采集服务，不与OpenLB任务队列混用。 */
public interface GenBaseCollectionService {
    /** 启动新作业，或从最近一个STOPPED/FAILED作业的nextStart恢复。 */
    GenBaseCollectionStatus start(GenBaseCollectionStartRequest request);

    /** 请求当前页完成后安全停止并持久化断点。 */
    GenBaseCollectionStatus stop();

    /** 将GenBase主列表文献（含无PMID记录）重新标记为待OpenLB补全，不删除文献或重置OpenLB导入任务。 */
    GenBaseCollectionStatus resetEnrichment();

    /** 查询进程状态及数据库中最近一次作业进度。 */
    GenBaseCollectionStatus status();
}


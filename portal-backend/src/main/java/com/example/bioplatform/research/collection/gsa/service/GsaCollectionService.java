package com.example.bioplatform.research.collection.gsa.service;

import com.example.bioplatform.research.collection.gsa.dto.GsaCollectionStartRequest;

import java.util.Map;

/**
 * GSA主文献采集与补全；关联采集由现有公共关联接口控制。
 */
public interface GsaCollectionService {
    /**
     * 校验参数及运行状态后启动后台处理，调用立即返回当前状态，后续通过状态接口查看结果。
     * @param request 调用方传入并校验后的请求参数
     * @param restart 是否开始新的来源列表扫描
     * @return 按上述规则计算或读取的Map<String, Object>结果
     */
    Map<String, Object> start(GsaCollectionStartRequest request, boolean restart);

    /**
     * 请求后台在安全边界停止，已提交的数据和断点保留，不清空原始记录。
     * @return 按上述规则计算或读取的Map<String, Object>结果
     */
    Map<String, Object> stop();

    /**
     * 返回当前进程标记和持久化任务状态，不启动新的采集或统计任务。
     * @return 按上述规则计算或读取的Map<String, Object>结果
     */
    Map<String, Object> status();

    /**
     * 重置本来源元数据补全状态，后续任务重新领取待补全记录；不删除原始文献。
     * @return 按上述规则计算或读取的Map<String, Object>结果
     */
    Map<String, Object> resetEnrichment();
}

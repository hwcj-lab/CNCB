package com.example.bioplatform.research.collection.openlb.controller;

import com.example.bioplatform.common.ApiResponse;
import com.example.bioplatform.research.collection.openlb.mapper.OpenLbRoutingMapper;
import com.example.bioplatform.research.collection.openlb.service.impl.OpenLbBankInventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * OpenLB来源盘点独立接口：不修改采集任务状态，不生成统计快照。
 */
@RestController
@RequestMapping("/api/collection/openlb/banks")
@RequiredArgsConstructor
public class OpenLbBankInventoryController {
    /**
     * routing：OpenLB来源分流组件。
     */
    private final com.example.bioplatform.research.collection.openlb.service.impl.OpenLbRoutingService routing;
    /**
     * integration：旧全量文献整合服务。
     */
    private final com.example.bioplatform.research.integration.service.ResearchPublicationIntegrationService integration;
    /**
     * service：控制器依赖的业务服务，不在控制器中实现持久化逻辑。
     */
    private final OpenLbBankInventoryService service;
    /**
     * mapper：本组件使用的持久层接口，SQL由对应XML维护。
     */
    private final OpenLbRoutingMapper mapper;

    /**
     * 将本地暂存OpenLB文献按当前路由重新分流；不重新请求上游正文。
     * @param afterId 最后已提交的扫描ID；不是已处理条数
     * @return 按上述规则计算或读取的ApiResponse<?>结果
     */
    @PostMapping("/redistribute-existing")
    public ApiResponse<?> redistribute(@RequestParam(defaultValue = "0") long afterId) {
        var result = routing.redistribute(Math.max(0, afterId));
        integration.syncIdentifiers((java.util.List<String>) result.get("pmids"), java.util.List.of());
        return ApiResponse.ok(result);
    }

    /**
     * 校验参数及运行状态后启动后台处理，调用立即返回当前状态，后续通过状态接口查看结果。
     * @return 按上述规则计算或读取的ApiResponse<?>结果
     */
    @PostMapping("/start")
    public ApiResponse<?> start() {
        return ApiResponse.ok(service.start());
    }

    /**
     * 请求后台在安全边界停止，已提交的数据和断点保留，不清空原始记录。
     * @return 按上述规则计算或读取的ApiResponse<?>结果
     */
    @PostMapping("/stop")
    public ApiResponse<?> stop() {
        return ApiResponse.ok(service.stop());
    }

    /**
     * 返回当前进程标记和持久化任务状态，不启动新的采集或统计任务。
     * @return 按上述规则计算或读取的ApiResponse<?>结果
     */
    @GetMapping("/status")
    public ApiResponse<?> status() {
        return ApiResponse.ok(service.status());
    }

    /**
     * 汇总cncbBanks盘点结果，分别返回资源条目数与不同查询PMID数。
     * @return 按上述规则计算或读取的ApiResponse<?>结果
     */
    @GetMapping("/summary")
    public ApiResponse<?> summary() {
        return ApiResponse.ok(mapper.summary());
    }

    /**
     * 分页读取来源盘点条目，便于核对db、accession及link等原始信息。
     * @param page 分页页码或单页结果
     * @param size 本次读取的最大条数或目标容量
     * @return 按上述规则计算或读取的ApiResponse<?>结果
     */
    @GetMapping("/records")
    public ApiResponse<?> records(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "100") int size) {
        int n = Math.max(1, Math.min(size, 500));
        return ApiResponse.ok(mapper.inventoryRows((Math.max(1, page) - 1) * n, n));
    }
}

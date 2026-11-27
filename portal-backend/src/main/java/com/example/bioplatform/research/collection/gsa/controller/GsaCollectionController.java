package com.example.bioplatform.research.collection.gsa.controller;

import com.example.bioplatform.common.ApiResponse;
import com.example.bioplatform.research.collection.gsa.dto.GsaCollectionStartRequest;
import com.example.bioplatform.research.collection.gsa.service.GsaCollectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * GSA独立主采集入口；读取支持文章全列表并补元数据，关联发现沿用公共关联Controller。
 */
@RestController
@RequestMapping("/api/collection/gsa")
@RequiredArgsConstructor
public class GsaCollectionController {
    /**
     * service：控制器依赖的业务服务，不在控制器中实现持久化逻辑。
     */
    private final GsaCollectionService service;

    /**
     * 首次采集、停止/失败续做；上次SUCCESS时创建新的完整列表扫描。
     */
    @PostMapping("/jobs/start")
    public ApiResponse<?> start(@Valid @RequestBody(required = false) GsaCollectionStartRequest request) {
        return ApiResponse.ok(service.start(config(request), false));
    }

    /**
     * 明确放弃旧扫描游标并下载新快照；不删除文献、成功补全内容或OpenLB任务。
     */
    @PostMapping("/jobs/restart")
    public ApiResponse<?> restart(@Valid @RequestBody(required = false) GsaCollectionStartRequest request) {
        return ApiResponse.ok(service.start(config(request), true));
    }

    /**
     * 安全停止当前GSA作业。
     */
    @PostMapping("/jobs/stop")
    public ApiResponse<?> stop() {
        return ApiResponse.ok(service.stop());
    }

    /**
     * 查询GSA下载、导入、补全阶段及持久化断点。
     */
    @GetMapping("/status")
    public ApiResponse<?> status() {
        return ApiResponse.ok(service.status());
    }

    /**
     * 保留文献，只重置元数据补全标记。
     */
    @PostMapping("/enrichment/reset")
    public ApiResponse<?> reset() {
        return ApiResponse.ok(service.resetEnrichment());
    }

    /**
     * 返回当前采集器的配置及实际启用入口，便于核对来源与请求方式。
     * @param r 当前响应、记录或文献对象
     * @return 按上述规则计算或读取的GsaCollectionStartRequest结果
     */
    private GsaCollectionStartRequest config(GsaCollectionStartRequest r) {
        return r == null ? new GsaCollectionStartRequest(null, null, null) : r;
    }
}

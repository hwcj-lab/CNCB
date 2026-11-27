package com.example.bioplatform.research.preparation.controller;

import com.example.bioplatform.common.ApiResponse;
import com.example.bioplatform.research.preparation.model.PreparationRequest;
import com.example.bioplatform.research.preparation.service.PreparationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 数据整理管理入口。仅调度本地原始记录的整理，不负责采集网络数据或自动生成快照。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/research-preparation")
public class PreparationController {
    /**
     * service：控制器依赖的业务服务，不在控制器中实现持久化逻辑。
     */
    private final PreparationService service;

    /**
     * 返回当前进程标记和持久化任务状态，不启动新的采集或统计任务。
     * @return 按上述规则计算或读取的ApiResponse<Map<String, Object>>结果
     */
    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> status() {
        return ApiResponse.ok(service.status());
    }

    /**
     * 校验参数及运行状态后启动后台处理，调用立即返回当前状态，后续通过状态接口查看结果。
     * @param request 调用方传入并校验后的请求参数
     * @return 按上述规则计算或读取的ApiResponse<Map<String, Object>>结果
     */
    @PostMapping("/start")
    public ApiResponse<Map<String, Object>> start(@RequestBody(required = false) PreparationRequest request) {
        return ApiResponse.ok(service.start(request, false));
    }

    /**
     * 人工启动重新构建或强制重解析流程；具体范围由请求和当前任务状态决定。
     * @param request 调用方传入并校验后的请求参数
     * @return 按上述规则计算或读取的ApiResponse<Map<String, Object>>结果
     */
    @PostMapping("/rebuild")
    public ApiResponse<Map<String, Object>> rebuild(@RequestBody(required = false) PreparationRequest request) {
        return ApiResponse.ok(service.start(request, true));
    }

    /**
     * 重新提交已有失败或停止的任务，沿用持久化断点，而非删除全部结果重跑。
     * @return 按上述规则计算或读取的ApiResponse<Map<String, Object>>结果
     */
    @PostMapping("/retry")
    public ApiResponse<Map<String, Object>> retry() {
        return ApiResponse.ok(service.start(null, false));
    }

    /**
     * 请求后台在安全边界停止，已提交的数据和断点保留，不清空原始记录。
     * @return 按上述规则计算或读取的ApiResponse<Map<String, Object>>结果
     */
    @PostMapping("/stop")
    public ApiResponse<Map<String, Object>> stop() {
        return ApiResponse.ok(service.stop());
    }

    /**
     * 人工处理进程已退出但持久化状态未收尾的任务，不能与仍在运行的旧进程同时执行。
     * @return 按上述规则计算或读取的ApiResponse<Map<String, Object>>结果
     */
    @PostMapping("/recover")
    public ApiResponse<Map<String, Object>> recover() {
        return ApiResponse.ok(service.recover());
    }

    /**
     * 撤销未发布整理任务的版本变更并恢复此前版本，不删除原始采集表。
     * @return 按上述规则计算或读取的ApiResponse<Map<String, Object>>结果
     */
    @PostMapping("/discard")
    public ApiResponse<Map<String, Object>> discard() {
        return ApiResponse.ok(service.discard());
    }

    /**
     * 按成功来源基线执行增量整理；没有可靠基线的来源回退全量扫描。
     * @param request 调用方传入并校验后的请求参数
     * @return 按上述规则计算或读取的ApiResponse<Map<String, Object>>结果
     */
    @PostMapping("/incremental")
    public ApiResponse<Map<String, Object>> incremental(@RequestBody(required = false) PreparationRequest request) {
        return ApiResponse.ok(service.incremental(request));
    }
}

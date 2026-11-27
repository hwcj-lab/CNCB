package com.example.bioplatform.research.collection.common.controller;

import com.example.bioplatform.common.ApiResponse;
import com.example.bioplatform.research.collection.common.dto.PendingRelationBatchRequest;
import com.example.bioplatform.research.collection.common.dto.PublicationRelationCollectionRequest;
import com.example.bioplatform.research.collection.common.service.PublicationRelationCollectionService;
import com.example.bioplatform.research.collection.common.vo.PublicationRelationJobStatus;
import com.example.bioplatform.research.collection.common.vo.RelatedPublicationPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 人工控制的单文献刷新和单层关联采集接口；不会自动运行全量任务。
 */
@RestController
@RequestMapping("/api/collection/publication-relations")
@RequiredArgsConstructor
@Tag(name = "文献关联采集", description = "按已注册数据库及唯一值人工启动、停止和恢复；资源链接由可扩展解析器处理")
public class PublicationRelationCollectionController {
    /**
     * service：控制器依赖的业务服务，不在控制器中实现持久化逻辑。
     */
    private final PublicationRelationCollectionService service;

    /**
     * 校验参数及运行状态后启动后台处理，调用立即返回当前状态，后续通过状态接口查看结果。
     * @param request 调用方传入并校验后的请求参数
     * @return 按上述规则计算或读取的ApiResponse<PublicationRelationJobStatus>结果
     */
    @PostMapping("/jobs/start")
    @Operation(summary = "启动单文献刷新/单层关联发现", description = "identifiers示例：GSA={pmid:41028458}；GenBase={paperId:203}或{pmid:42514208}")
    public ApiResponse<PublicationRelationJobStatus> start(@Valid @RequestBody PublicationRelationCollectionRequest request) {
        return ApiResponse.ok(service.start(request));
    }

    /**
     * 从来源表领取一批待关联根记录，持久化队列后启动单层发现，不要求逐篇手填PMID。
     * @param request 调用方传入并校验后的请求参数
     * @return 按上述规则计算或读取的ApiResponse<PublicationRelationJobStatus>结果
     */
    @PostMapping("/jobs/start-pending-batch")
    @Operation(summary = "自动批量领取待关联文献",
            description = "无需填写文献唯一值；从指定已注册数据库来源表领取PENDING记录并创建持久化队列，支持停止和断点续做")
    public ApiResponse<PublicationRelationJobStatus> startPendingBatch(
            @Valid @RequestBody PendingRelationBatchRequest request) {
        return ApiResponse.ok(service.startPendingBatch(request));
    }

    /**
     * 恢复原作业的未完成及可重试队列，保持作业ID和已提交进度。
     * @param jobId 所属持久化任务或作业ID
     * @return 按上述规则计算或读取的ApiResponse<PublicationRelationJobStatus>结果
     */
    @PostMapping("/jobs/{jobId}/resume")
    @Operation(summary = "继续暂停、停止或失败的关联采集作业")
    public ApiResponse<PublicationRelationJobStatus> resume(@PathVariable long jobId) {
        return ApiResponse.ok(service.resume(jobId));
    }

    /**
     * 请求后台在安全边界停止，已提交的数据和断点保留，不清空原始记录。
     * @return 按上述规则计算或读取的ApiResponse<PublicationRelationJobStatus>结果
     */
    @PostMapping("/jobs/stop")
    @Operation(summary = "安全停止当前关联采集作业")
    public ApiResponse<PublicationRelationJobStatus> stop() {
        return ApiResponse.ok(service.stop());
    }

    /**
     * 返回当前进程标记和持久化任务状态，不启动新的采集或统计任务。
     * @param jobId 所属持久化任务或作业ID
     * @return 按上述规则计算或读取的ApiResponse<PublicationRelationJobStatus>结果
     */
    @GetMapping("/jobs/status")
    @Operation(summary = "查询关联采集作业状态")
    public ApiResponse<PublicationRelationJobStatus> status(@RequestParam(required = false) Long jobId) {
        return ApiResponse.ok(service.status(jobId));
    }

    /**
     * 查询参数形式支持含斜杠的DOI，避免URL路径编码限制；与路径形式共用查询服务。
     */
    @GetMapping("/publications")
    public ApiResponse<RelatedPublicationPage> relatedByQuery(
            @RequestParam String databaseCode, @RequestParam String identifierType, @RequestParam String identifier,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.relatedPublications(databaseCode, identifierType, identifier, page, size));
    }

    /**
     * 按数据库来源和文献唯一值查询关联文献，不把关联边当成全局归属判断。
     * @param databaseCode 数据库来源编码或目录业务编码
     * @param identifierType 唯一值类别，例如PMID、DOI或SOURCE_KEY
     * @param identifier 文献或资源的唯一值
     * @param page 分页页码或单页结果
     * @param size 本次读取的最大条数或目标容量
     * @return 按上述规则计算或读取的ApiResponse<RelatedPublicationPage>结果
     */
    @GetMapping("/publications/{databaseCode}/{identifierType}/{identifier}")
    @Operation(summary = "分页查看一篇文献的双向关联文献")
    public ApiResponse<RelatedPublicationPage> related(
            @PathVariable String databaseCode, @PathVariable String identifierType, @PathVariable String identifier,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.ok(service.relatedPublications(databaseCode, identifierType, identifier, page, size));
    }
}

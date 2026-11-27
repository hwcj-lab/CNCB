package com.example.bioplatform.research.statistics.prepared.controller;

import com.example.bioplatform.common.ApiResponse;
import com.example.bioplatform.research.statistics.prepared.service.PreparedSnapshotService;
import com.example.bioplatform.research.statistics.vo.ResearchSnapshotStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 人工恢复中断快照的管理入口；恢复前先确认旧构建进程已退出。
 */
@RestController
@RequiredArgsConstructor
public class PreparedSnapshotRecoveryController {
    /**
     * service：控制器依赖的业务服务，不在控制器中实现持久化逻辑。
     */
    private final PreparedSnapshotService service;

    /**
     * 人工处理进程已退出但持久化状态未收尾的任务，不能与仍在运行的旧进程同时执行。
     * @return 按上述规则计算或读取的ApiResponse<ResearchSnapshotStatus>结果
     */
    @PostMapping("/api/research-snapshots/recover")
    public ApiResponse<ResearchSnapshotStatus> recover() {
        return ApiResponse.ok(service.recover());
    }
}

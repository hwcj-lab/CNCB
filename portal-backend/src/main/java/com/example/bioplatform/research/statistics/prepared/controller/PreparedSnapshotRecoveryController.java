package com.example.bioplatform.research.statistics.prepared.controller;
import com.example.bioplatform.research.statistics.prepared.service.PreparedSnapshotService;
import com.example.bioplatform.research.statistics.vo.ResearchSnapshotStatus;
import com.example.bioplatform.common.ApiResponse;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
@RestController @RequiredArgsConstructor
public class PreparedSnapshotRecoveryController {
    private final PreparedSnapshotService service;
    @PostMapping("/api/research-snapshots/recover") public ApiResponse<ResearchSnapshotStatus> recover() {
        return ApiResponse.ok(service.recover());
    }
}

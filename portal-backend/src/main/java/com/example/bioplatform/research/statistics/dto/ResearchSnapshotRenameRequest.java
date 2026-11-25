package com.example.bioplatform.research.statistics.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 修改快照展示名称。 */
public record ResearchSnapshotRenameRequest(
        @NotBlank(message = "快照名称不能为空")
        @Size(max = 100, message = "快照名称不能超过100个字符") String name) { }


package com.example.bioplatform.research.statistics.vo;


/**
 * 统计构建运行状态和最近可用版本。
 */
public record ResearchSnapshotStatus(boolean running, String message, ResearchSnapshotDetail current,
                                     ResearchSnapshotDetail latestSuccess) {
}

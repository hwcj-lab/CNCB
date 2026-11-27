package com.example.bioplatform.research.statistics.vo;


import java.util.List;

/**
 * 快照文献分页结果。
 */
public record ResearchSnapshotPublicationPage(long total, int page, int size,
                                              List<ResearchSnapshotPublication> items) {
}

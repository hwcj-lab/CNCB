package com.example.bioplatform.research.statistics.vo;


import java.util.List;
import java.util.Map;

/**
 * 前端仪表盘的一次性总汇对象：版本信息加按类型分组的图表数据。
 */
public record ResearchSnapshotResult(ResearchSnapshotDetail snapshot,
                                     Map<String, List<ResearchSnapshotDimension>> dimensions) {
}

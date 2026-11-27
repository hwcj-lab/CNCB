package com.example.bioplatform.research.statistics.vo;


import java.util.List;
import java.util.Map;

/**
 * 前端仪表盘的一次性总汇对象：版本信息加按类型分组的图表数据。
 * @param snapshot 本结果所属的快照版本、统计选项和汇总信息
 * @param dimensions 按维度类型分组的图表统计点，含年度、主题、作者、机构和资助方等
 */
public record ResearchSnapshotResult(ResearchSnapshotDetail snapshot,
                                     Map<String, List<ResearchSnapshotDimension>> dimensions) {
}

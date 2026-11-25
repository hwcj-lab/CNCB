<script setup>
import AnalysisChartCard from "./AnalysisChartCard.vue";
import ChartSurface from "./ChartSurface.vue";
defineProps({ result: Object, resourceMode: Boolean });
const emit = defineEmits(["select"]);
</script>
<template>
  <AnalysisChartCard
    title="基金资助分析"
    subtitle="优先使用 agency；缺失时按基金编号匹配本地字典全称，同一统计对象内的同名基金只计一次"
    :result="result"
    ><div v-if="result?.items?.length" class="fund-table">
      <el-table :data="result.items" style="height: 330px" @row-click="(row) => emit('select', row)"
        ><el-table-column type="index" label="#" width="40" /><el-table-column
          prop="name"
          label="基金名称"
          min-width="150" /><el-table-column
          prop="value"
          :label="resourceMode ? '关联记录' : '关联文献'"
          width="90"
          align="right"
      /></el-table>
    </div>
    <ChartSurface
      v-else
      :option="{}"
      empty
      label="基金资助统计"
      empty-title="暂无有效基金名称"
      empty-message="未取得有效 agency，或基金编号未匹配到本地字典全称。"
  /></AnalysisChartCard>
</template>
<!-- 基金资助名称及其关联文献数量展示组件；总统计与分库共用。 -->

<script setup>
import { computed } from "vue";
import ChartSurface from "./ChartSurface.vue";
import AnalysisChartCard from "./AnalysisChartCard.vue";
import { distributionOption } from "../utils/chartOptions.mjs";
const props = defineProps({
  result: Object,
  resourceMode: Boolean,
  title: { type: String, default: "研究主题分布" },
  subtitle: { type: String, default: "论文关键词与 PubMed MeSH 主题词 · 多标签计数" },
  emptyMessage: { type: String, default: "尚未取得可用的论文关键词或 MeSH 主题词。" },
});
const emit = defineEmits(["select"]);
const option = computed(() => distributionOption(props.result?.items || [], props.resourceMode));
</script>
<template>
  <AnalysisChartCard
    :title="title"
    :subtitle="subtitle"
    :result="result"
    ><ChartSurface
      :option="option"
      :empty="!result?.items?.length"
      label="研究主题分布环形图"
      :empty-message="emptyMessage"
      @select="emit('select', $event)"
  /></AnalysisChartCard>
</template>
<!-- 研究主题或资源关联类别的分布图。 -->

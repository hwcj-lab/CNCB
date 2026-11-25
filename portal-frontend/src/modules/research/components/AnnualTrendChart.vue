<script setup>
import { computed } from "vue";
import ChartSurface from "./ChartSurface.vue";
import AnalysisChartCard from "./AnalysisChartCard.vue";
import { annualOption } from "../utils/chartOptions.mjs";
const props = defineProps({ result: Object, resourceMode: Boolean });
const emit = defineEmits(["select"]);
const option = computed(() => annualOption(props.result?.items || [], props.resourceMode));
</script>
<template>
  <AnalysisChartCard
    :title="resourceMode ? '年度产出趋势' : '年度文献产出趋势'"
    :subtitle="resourceMode ? '按文献发表年份或资源发布日期统计；未知年份不计入趋势' : '按发表年份统计，按文献唯一值去重'"
    :result="result"
    ><ChartSurface
      :option="option"
      :empty="!result?.items?.length"
      label="年度文献产出趋势折线图"
      empty-message="当前筛选范围没有已收录的带年份文献。未知年份未纳入趋势。"
      @select="emit('select', $event)"
  /></AnalysisChartCard>
</template>
<!-- 年度文献产出折线图封装。 -->

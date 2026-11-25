<script setup>
import { computed } from "vue";
import ChartSurface from "./ChartSurface.vue";
import AnalysisChartCard from "./AnalysisChartCard.vue";
import { rankingOption } from "../utils/chartOptions.mjs";
const props = defineProps({
  title: String,
  subtitle: String,
  result: Object,
  resourceMode: Boolean,
  emptyMessage: String,
});
const emit = defineEmits(["select"]);
const option = computed(() => rankingOption(props.result?.items || [], props.resourceMode));
</script>
<template>
  <AnalysisChartCard :title="title" :subtitle="subtitle" :result="result"
    ><ChartSurface
      :option="option"
      :empty="!result?.items?.length"
      :label="title + '柱状图'"
      :empty-title="
        result?.status === 'PENDING' ? '数据待接入' : '暂无可统计数据'
      "
      :empty-message="emptyMessage"
      @select="emit('select', $event)"
  /></AnalysisChartCard>
</template>
<!-- 机构或作者排名条形图。 -->

<script setup>
import { ref, watch, onMounted, onBeforeUnmount, nextTick } from "vue";
import * as echarts from "echarts/core";
import { LineChart, BarChart, PieChart } from "echarts/charts";
import {
  GridComponent,
  TooltipComponent,
  LegendComponent,
  DataZoomComponent,
} from "echarts/components";
import { CanvasRenderer } from "echarts/renderers";
import AppIcon from "../../../shared/components/AppIcon.vue";
echarts.use([
  LineChart,
  BarChart,
  PieChart,
  GridComponent,
  TooltipComponent,
  LegendComponent,
  DataZoomComponent,
  CanvasRenderer,
]);
const props = defineProps({
  option: { type: Object, required: true },
  empty: Boolean,
  label: { type: String, required: true },
  emptyTitle: { type: String, default: "暂无可统计数据" },
  emptyMessage: {
    type: String,
    default: "请调整筛选条件，或在数据接入后查看。",
  },
});
const emit = defineEmits(["select"]);
const host = ref(null);
let chart,
  observer,
  disposed = false;
async function render() {
  await nextTick();
  if (disposed) return;
  if (props.empty) {
    chart?.dispose();
    chart = null;
    observer?.disconnect();
    return;
  }
  if (!host.value) return;
  if (!chart) {
    chart = echarts.init(host.value);
    chart.on("click", (params) => emit("select", params.data));
    observer = new ResizeObserver(() => chart?.resize());
    observer.observe(host.value);
  }
  chart.setOption(props.option, { notMerge: true });
  chart.resize();
}
onMounted(render);
watch(() => [props.option, props.empty], render, { deep: true });
onBeforeUnmount(() => {
  disposed = true;
  observer?.disconnect();
  chart?.dispose();
});
</script>
<template>
  <div v-if="empty" class="chart-empty">
    <AppIcon name="chart" :size="30" /><strong>{{ emptyTitle }}</strong>
    <p>{{ emptyMessage }}</p>
  </div>
  <div v-else ref="host" class="chart-surface" role="img" :aria-label="label"></div>
</template>
<!-- ECharts 生命周期适配层，负责初始化、更新、缩放和销毁实例。 -->
<style scoped>
.chart-surface { cursor: pointer; width: 100%; height: 360px; min-width: 0; }
</style>

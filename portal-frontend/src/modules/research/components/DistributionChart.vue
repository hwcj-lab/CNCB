<script setup>
import { computed } from "vue";
import ChartSurface from "./ChartSurface.vue";
import AnalysisChartCard from "./AnalysisChartCard.vue";
import { distributionOption } from "../utils/chartOptions.mjs";
const props = defineProps({
  result: Object,
  resourceMode: Boolean,
  showCategoryList: Boolean,
  title: { type: String, default: "研究主题分布" },
  subtitle: { type: String, default: "论文关键词与 PubMed MeSH 主题词 · 多标签计数" },
  emptyMessage: { type: String, default: "尚未取得可用的论文关键词或 MeSH 主题词。" },
});
const emit = defineEmits(["select"]);
const option = computed(() => {
  const value = distributionOption(props.result?.items || [], props.resourceMode);
  if (props.showCategoryList) value.legend.show = false;
  return value;
});
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
  />
    <ul v-if="showCategoryList && result?.items?.length" class="category-list" aria-label="全部研究主题分类及数量">
      <li v-for="item in result.items" :key="item.dimensionKey || item.name">
        <button type="button" @click="emit('select', item)">
          <span>{{ item.name }}</span>
          <strong>{{ Number(item.value) || 0 }}</strong>
        </button>
      </li>
    </ul>
  </AnalysisChartCard>
</template>
<style scoped>
.category-list {
  list-style: none;
  padding: 0;
  margin: 0 0 8px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 8px;
}
.category-list button {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
  padding: 9px 12px;
  border: 1px solid var(--el-border-color, #394641);
  border-radius: 6px;
  background: transparent;
  color: inherit;
  font: inherit;
  text-align: left;
  cursor: pointer;
}
.category-list button:hover,
.category-list button:focus-visible { border-color: var(--el-color-primary, #3f9f85); }
.category-list strong { font-variant-numeric: tabular-nums; }
</style>
<!-- 研究主题或资源关联类别的分布图。 -->

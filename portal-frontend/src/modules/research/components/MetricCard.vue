<script setup>
import AppIcon from "../../../shared/components/AppIcon.vue";
defineProps({ metric: Object });
const icons = {
  publications: "book",
  databases: "database",
  institutions: "layers",
  authors: "species",
  funds: "protein",
};
</script>
<template>
  <article class="metric-card">
    <div class="metric-top">
      <span>{{ metric.label }}</span
      ><AppIcon :name="icons[metric.code]" :size="18" />
    </div>
    <div class="metric-value">
      {{ metric.value === null ? "—" : metric.value?.toLocaleString() }}
    </div>
    <el-tooltip :content="metric.note"
      ><div class="metric-note">
        <span class="status-dot"></span
        >{{
          metric.status === "DEMO"
            ? "合成演示数据"
            : metric.status === "PENDING"
              ? "数据待接入"
              : metric.status === "EMPTY"
                ? "筛选范围内无记录"
                : "有限调研样例"
        }}
      </div></el-tooltip
    >
  </article>
</template>
<!-- 仪表盘指标卡，保留数据状态与口径说明。 -->

<script setup>
defineProps({ title: String, subtitle: String, result: Object });
</script>
<template>
  <section class="chart-card">
    <div class="chart-heading">
      <div>
        <h3>{{ title }}</h3>
        <p>{{ subtitle }}</p>
      </div>
    </div>
    <slot />
  </section>
</template>
<!-- 带标题、状态和空数据提示的分析图表卡片。 -->

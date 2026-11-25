<script setup>
import PlatformLayout from "./shared/layout/PlatformLayout.vue";
</script>
<template>
  <PlatformLayout><router-view /></PlatformLayout>
</template>
<!-- 应用根组件仅承载路由出口，具体框架由页面布局组件负责。 -->

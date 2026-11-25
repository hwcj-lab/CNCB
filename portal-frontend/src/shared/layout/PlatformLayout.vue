<script setup>
import { ref, watch } from "vue";
import { useRoute } from "vue-router";
import AppIcon from "../components/AppIcon.vue";
const route = useRoute(),
  mobileOpen = ref(false);
watch(
  () => route.path,
  () => {
    mobileOpen.value = false;
    document.title = (route.meta.title || "BioPortal") + " · BioPortal";
  },
);
</script>
<template>
  <div class="app-shell">
    <div
      v-if="mobileOpen"
      class="sidebar-shade"
      @click="mobileOpen = false"
    ></div>
    <aside class="sidebar" :class="{ open: mobileOpen }" aria-label="主导航">
      <router-link class="brand" to="/tool-recommend"
        ><span class="brand-mark"><AppIcon name="dna" :size="26" /></span
        ><span>BioPortal<small>生物数据资源平台</small></span></router-link
      >
      <div class="nav-section-label">工作空间 <span>WORKSPACE</span></div>
      <nav>
        <router-link to="/tool-recommend" class="nav-link"
          ><AppIcon name="grid" /><span>资源与工具推荐</span
          ><span class="nav-dot"></span
        ></router-link>
        <router-link to="/research-analysis" class="nav-link"
          ><AppIcon name="chart" /><span>科研产出分析</span
          ><span class="nav-dot"></span
        ></router-link>
      </nav>
      <div class="sidebar-note">
        <span class="tiny-label">EVIDENCE FIRST</span>
        <h3>让每一项推荐有据可查</h3>
        <p>保留数据来源与核验状态，连接资源、工具与科研发现。</p>
        <a
          href="https://ngdc.cncb.ac.cn/"
          target="_blank"
          rel="noopener noreferrer"
          >访问中心资源门户 <AppIcon name="external" :size="14"
        /></a>
      </div>
      <div class="sidebar-footer">
        <span class="status-dot"></span>
        <div>调研基线 · 2026.11.06<small>目录收录 ≠ 计算运行验证</small></div>
        <span class="version">v1.0</span>
      </div>
    </aside>
    <div class="main-shell">
      <header class="topbar">
        <button
          class="mobile-menu icon-button"
          aria-label="打开导航"
          @click="mobileOpen = !mobileOpen"
        >
          <AppIcon name="menu" />
        </button>
        <div class="breadcrumb">
          <span>工作空间</span><span class="breadcrumb-separator">/</span
          ><span>{{ route.meta.section }}</span>
        </div>
        <div class="topbar-right">
          <span class="topbar-label">BIOINFORMATICS RESOURCE HUB</span
          ><span class="avatar">BIO</span>
        </div>
      </header>
      <main id="main-content" class="main-content"><slot /></main>
      <footer class="page-footer">
        <span>BioPortal · 生物数据资源推荐与科研产出分析</span
        ><span>以调研为依据，连接数据与发现</span>
      </footer>
    </div>
  </div>
</template>
<!-- 两个业务模块共用的顶部栏、侧栏、面包屑和主内容布局。 -->

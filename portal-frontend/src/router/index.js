import { createRouter, createWebHistory } from "vue-router";
// 两个业务页面共享平台布局；未知地址进入明确的 404 页面。
export default createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: "/", redirect: "/tool-recommend" },
    {
      path: "/tool-recommend",
      component: () => import("../modules/catalog/views/ToolRecommendView.vue"),
      meta: { title: "数据资源与工具推荐", section: "资源探索" },
    },
    {
      path: "/research-analysis",
      component: () =>
        import("../modules/research/views/ResearchAnalysisView.vue"),
      meta: { title: "科研产出分析", section: "科研洞察" },
    },
    {
      path: "/research-analysis/databases",
      component: () => import("../modules/research/views/ResearchDatabaseOverviewView.vue"),
      meta: { title: "数据库统计总览", section: "科研洞察" },
    },
    {
      path: "/research-analysis/database/:databaseCode",
      component: () => import("../modules/research/views/DatabaseResearchAnalysisView.vue"),
      meta: { title: "分数据库科研产出分析", section: "科研洞察" },
    },
    {
      path: "/:pathMatch(.*)*",
      component: () => import("../shared/views/NotFoundView.vue"),
      meta: { title: "页面不存在", section: "导航" },
    },
  ],
  // 页面切换后回到顶部，避免保留上一页面的长列表位置。
  scrollBehavior: () => ({ top: 0 }),
});

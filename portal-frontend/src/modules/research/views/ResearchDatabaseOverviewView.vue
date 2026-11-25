<script setup>
import { onMounted, ref, computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { getResearchSnapshotResult, getLatestSnapshotResult, getResearchSnapshotDatabases, getResearchTopicDatabases } from "../api/research";

const route = useRoute(), router = useRouter();
const snapshotId = ref(Number(route.query.snapshotId) || null);
const topic = computed(() => String(route.query.topic || "").toUpperCase());
const resourceMode = ref(false);
const databases = ref([]), matching = ref([]), loading = ref(true), error = ref("");
async function load() {
  loading.value = true; error.value = "";
  try {
    if (!snapshotId.value) snapshotId.value = (await getLatestSnapshotResult(1)).snapshot.id;
    resourceMode.value = Boolean((await getResearchSnapshotResult(snapshotId.value,1)).snapshot.includeResources);
    databases.value = await getResearchSnapshotDatabases(snapshotId.value);
    matching.value = topic.value ? await getResearchTopicDatabases(snapshotId.value, topic.value) : databases.value;
  } catch (e) { error.value = e.message; }
  finally { loading.value = false; }
}
function openDatabase(code) {
  router.push({ path: `/research-analysis/database/${encodeURIComponent(code)}`, query: { snapshotId: snapshotId.value } });
}
onMounted(load);
</script>

<template>
  <div class="database-overview">
    <div class="overview-heading">
      <div><div class="eyebrow">DATABASE RESEARCH INSIGHTS</div><h1>数据库统计总览</h1>
        <p>当前快照的数据库来源。选择一个数据库查看它自己的年度、主题、作者、机构和基金统计。</p></div>
      <el-button @click="router.push('/research-analysis')">返回总统计</el-button>
    </div>
    <el-skeleton v-if="loading" :rows="5" animated />
    <el-alert v-else-if="error" :title="error" type="error" show-icon />
    <template v-else>
      <h2 v-if="topic">{{ topic }} · 关联数据库</h2>
      <p v-if="topic && !matching.length">当前主题没有已映射的文献来源。下方仍列出本快照的全部数据库。</p>
      <div class="database-grid">
        <button v-for="db in (matching.length ? matching : databases)" :key="db.code" class="database-card" type="button" @click="openDatabase(db.code)">
          <strong>{{ db.name || db.code }}</strong><span>{{ db.code }}</span><span>{{ db.publicationCount }} {{ resourceMode ? "条记录" : "篇文献" }}</span>
        </button>
      </div>
      <template v-if="topic && matching.length && matching.length < databases.length">
        <h2>其他数据库</h2>
        <div class="database-grid"><button v-for="db in databases.filter(v => !matching.some(m => m.code === v.code))" :key="db.code" class="database-card" type="button" @click="openDatabase(db.code)"><strong>{{ db.name || db.code }}</strong><span>{{ db.code }}</span><span>{{ db.publicationCount }} {{ resourceMode ? "条记录" : "篇文献" }}</span></button></div>
      </template>
    </template>
  </div>
</template>

<style scoped>
.database-overview{padding:34px 40px;color:#dbe9ee}.overview-heading{display:flex;justify-content:space-between;gap:24px;align-items:center;margin-bottom:34px}.overview-heading h1{font-size:30px;margin:12px 0}.overview-heading p{color:#98afb6}.eyebrow{color:#72c9b3;letter-spacing:2px;font-size:12px}.database-overview h2{font-size:19px;margin:24px 0 16px}.database-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(230px,1fr));gap:16px}.database-card{display:flex;flex-direction:column;text-align:left;gap:12px;padding:22px;background:#141a1b;border:1px solid #394446;border-radius:10px;color:#dbe9ee;cursor:pointer}.database-card:hover{border-color:#51bba2;background:#1b2927}.database-card strong{font-size:20px}.database-card span{color:#9bb3b8}
</style>

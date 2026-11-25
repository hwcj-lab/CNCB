<script setup>
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import {
  getDatabaseSnapshotResult, getDimensionPublications, getLatestSnapshotResult,
  getRelatedPublications, getResearchSnapshotDatabases,
} from "../api/research";
import AnnualTrendChart from "../components/AnnualTrendChart.vue";
import DistributionChart from "../components/DistributionChart.vue";
import RankingChart from "../components/RankingChart.vue";
import FundAnalysis from "../components/FundAnalysis.vue";
import { safeExternalUrl, publicationExternalUrl, publicationExternalLabel, publicationResourceLinks } from "../utils/links";

const route = useRoute(), router = useRouter();
const databaseCode = computed(() => String(route.params.databaseCode || "").toUpperCase());
const snapshotId = ref(Number(route.query.snapshotId) || null);
const databaseName = ref(databaseCode.value), result = ref(null), databases = ref([]);
const loading = ref(true), error = ref(""), displayLimit = ref(10);
const yearStart = ref(null), yearEnd = ref(null), years = ref([]);
const drillOpen = ref(false), drillLoading = ref(false);
const drill = ref({ title: "", type: "", dimensionId: null, page: 1, data: null });
const relatedOpen = ref(false), relatedLoading = ref(false), related = ref({ title: "", items: [] });
const limits = [10, 20, 30, 40, 50];
const resourceMode = computed(() => Boolean(result.value?.snapshot?.includeResources));

const items = (type) => (result.value?.dimensions?.[type] || []).map((row) => ({
  dimensionId: row.id, dimensionKey: row.dimensionKey, name: row.displayName,
  year: row.statYear || Number(row.displayName), value: row.publicationCount,
}));
const charts = computed(() => ({
  annual: { status: "CONFIRMED", items: items("YEAR").filter((v) =>
    (!yearStart.value || v.year >= yearStart.value) && (!yearEnd.value || v.year <= yearEnd.value)) },
  topics: { status: "CONFIRMED", items: items("TOPIC").slice(0, displayLimit.value) },
  institutions: { status: "CONFIRMED", items: items("INSTITUTION").slice(0, displayLimit.value) },
  authors: { status: "CONFIRMED", items: items("AUTHOR").slice(0, displayLimit.value) },
  funds: { status: "CONFIRMED", items: items("FUND").slice(0, displayLimit.value) },
}));

async function load() {
  loading.value = true; error.value = "";
  try {
    if (!snapshotId.value) snapshotId.value = (await getLatestSnapshotResult(1)).snapshot.id;
    databases.value = await getResearchSnapshotDatabases(snapshotId.value);
    const option = databases.value.find((row) => row.code === databaseCode.value);
    if (!option) throw new Error(`快照中不存在数据库 ${databaseCode.value}`);
    databaseName.value = option.name;
    result.value = await getDatabaseSnapshotResult(snapshotId.value, databaseCode.value, 50);
    years.value = [...new Set(items("YEAR").map((v) => Number(v.year)).filter(Number.isFinite))].sort((a, b) => a - b);
    yearStart.value = years.value[0] || null; yearEnd.value = years.value.at(-1) || null;
  } catch (e) { error.value = e.message; }
  finally { loading.value = false; }
}
async function changeDatabase(code) {
  await router.push({ path: `/research-analysis/database/${encodeURIComponent(code)}`, query: { snapshotId: snapshotId.value } });
  await load();
}
async function openPapers(item, type, page = 1) {
  drillOpen.value = true; drillLoading.value = true;
  drill.value = { ...drill.value, title: item.name, type, dimensionId: item.dimensionId, page };
  try {
    const data = await getDimensionPublications(snapshotId.value, item.dimensionId, page, 10);
    drill.value = { ...drill.value, data, page };
  } catch (e) { ElMessage.error(e.message); }
  finally { drillLoading.value = false; }
}
async function openRelated(row) {
  const type = row.recordKind === "RESOURCE" ? "SOURCE_KEY" : row.pmid ? "PMID" : row.doi ? "DOI" : ["GSA", "BIOPROJECT"].includes(databaseCode.value) && row.publicationKey ? "SOURCE_KEY" : null;
  const value = row.pmid || row.doi || row.publicationKey;
  if (!type) return ElMessage.info("该文献没有可用于查询关联的业务唯一值");
  relatedOpen.value = true; relatedLoading.value = true; related.value = { title: row.title || value, items: [] };
  try {
    const data = await getRelatedPublications(databaseCode.value, type, value, 1, 100);
    related.value = { ...related.value, items: data.items || [] };
  } catch (e) { ElMessage.warning(e.message); }
  finally { relatedLoading.value = false; }
}
onMounted(load);
</script>

<template>
  <div class="database-analysis-page">
    <section class="db-heading">
      <div><div class="eyebrow"><span></span> DATABASE RESEARCH INSIGHTS</div><h1>{{ databaseName }} 科研产出分析</h1><p>快照 v{{ result?.snapshot?.versionNo }} · {{ resourceMode ? "文献＋无文献资源；资源按原库编号去重" : "仅文献；按文献唯一值去重" }}。</p></div>
      <div class="db-actions"><el-button @click="router.push('/research-analysis')">返回总统计</el-button>
        <el-select :model-value="databaseCode" style="width:220px" @change="changeDatabase">
          <el-option v-for="db in databases" :key="db.code" :label="`${db.name}（${db.publicationCount}）`" :value="db.code" />
        </el-select>
        <el-select v-model="displayLimit" style="width:110px"><el-option v-for="n in limits" :key="n" :label="`Top ${n}`" :value="n" /></el-select>
      </div>
    </section>
    <div v-if="error" class="state-card"><h3>数据库统计不可用</h3><p>{{ error }}</p><el-button @click="load">重试</el-button></div>
    <el-skeleton v-else-if="loading" :rows="12" animated />
    <template v-else>
      <div class="chart-grid">
        <section class="annual-wrap"><div class="year-filter"><span>年份</span><el-select v-model="yearStart" style="width:95px"><el-option v-for="y in years.filter(v => !yearEnd || v <= yearEnd)" :key="y" :value="y" /></el-select><span>至</span><el-select v-model="yearEnd" style="width:95px"><el-option v-for="y in years.filter(v => !yearStart || v >= yearStart)" :key="y" :value="y" /></el-select></div><AnnualTrendChart :resource-mode="resourceMode" :result="charts.annual" @select="openPapers($event,'YEAR')" /></section>
        <DistributionChart :resource-mode="resourceMode" :result="charts.topics" @select="openPapers($event,'TOPIC')" />
      </div>
      <div class="chart-grid"><RankingChart :resource-mode="resourceMode" title="核心机构分析" :subtitle="resourceMode ? '文献署名或资源所属机构' : '论文署名机构'" :result="charts.institutions" @select="openPapers($event,'INSTITUTION')" /><RankingChart :resource-mode="resourceMode" title="核心作者分析" subtitle="作者 + 署名机构" :result="charts.authors" @select="openPapers($event,'AUTHOR')" /></div>
      <div class="chart-grid single"><FundAnalysis :resource-mode="resourceMode" :result="charts.funds" @select="openPapers($event,'FUND')" /></div>
    </template>
    <el-drawer v-model="drillOpen" :title="`${drill.title} · 文献`" size="min(900px,96vw)">
      <el-table v-loading="drillLoading" :data="drill.data?.items || []" empty-text="暂无文献">
        <el-table-column label="文献" min-width="390"><template #default="{row}"><div class="paper-title">{{ row.title || row.pmid }}</div><a v-if="publicationExternalUrl(row)" :href="publicationExternalUrl(row)" target="_blank">{{ publicationExternalLabel(row) }} ↗</a><span v-for="link in publicationResourceLinks(row).filter(link => link.url !== publicationExternalUrl(row))" :key="link.url" style="margin-left: 12px"><a :href="link.url" target="_blank" rel="noopener noreferrer">{{ link.label }} ↗</a></span></template></el-table-column>
        <el-table-column label="类型" width="95"><template #default="{row}">{{ row.recordKind === 'RESOURCE' ? '无文献资源' : '文献' }}</template></el-table-column><el-table-column prop="pubYear" label="年份" width="80"/><el-table-column label="关联" width="110"><template #default="{row}"><el-button link type="primary" @click="openRelated(row)">关联文献</el-button></template></el-table-column>
      </el-table>
      <el-pagination background layout="total,prev,pager,next" :total="drill.data?.total || 0" :page-size="10" :current-page="drill.page" @current-change="p => openPapers({name:drill.title,dimensionId:drill.dimensionId},drill.type,p)" />
    </el-drawer>
    <el-drawer v-model="relatedOpen" :title="`${related.title} · 关联文献`" size="min(900px,96vw)"><el-table v-loading="relatedLoading" :data="related.items" empty-text="尚未采集到关联文献"><el-table-column label="标题" min-width="420"><template #default="{row}"><a v-if="publicationExternalUrl(row)" :href="publicationExternalUrl(row)" target="_blank" rel="noopener noreferrer">{{ row.title || row.identifierValue || '查看文献' }} ↗</a><span v-else>{{ row.title || row.identifierValue || '标题未提供' }}</span><div v-if="publicationExternalUrl(row)" style="font-size: 12px; margin-top: 4px">{{ publicationExternalLabel(row) }}<span v-for="link in publicationResourceLinks(row).filter(link => link.url !== publicationExternalUrl(row))" :key="link.url" style="margin-left: 12px"><a :href="link.url" target="_blank" rel="noopener noreferrer">{{ link.label }} ↗</a></span></div></template></el-table-column><el-table-column label="类型" width="95"><template #default="{row}">{{ row.recordKind === 'RESOURCE' ? '无文献资源' : '文献' }}</template></el-table-column><el-table-column prop="pubYear" label="年份" width="80"/><el-table-column prop="pmid" label="PMID" width="130"/></el-table></el-drawer>
  </div>
</template>

<style scoped>
.database-analysis-page{font-size:15px}.db-heading{display:flex;justify-content:space-between;gap:24px;align-items:flex-end;margin-bottom:24px}.db-heading h1{font-size:30px;margin:8px 0}.db-heading p{color:#8da09f}.db-actions{display:flex;gap:10px;align-items:center}.chart-grid{display:grid;grid-template-columns:minmax(0,1.3fr) minmax(0,.9fr);gap:20px;margin-bottom:20px}.chart-grid.single{grid-template-columns:1fr}.annual-wrap{position:relative;min-width:0}.year-filter{position:absolute;z-index:3;right:20px;top:17px;display:flex;gap:8px;align-items:center}.state-card{padding:38px;border:1px solid #34423e;border-radius:12px}.paper-title{font-weight:600;margin-bottom:6px}.el-pagination{margin-top:18px}@media(max-width:1000px){.db-heading{align-items:flex-start;flex-direction:column}.chart-grid{grid-template-columns:1fr}.db-actions{flex-wrap:wrap}}
</style>

<script setup>
import PreparationPanel from "../components/PreparationPanel.vue";
const preparationReady = ref(false);
import BioProjectCollectionPanel from "../components/BioProjectCollectionPanel.vue";
import GsaCollectionPanel from "../components/GsaCollectionPanel.vue";
import OpenLbBankInventory from "../components/OpenLbBankInventory.vue";
import { computed, ref, onMounted, onBeforeUnmount } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  getLatestSnapshotResult,
  getDimensionPublications,
  buildResearchSnapshot,
  stopResearchSnapshot,
  getResearchSnapshotStatus,
  renameResearchSnapshot,
  getResearchSnapshotVersions,
  getResearchSnapshotResult,
  deleteResearchSnapshot,
  getRelatedPublications,
  getResearchSnapshotSources,
} from "../api/research";
import { safeExternalUrl, publicationExternalUrl, publicationExternalLabel, publicationResourceLinks } from "../utils/links";
import AppIcon from "../../../shared/components/AppIcon.vue";
import MetricCard from "../components/MetricCard.vue";
import AnnualTrendChart from "../components/AnnualTrendChart.vue";
import DistributionChart from "../components/DistributionChart.vue";
import RankingChart from "../components/RankingChart.vue";
import FundAnalysis from "../components/FundAnalysis.vue";
const summary = ref(null),
  charts = ref({}),
  loading = ref(true),
  error = ref(""),
  sourceDrawer = ref(false),
  sources = ref([]),
  sourceLoading = ref(false),
  sourceError = ref(""),
  building = ref(false),
  snapshotRunning = ref(false),
  snapshotStatusMessage = ref(""),
  drilldownOpen = ref(false),
  drilldownLoading = ref(false),
  drilldown = ref({ title: "", type: "", dimensionId: null, page: 1, data: null }),
  relatedOpen = ref(false),
  relatedLoading = ref(false),
  related = ref({ title: "", items: [], errors: [] }),
  displayLimit = ref(10),
  allDimensions = ref({}),
  snapshotVersions = ref([]),
  selectedSnapshotId = ref(null),
  yearStart = ref(null),
  yearEnd = ref(null),
  availableYears = ref([]),
  createDialogOpen = ref(false),
  buildForm = ref({ name: "", includeRelated: false, includeResources: false });
let sequence = 0;
let statusTimer = null;
const router = useRouter();
const limits = [10, 20, 30, 40, 50];
const resourceMode = computed(() => Boolean(summary.value?.snapshot?.includeResources));
function rebuildCharts() {
  const toItems = (type) =>
    (allDimensions.value?.[type] || []).map((row) => ({
      id: row.id,
      dimensionId: row.id,
      dimensionKey: row.dimensionKey,
      name: row.displayName,
      year: row.statYear || Number(row.displayName),
      value: row.publicationCount,
    }));
  const chart = (type, ranked = true) => ({
    status: "CONFIRMED",
    items: ranked ? toItems(type).slice(0, displayLimit.value) : toItems(type),
  });
  const snapshot = summary.value?.snapshot;
  const annualItems = toItems("YEAR").filter((item) =>
    (!yearStart.value || item.year >= yearStart.value) && (!yearEnd.value || item.year <= yearEnd.value));
  charts.value = {
    annual: { status: "CONFIRMED", items: annualItems, unknownYearCount: snapshot ? snapshot.publicationCount - snapshot.yearCoverageCount : 0 },
    topics: chart("TOPIC", false),
    institutions: chart("INSTITUTION"),
    authors: chart("AUTHOR"),
    funds: chart("FUND"),
  };
}
async function load(snapshotId = null) {
  const version = ++sequence;
  loading.value = true;
  error.value = "";
  summary.value = null;
  charts.value = {};
  try {
    const result = snapshotId
      ? await getResearchSnapshotResult(snapshotId, 50)
      : await getLatestSnapshotResult(50);
    if (version !== sequence) return;
    const snapshot = result.snapshot;
    selectedSnapshotId.value = snapshot.id;
    allDimensions.value = result.dimensions || {};
    const years = (allDimensions.value.YEAR || [])
      .map((row) => Number(row.statYear || row.displayName))
      .filter(Number.isFinite)
      .sort((a, b) => a - b);
    availableYears.value = [...new Set(years)];
    yearStart.value = availableYears.value[0] || null;
    yearEnd.value = availableYears.value.at(-1) || null;
    summary.value = {
      snapshot,
      coverage: {
        totalPublications: snapshot.publicationCount,
        yearCoverage: snapshot.yearCoverageCount,
        institutionCoverage: snapshot.institutionCoverageCount,
        authorCoverage: snapshot.authorCoverageCount,
        fundCoverage: snapshot.fundCoverageCount,
      },
      message: `统计快照 v${snapshot.versionNo}，来源 ${snapshot.sourceCodes}；${snapshot.includeResources ? "文献＋无文献资源" : "仅文献"}。`,
    };
    rebuildCharts();
  } catch (e) {
    if (version === sequence) error.value = e.message;
  } finally {
    if (version === sequence) loading.value = false;
  }
}
async function loadVersions() {
  try {
    const rows = await getResearchSnapshotVersions();
    snapshotVersions.value = (rows || []).filter((row) => row.status === "SUCCESS");
  } catch (e) {
    ElMessage.error(`无法加载快照版本：${e.message}`);
  }
}
async function refreshSnapshotStatus() {
  try {
    const state = await getResearchSnapshotStatus();
    snapshotRunning.value = Boolean(state?.running);
    snapshotStatusMessage.value = state?.message || "";
  } catch {
    // 状态接口异常不覆盖主体内容；生成接口仍由后端执行并发保护。
  }

}
onMounted(() => {
  load();
  loadVersions();
  refreshSnapshotStatus();
  statusTimer = window.setInterval(refreshSnapshotStatus, 3000);
});
onBeforeUnmount(() => {
  if (statusTimer) window.clearInterval(statusTimer);
});
function formatSnapshotTime(value) {
  if (!value) return "已完成";
  return value.replace("T", " ").replace(/\.\d+$/, "");
}
async function openDimensionPapers(item, type, page = 1) {
  if (!item?.dimensionId) {
    ElMessage.info("该历史快照尚未建立文献穿透关系，请生成新快照");
    return;
  }
  drilldownOpen.value = true;
  drilldownLoading.value = true;
  drilldown.value = { ...drilldown.value, title: item.name, type, dimensionId: item.dimensionId, page };
  try {
    const data = await getDimensionPublications(
      summary.value.snapshot.id,
      item.dimensionId,
      page,
      10,
    );
    drilldown.value = { ...drilldown.value, page, data };
  } catch (e) {
    ElMessage.error(e.message);
  } finally {
    drilldownLoading.value = false;
  }
}
function openDatabaseAnalysis(item) {
  if (!item?.dimensionKey || !summary.value?.snapshot?.id) return;
  router.push({
    path: "/research-analysis/databases",
    query: { snapshotId: summary.value.snapshot.id, topic: item.dimensionKey },
  });
}
async function openRelated(row) {
  let sourceKeys = {};
  try {
    sourceKeys = typeof row.sourceRecordKeys === "string"
      ? JSON.parse(row.sourceRecordKeys || "{}")
      : (row.sourceRecordKeys || {});
  } catch {
    sourceKeys = {};
  }
  let targets = Object.entries(sourceKeys)
    .filter(([code, value]) => value && (!row.scopeCode || row.scopeCode === "TOTAL" || code === row.scopeCode))
    .map(([code, value]) => {
      const separator = String(value).indexOf(":");
      if (separator < 1) return null;
      const prefix = String(value).slice(0, separator).toUpperCase();
      if (prefix === "RESOURCE" || ["GSA", "BIOPROJECT"].includes(code) && ["TITLE", "LOCAL_ID"].includes(prefix))
        return { code, identifierType: "SOURCE_KEY", identifier: String(value) };
      const identifierType = prefix === "PAPER" ? "PAPER_ID" : prefix;
      if (!["PMID", "DOI", "PAPER_ID"].includes(identifierType)) return null;
      return { code, identifierType, identifier: String(value).slice(separator + 1) };
    })
    .filter(Boolean);
  if (!targets.length) {
    const fallbackType = row.pmid ? "PMID" : row.doi ? "DOI" : null;
    const fallbackValue = row.pmid || row.doi;
    const codes = row.scopeCode && row.scopeCode !== "TOTAL"
      ? [row.scopeCode]
      : [...new Set((row.sourceCodes || "").split(",").map((v) => v.trim()).filter(Boolean))];
    if (!fallbackType || !fallbackValue) return ElMessage.info("该文献没有可用于关联查询的来源唯一值");
    targets = codes.map((code) => ({ code, identifierType: fallbackType, identifier: fallbackValue }));
  }
  relatedOpen.value = true;
  relatedLoading.value = true;
  related.value = { title: row.title || row.publicationKey || row.pmid || row.doi, items: [], errors: [] };
  const results = await Promise.allSettled(targets.map((target) =>
    getRelatedPublications(target.code, target.identifierType, target.identifier, 1, 100)
      .then((data) => ({ ...target, data }))));
  const seen = new Set();
  const items = [];
  const errors = [];
  results.forEach((result, index) => {
    if (result.status === "rejected") return errors.push(`${targets[index].code}：${result.reason?.message || "暂不支持关联查询"}`);
    (result.value.data?.items || []).forEach((item) => {
      const key = item.pmid || item.doi || `${item.identifierType}:${item.identifierValue}`;
      if (!seen.has(key)) { seen.add(key); items.push({ ...item, databaseCode: result.value.code }); }
    });
  });
  related.value = { ...related.value, items, errors };
  relatedLoading.value = false;
}
async function showSources() {
  sourceDrawer.value = true;
  sourceLoading.value = true;
  sourceError.value = "";
  try {
    const options = await getResearchSnapshotSources();
    const includedCodes = new Set(summary.value?.snapshot?.sourceCodes?.split(",").filter(Boolean) || []);
    sources.value = (options || []).filter((source) => includedCodes.has(source.code)).map((source, index) => ({
      id: source.code || index,
      code: source.code,
      name: source.name || source.code,
      verificationStatus: "RESPONSE_VERIFIED",
      method: "数据库快照",
      address: "已采集到本地来源表",
      statusText: "此来源已纳入该快照统计。",
      verifiedOn: summary.value?.snapshot?.completedTime?.slice(0, 10),
      documentUrl: null,
    }));
  } catch (error) {
    sourceError.value = error.message || "无法读取来源列表";
  } finally {
    sourceLoading.value = false;
  }
}
async function openCreateSnapshot() {
  await refreshSnapshotStatus();
  if (!preparationReady.value) {
    ElMessage.warning("请先完成统计数据整理");
    return;
  }
  if (snapshotRunning.value) {
    ElMessage.warning(snapshotStatusMessage.value || "已有统计快照正在生成，请稍后再试");
    return;
  }
  createDialogOpen.value = true;
  buildForm.value = {
    name: `科研统计快照 ${new Date().toLocaleString("zh-CN", { hour12: false })}`,
    includeRelated: false, includeResources: false,
  };
}
async function changeSnapshot(snapshotId) {
  await load(snapshotId);
}
async function deleteCurrentSnapshot() {
  const snapshot = summary.value?.snapshot;
  if (!snapshot) return;
  try {
    await ElMessageBox.confirm(
      `确定删除“${snapshot.snapshotName || `统计快照 v${snapshot.versionNo}`}”吗？该版本的维度和穿透关系会删除，全局文献继续保留。`,
      "删除统计快照",
      { type: "warning", confirmButtonText: "删除", cancelButtonText: "取消" },
    );
    await deleteResearchSnapshot(snapshot.id);
    selectedSnapshotId.value = null;
    await loadVersions();
    await load();
    ElMessage.success("统计快照已删除");
  } catch (e) {
    if (e !== "cancel" && e !== "close") ElMessage.error(e.message || "删除失败");
  }
}
async function createSnapshot() {
  await refreshSnapshotStatus();
  if (!preparationReady.value) {
    ElMessage.warning("请先完成统计数据整理");
    return;
  }
  building.value = true;
  error.value = "";
  createDialogOpen.value = false;
  try {
    await buildResearchSnapshot(buildForm.value);
    snapshotRunning.value = true;
    for (let i = 0; i < 180; i++) {
      await new Promise((resolve) => setTimeout(resolve, 1000));
      const state = await getResearchSnapshotStatus();
      snapshotRunning.value = Boolean(state?.running);
      snapshotStatusMessage.value = state?.message || "";
      if (!state.running) {
        if (state.current?.status === "FAILED") throw new Error(state.current.errorMessage || "统计快照构建失败");
        await load();
        await loadVersions();
        ElMessage.success("统计快照构建完成");
        return;
      }
    }
    throw new Error("统计仍在后台运行，请稍后点击重试");
  } catch (e) { error.value = e.message; }
  finally { building.value = false; await refreshSnapshotStatus(); }
}
async function renameCurrentSnapshot() {
  if (!summary.value?.snapshot) return;
  try {
    const { value } = await ElMessageBox.prompt("请输入新的快照名称", "修改快照名称", {
      inputValue: summary.value.snapshot.snapshotName || `统计快照 v${summary.value.snapshot.versionNo}`,
      inputPattern: /^.{1,100}$/,
      inputErrorMessage: "名称必须为1至100个字符",
    });
    const updated = await renameResearchSnapshot(summary.value.snapshot.id, value.trim());
    summary.value.snapshot = updated;
    await loadVersions();
    ElMessage.success("快照名称已修改");
  } catch (e) {
    if (e !== "cancel" && e !== "close") ElMessage.error(e.message || "修改失败");
  }
}
async function stopSnapshot() {
  try {
    await stopResearchSnapshot();
    await refreshSnapshotStatus();
    ElMessage.info("已请求停止，将在当前批次完成后结束");
  } catch (e) { ElMessage.error(e.message); }
}
const sourceStatus = (s) =>
  s === "RESPONSE_VERIFIED"
    ? "已纳入当前统计快照"
    : "来源待验证";
</script>
<template>
  <OpenLbBankInventory /><GsaCollectionPanel /><BioProjectCollectionPanel />
  <div class="analysis-page">
    <PreparationPanel @status="preparationReady = Boolean($event.canBuild)" />
    <section class="page-heading analysis-heading">
      <div>
        <div class="eyebrow"><span></span> RESEARCH INSIGHTS</div>
        <h1>科研产出分析</h1>
      </div>
      <div class="heading-actions">
        <el-button @click="showSources"
          ><AppIcon name="database" :size="14" /><span style="margin-left: 6px"
            >数据来源</span
          ></el-button
        >
      </div>
    </section>
    <section class="analysis-filter" aria-label="统计快照操作">
      <div class="filter-item">
        <label>统计版本</label>
        <el-select v-model="selectedSnapshotId" style="width: 310px" placeholder="请选择统计版本" @change="changeSnapshot">
          <el-option
            v-for="snapshot in snapshotVersions"
            :key="snapshot.id"
            :value="snapshot.id"
            :label="`${snapshot.snapshotName || `统计快照 v${snapshot.versionNo}`} · v${snapshot.versionNo}`"
          />
        </el-select>
        <span v-if="summary" class="snapshot-time">完成于 {{ formatSnapshotTime(summary.snapshot.completedTime) }}</span>
      </div>
      <div class="filter-item" v-if="summary">
        <label>排名展示数量</label>
        <el-select v-model="displayLimit" style="width: 110px" @change="rebuildCharts">
          <el-option v-for="limit in limits" :key="limit" :label="`Top ${limit}`" :value="limit" />
        </el-select>
      </div>
      <div class="filter-submit">
        <el-button @click="load(selectedSnapshotId)" :loading="loading">刷新</el-button>
        <el-button v-if="summary" @click="renameCurrentSnapshot">修改名称</el-button>
        <el-button v-if="summary" type="danger" plain @click="deleteCurrentSnapshot">删除版本</el-button>
        <el-button
          type="primary"
          @click="openCreateSnapshot"
          :loading="building"
          :disabled="snapshotRunning || building || !preparationReady"
          :title="snapshotRunning ? (snapshotStatusMessage || '已有快照正在生成') : ''"
        >生成新快照</el-button>
        <el-button v-if="snapshotRunning || building" type="danger" plain @click="stopSnapshot">停止</el-button>
      </div>
    </section>
    <div v-if="error" class="error-panel" role="alert">
      <h3>统计服务暂不可用</h3>
      <p>{{ error }}</p>
      <el-button @click="load">重试</el-button>
      <el-button type="primary" @click="openCreateSnapshot" :loading="building" :disabled="snapshotRunning || building || !preparationReady">生成新快照</el-button>
    </div>
    <section v-else-if="loading" class="loading-panel analysis-loading">
      <el-skeleton :rows="12" animated />
    </section>
    <template v-else-if="summary">
      <div class="chart-grid">
        <section class="annual-chart-wrap">
          <div class="year-range-filter">
            <span>年份区间</span>
            <el-select v-model="yearStart" style="width: 100px" @change="rebuildCharts">
              <el-option v-for="year in availableYears.filter((y) => !yearEnd || y <= yearEnd)" :key="year" :label="year" :value="year" />
            </el-select>
            <span>至</span>
            <el-select v-model="yearEnd" style="width: 100px" @change="rebuildCharts">
              <el-option v-for="year in availableYears.filter((y) => !yearStart || y >= yearStart)" :key="year" :label="year" :value="year" />
            </el-select>
          </div>
          <AnnualTrendChart :resource-mode="resourceMode" :result="charts.annual" @select="openDimensionPapers($event, 'YEAR')" />
        </section><DistributionChart :resource-mode="resourceMode"
          show-category-list
          :result="charts.topics"
          title="研究主题分布"
          :subtitle="resourceMode ? '按所属数据库的主题标签统计，同一记录可对应多个主题' : '按文献所属数据库的主题标签统计'"
          empty-message="当前快照没有研究主题统计。确认数据库主题映射后，可生成新快照查看。"
          @select="openDatabaseAnalysis"
        />
      </div>
      <div class="chart-grid equal">
        <RankingChart :resource-mode="resourceMode"
          title="核心机构分析"
          :subtitle="resourceMode ? '文献署名机构或原资源明确的所属机构' : '论文署名机构 · 去重文献全计数'"
          :result="charts.institutions"
          @select="openDimensionPapers($event, 'INSTITUTION')"
          empty-message="当前统计对象尚无可用的机构信息；不将资源机构分配给论文作者。"
        /><RankingChart :resource-mode="resourceMode"
          title="核心作者分析"
          subtitle="作者 + 署名机构 · 同名不同机构分别统计"
          :result="charts.authors"
          @select="openDimensionPapers($event, 'AUTHOR')"
          empty-message="作者缩写尚未完成身份消歧，不将原始作者字符串当作唯一科研人员。"
        />
      </div>
      <div class="chart-grid single">
        <FundAnalysis :resource-mode="resourceMode" :result="charts.funds" @select="openDimensionPapers($event, 'FUND')" />
      </div>
      <p class="analysis-bottom">
        统计范围：{{ summary.message }} 未知年份
        {{
          charts.annual?.unknownYearCount || 0
        }}
        {{ resourceMode ? "条" : "篇" }}。研究主题分布按关联资源的已确认主题标签统计；点击标签可查看命中文献。
      </p>
    </template>
    <el-drawer
      v-model="sourceDrawer"
      title="科研统计数据来源与接入状态"
      size="min(720px, 94vw)"
      ><el-alert
        title="以下来源来自当前成功统计快照；统计只使用已经采集到本地的数据，不会在打开页面时请求上游。"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 15px"
      /><el-skeleton v-if="sourceLoading" :rows="9" animated /><el-alert
        v-else-if="sourceError"
        :title="sourceError"
        type="error"
        :closable="false"
      />
      <template v-else>
      <section v-if="summary?.coverage" class="coverage-panel">
        <h3>数据覆盖情况</h3>
        <p>分母为当前快照去重文献总数；字段缺失的文献不会进入对应排名。</p>
        <div class="coverage-list">
          <div><span>发表年份可用</span><strong>{{ summary.coverage.yearCoverage }} / {{ summary.coverage.totalPublications }}</strong></div>
          <div><span>论文署名机构可用</span><strong>{{ summary.coverage.institutionCoverage }} / {{ summary.coverage.totalPublications }}</strong></div>
          <div><span>作者信息可用</span><strong>{{ summary.coverage.authorCoverage }} / {{ summary.coverage.totalPublications }}</strong></div>
          <div><span>论文基金关联可用</span><strong>{{ summary.coverage.fundCoverage }} / {{ summary.coverage.totalPublications }}</strong></div>
        </div>
        <p>研究主题按文献关联数据库的已确认类别标签统计；作者按姓名和署名机构区分，同名不同机构分别计数。</p>
      </section>
      <div v-for="s in sources" :key="s.id" class="source-row">
        <h3>{{ s.code }} · {{ s.name }}</h3>
        <span class="source-status">{{
          sourceStatus(s.verificationStatus)
        }}</span>
        <p class="source-address">{{ s.method }} {{ s.address }}</p>
        <p>{{ s.statusText }}</p>
        <p>调研核验日期：{{ s.verifiedOn }}</p>
        <a
          v-if="safeExternalUrl(s.documentUrl)"
          :href="safeExternalUrl(s.documentUrl)"
          target="_blank"
          rel="noopener noreferrer"
          >查看官方文档 ↗</a
        >
      </div>
      </template></el-drawer
    >
    <el-drawer
      v-model="drilldownOpen"
      :title="`${drilldown.title || '统计项'} · 关联文献`"
      size="min(860px, 96vw)"
    >
      <el-alert
        :title="`维度：${drilldown.type}。下列文献共同构成当前统计值。`"
        type="info"
        :closable="false"
        style="margin-bottom: 16px"
      />
      <el-table v-loading="drilldownLoading" :data="drilldown.data?.items || []" empty-text="该历史快照没有穿透关系，请生成新快照">
        <el-table-column label="文献标题" min-width="360">
          <template #default="{ row }">
            <div class="paper-title">{{ row.title || `PMID ${row.pmid || '未提供'}` }}</div>
            <div class="paper-secondary">{{ row.journalTitle || row.doi || row.sourceCodes }}</div>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="95"><template #default="{row}">{{ row.recordKind === 'RESOURCE' ? '无文献资源' : '文献' }}</template></el-table-column>
        <el-table-column prop="pubYear" label="年份" width="80" />
        <el-table-column label="来源" width="110">
          <template #default="{ row }">
            <div>{{ row.sourceCodes || row.scopeCode || '未知来源' }}</div>
            <a v-if="publicationExternalUrl(row)" :href="publicationExternalUrl(row)" target="_blank" rel="noopener noreferrer">{{ publicationExternalLabel(row) }} ↗</a><span v-for="link in publicationResourceLinks(row).filter(link => link.url !== publicationExternalUrl(row))" :key="link.url" style="margin-left: 12px"><a :href="link.url" target="_blank" rel="noopener noreferrer">{{ link.label }} ↗</a></span>
          </template>
        </el-table-column>
        <el-table-column label="关联" width="105">
          <template #default="{ row }"><el-button link type="primary" @click="openRelated(row)">关联文献</el-button></template>
        </el-table-column>
      </el-table>
      <el-pagination
        style="margin-top: 18px"
        background
        layout="total, prev, pager, next"
        :total="drilldown.data?.total || 0"
        :page-size="10"
        :current-page="drilldown.page"
        @current-change="(page) => openDimensionPapers({ name: drilldown.title, dimensionId: drilldown.dimensionId }, drilldown.type, page)"
      />
    </el-drawer>
    <el-drawer v-model="relatedOpen" :title="`${related.title || '文献'} · 关联文献`" size="min(900px, 96vw)">
      <el-alert v-if="related.errors.length" :title="related.errors.join('；')" type="warning" :closable="false" style="margin-bottom: 14px" />
      <el-table v-loading="relatedLoading" :data="related.items" empty-text="尚未采集到关联文献">
        <el-table-column label="文献标题" min-width="380">
          <template #default="{ row }">
            <a v-if="publicationExternalUrl(row)" :href="publicationExternalUrl(row)" target="_blank" rel="noopener noreferrer">{{ row.title || row.identifierValue || '查看文献' }} ↗</a>
            <span v-else>{{ row.title || row.identifierValue || '标题未提供' }}</span><div v-if="publicationExternalUrl(row)" style="font-size: 12px; margin-top: 4px">{{ publicationExternalLabel(row) }}<span v-for="link in publicationResourceLinks(row).filter(link => link.url !== publicationExternalUrl(row))" :key="link.url" style="margin-left: 12px"><a :href="link.url" target="_blank" rel="noopener noreferrer">{{ link.label }} ↗</a></span></div>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="95"><template #default="{row}">{{ row.recordKind === 'RESOURCE' ? '无文献资源' : '文献' }}</template></el-table-column>
        <el-table-column prop="pubYear" label="年份" width="80" />
        <el-table-column prop="databaseCode" label="关系来源" width="110" />
        <el-table-column prop="pmid" label="PMID" width="120" />
      </el-table>
    </el-drawer>
    <el-dialog v-model="createDialogOpen" title="生成科研统计快照" width="min(560px, 94vw)">
      <el-form label-position="top">
        <el-form-item label="快照名称">
          <el-input v-model="buildForm.name" maxlength="100" show-word-limit placeholder="请输入快照名称" />
        </el-form-item>
        <el-form-item label="统计数据库">
          <el-alert title="使用已完成整理的数据，自动统计全部已接入数据库。" type="info" :closable="false" />
        </el-form-item>
        <el-form-item label="统计对象">
          <el-radio-group v-model="buildForm.includeResources"><el-radio :value="false">仅文献</el-radio><el-radio :value="true">文献＋无文献资源</el-radio></el-radio-group>
          <p class="form-help">资源有原生文献时只统计文献；无原生文献的资源按数据库和原库编号计一条。</p>
        </el-form-item>
        <el-form-item label="关联文献口径">
          <el-checkbox v-model="buildForm.includeRelated">
            纳入仅由关联链路发现、并非数据库原始文献的记录
          </el-checkbox>
          <p class="form-help">默认关闭；原始文献即使同时被关联发现，仍会正常统计。</p>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogOpen = false">取消</el-button>
        <el-button type="primary" :loading="building" :disabled="snapshotRunning || building || !preparationReady" @click="createSnapshot">开始生成</el-button>
      </template>
    </el-dialog>
  </div>
</template>
<style scoped>
.analysis-page {
  font-size: 15px;
}
.analysis-page :deep(.filter-item label),
.analysis-page :deep(.metric-note),
.analysis-page :deep(.chart-heading p),
.analysis-page :deep(.chart-footer),
.analysis-page :deep(.analysis-bottom),
.analysis-page :deep(.paper-secondary) {
  font-size: 13px;
}
.analysis-page :deep(.filter-item > span),
.analysis-page :deep(.metric-top),
.analysis-page :deep(.paper-title),
.analysis-page :deep(.fund-table .el-table) {
  font-size: 14px;
}
.analysis-page :deep(.chart-heading h3) {
  font-size: 18px;
}
.snapshot-time {
  display: block;
  margin-top: 7px;
  color: #829798;
}
.annual-chart-wrap {
  position: relative;
  min-width: 0;
}
.annual-chart-wrap :deep(.chart-card) {
  height: 100%;
}
.year-range-filter {
  position: absolute;
  z-index: 2;
  right: 22px;
  top: 18px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #829798;
}
.coverage-list {
  margin-top: 24px;
}
.coverage-list > div {
  display: flex;
  justify-content: space-between;
  padding: 13px 0;
  border-bottom: 1px solid #edf2ef;
  font-size: 14px;
  color: #8a9d9d;
}
.coverage-list strong {
  font-weight: 500;
  color: #6b9684;
}
.source-checkboxes {
  display: grid;
  gap: 10px;
}
.source-checkboxes :deep(.el-checkbox) {
  margin-right: 0;
  font-size: 15px;
}
</style>
<!-- 科研产出仪表盘：读取最近成功快照，并支持从聚合图表穿透到具体文献。 -->

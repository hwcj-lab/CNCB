<script setup>
import { computed, onMounted, ref } from "vue";
import { getResourceRecommendations, getSystemStatus } from "../api/catalog";
import { safeExternalUrl, relationLabels } from "../utils/links";
import AppIcon from "../../../shared/components/AppIcon.vue";

const groups = ref([]);
const system = ref(null);
const keyword = ref("");
const selectedIds = ref({});
const loading = ref(true);
const error = ref("");

const icons = { DNA: "dna", PROTEIN: "protein", LITERATURE: "book", SPECIES: "species" };
const normalizedKeyword = computed(() => keyword.value.trim().toLowerCase());
const visibleGroups = computed(() => groups.value
  .map((group) => ({
    ...group,
    databases: (group.databases || []).filter((database) => {
      if (!normalizedKeyword.value) return true;
      return [database.name, database.dbId, database.dbZhTitle, database.dbEnTitle]
        .filter(Boolean).some((value) => value.toLowerCase().includes(normalizedKeyword.value));
    }),
  }))
  .filter((group) => group.databases.length > 0));
const databaseCount = computed(() => groups.value.reduce((sum, group) => sum + (group.databases?.length || 0), 0));
const toolCount = computed(() => new Set(groups.value.flatMap((group) => group.databases || []).flatMap((database) => database.tools || []).map((tool) => tool.id)).size);

function selectedFor(group) {
  const selectedId = selectedIds.value[group.dataType.code];
  return group.databases.find((database) => database.id === selectedId) || group.databases[0] || null;
}
function selectDatabase(group, database) {
  selectedIds.value = { ...selectedIds.value, [group.dataType.code]: database.id };
}
function scrollToType(code) {
  document.getElementById(`type-${code}`)?.scrollIntoView({ behavior: "smooth", block: "start" });
}
async function load() {
  loading.value = true;
  error.value = "";
  try {
    const [catalog, status] = await Promise.all([getResourceRecommendations(), getSystemStatus()]);
    groups.value = catalog;
    system.value = status;
    selectedIds.value = Object.fromEntries(catalog.map((group) => [group.dataType.code, group.databases?.[0]?.id]).filter((entry) => entry[1]));
  } catch (e) {
    error.value = e.message;
  } finally {
    loading.value = false;
  }
}
onMounted(load);
</script>

<template>
  <div class="resource-page">
    <section class="page-heading">
      <div>
        <div class="eyebrow"><span></span> RESOURCE EXPLORER</div>
        <h1>数据资源与工具推荐</h1>
      </div>
    </section>

    <div class="section-heading">
      <div class="section-heading-main">
        <div><h2>数据类型导航</h2><p>点击后定位到对应模块，页面默认展示全部类型。</p></div>
        <div class="hero-stats" aria-label="资源目录统计">
          <div><strong>{{ system?.databaseCount ?? databaseCount }}</strong><span>数据库目录</span></div>
          <div><strong>{{ system?.toolEntryCount ?? toolCount }}</strong><span>在线工具</span></div>
          <div><strong>{{ groups.length || "—" }}</strong><span>数据类型</span></div>
        </div>
      </div>
      <el-input v-model="keyword" clearable placeholder="搜索数据库名称…" class="resource-search">
        <template #prefix><AppIcon name="search" :size="17" /></template>
      </el-input>
    </div>

    <div class="data-type-grid" aria-label="数据类型导航">
      <button v-for="group in visibleGroups" :key="group.dataType.code" :class="['type-card', group.dataType.code.toLowerCase()]" @click="scrollToType(group.dataType.code)">
        <span class="type-icon"><AppIcon :name="icons[group.dataType.code] || 'database'" :size="25" /></span>
        <span><strong>{{ group.dataType.name }}</strong><small>{{ group.databases?.length || 0 }} 个关联数据库</small></span>
        <AppIcon class="type-arrow" name="arrow" :size="18" />
      </button>
    </div>

    <el-alert class="compact-alert" type="info" :closable="false" show-icon title="数据类型标签表示数据库的主要实际存储内容；少量附属数据和外部关联不作为标签依据。" />

    <div v-if="error" class="error-panel" role="alert">
      <h3>暂时无法加载资源</h3><p>{{ error }}</p><el-button type="primary" @click="load">重新加载</el-button>
    </div>
    <div v-else-if="loading" class="loading-panel"><el-skeleton :rows="12" animated /></div>

    <template v-else>
      <el-empty v-if="!visibleGroups.length" description="没有找到已分类的数据库资源" />
      <section v-for="(group, index) in visibleGroups" :id="`type-${group.dataType.code}`" :key="group.dataType.code" class="type-resource-section">
        <div class="explorer-toolbar">
          <div class="toolbar-label">
            <span class="section-number">{{ String(index + 1).padStart(2, '0') }}</span>
            <h2>{{ group.dataType.name }}</h2><span class="count-badge">{{ group.databases.length }}</span>
          </div>
          <span class="muted small">{{ group.dataType.description }}</span>
        </div>

        <el-empty v-if="!group.databases.length" description="当前没有已关联的数据库；请维护 database_data_type 关系。" />
        <div v-else class="resource-workspace">
          <div class="database-list" :aria-label="`${group.dataType.name}数据库列表`">
            <div class="list-caption">数据库资源 <span>{{ group.databases.length }} ITEMS</span></div>
            <button v-for="database in group.databases" :key="database.id" :class="['database-option', { selected: selectedFor(group)?.id === database.id }]" @click="selectDatabase(group, database)">
              <span class="database-option-icon"><AppIcon name="database" :size="21" /></span>
              <span class="database-option-copy">
                <strong>{{ database.name }}</strong><small>{{ database.dbZhTitle || database.dbEnTitle }}</small>
                <span class="database-meta">主要数据关联<span>{{ database.tools?.length || 0 }} 个工具</span></span>
              </span>
              <AppIcon name="arrow" :size="15" />
            </button>
          </div>

          <div v-if="selectedFor(group)" class="tool-panel">
            <div class="database-panel-heading">
              <div>
                <div class="tiny-label">SELECTED DATABASE</div>
                <h2>{{ selectedFor(group).name }} <span>{{ selectedFor(group).dbZhTitle }}</span></h2>
                <p>{{ selectedFor(group).dbZhDescription || selectedFor(group).dbEnDescription || '暂无数据库说明' }}</p>
              </div>
            </div>
            <div class="database-inline-details">
              <div><small>数据库标识</small><strong>{{ selectedFor(group).dbId || '未提供' }}</strong></div>
              <div><small>数据类型</small><strong>{{ group.dataType.name }}</strong></div>
              <a v-if="safeExternalUrl(selectedFor(group).dbUrl)" :href="safeExternalUrl(selectedFor(group).dbUrl)" target="_blank" rel="noopener noreferrer">访问数据库官网 <AppIcon name="external" :size="15" /></a>
            </div>
            <div class="tool-section-heading"><h3>对应在线工具 <span>{{ selectedFor(group).tools?.length || 0 }}</span></h3><span class="muted small">原生工具或数据兼容推荐</span></div>
            <div class="tool-list-scroll">
              <div v-if="selectedFor(group).tools?.length" class="tool-grid">
                <article v-for="tool in selectedFor(group).tools" :key="tool.id" class="tool-card">
                  <div class="tool-card-top"><span class="tool-symbol"><AppIcon name="protein" :size="23" /></span><span class="platform-badge">{{ tool.platform || 'CNCB' }}</span></div>
                  <h3>{{ tool.btZhTitle || tool.name }}</h3>
                  <div class="tool-tags"><span>在线工具</span><span :class="{ candidate: tool.relationType === 'CANDIDATE' }">{{ relationLabels[tool.relationType] || tool.relationType || '关联工具' }}</span></div>
                  <p class="tool-description">{{ tool.btZhDescription || tool.btEnDescription || '暂无工具说明' }}</p>
                  <div class="tool-actions">
                    <a v-if="safeExternalUrl(tool.btUrl)" :href="safeExternalUrl(tool.btUrl)" target="_blank" rel="noopener noreferrer" class="tool-open">访问工具入口 <AppIcon name="external" :size="15" /></a>
                    <el-button v-else disabled class="tool-disabled">入口未提供</el-button>
                  </div>
                  <span class="relation-note">{{ tool.relationBasis || '关联依据待补充' }}</span>
                </article>
              </div>
              <div v-else class="tool-empty"><AppIcon name="book" :size="34" /><h3>暂未维护关联工具</h3><p>请在 database_tool 表中维护数据库与工具关系。</p></div>
            </div>
          </div>
        </div>
      </section>
    </template>

    <div class="bottom-note"><AppIcon name="info" :size="16" /><span>当前共展示 {{ databaseCount }} 条类型关联，涉及 {{ toolCount }} 个去重工具。外链仅使用目录中已有地址。</span></div>

  </div>
</template>
<!-- 数据资源推荐页：按非空数据类型分组，数据库与工具均支持独立滚动。 -->

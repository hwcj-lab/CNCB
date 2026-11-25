<script setup>
import { ref, onBeforeUnmount } from "vue";
import { ElMessage } from "element-plus";
import request from "../../../shared/api/request";
const open = ref(false), busy = ref(false), state = ref({}), error = ref("");
const projectCodes = ref("");
const form = ref({ batchSize: 100, minDelayMs: 800, maxAttempts: 3 });
let timer;
async function refresh() {
  clearTimeout(timer);
  try {
    state.value = await request.get("/collection/bioproject/status"); error.value = "";
    if (open.value && state.value.running) timer = setTimeout(refresh, 2500);
  } catch (e) { error.value = e.message; }
}
async function show() { open.value = true; await refresh(); }
async function action(path, body) {
  busy.value = true;
  try { state.value = await request.post(`/collection/bioproject/${path}`, body && path.startsWith("jobs/") ? { ...body, projectAccessions: projectCodes.value.split(/[,;\s]+/).filter(Boolean) } : body); await refresh(); }
  catch (e) { ElMessage.error(e.message); }
  finally { busy.value = false; }
}
onBeforeUnmount(() => clearTimeout(timer));
</script>
<template>
  <el-button @click="show">BioProject文献采集</el-button>
  <el-dialog v-model="open" title="BioProject支持文章独立采集" width="min(720px, 96vw)" @closed="clearTimeout(timer)">
    <p>按官方项目列表读取每个项目的全部出版信息。提交单位单独保留；作者机构从论文元数据读取。</p>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <el-input v-model="projectCodes" placeholder="可选项目编号，如 PRJCA039050；留空扫描官方NGDC项目列表，多个用逗号分隔" style="margin: 12px 0" />
    <el-form :model="form" label-width="120px" inline>
      <el-form-item label="本地批次"><el-input-number v-model="form.batchSize" :min="1" :max="200" /></el-form-item>
      <el-form-item label="请求间隔(ms)"><el-input-number v-model="form.minDelayMs" :min="350" :max="10000" /></el-form-item>
      <el-form-item label="请求重试次数"><el-input-number v-model="form.maxAttempts" :min="1" :max="5" /></el-form-item>
    </el-form>
    <p>{{ state.message || '尚未启动' }}</p>
    <p>阶段：{{ state.job?.phase || '—' }} · 作业状态：{{ state.job?.status || '—' }} · 列表游标：{{ state.job?.nextIndex ?? 0 }} · 已处理补全：{{ state.job?.metadataProcessed ?? 0 }}</p>
    <p v-if="state.currentRecord">当前文献：{{ state.currentRecord }}</p>
    <el-alert v-if="state.job?.lastError" :title="state.job.lastError" type="warning" :closable="false" />
    <el-button :disabled="state.running || busy" type="primary" @click="action('jobs/start', form)">开始 / 断点继续</el-button>
    <el-button :disabled="!state.running || busy" @click="action('jobs/stop')">停止</el-button>
    <el-button @click="refresh">刷新</el-button>
    <el-button :disabled="state.running || busy" @click="action('jobs/restart', form)">放弃旧游标，新建扫描</el-button>
    <el-button :disabled="state.running || busy" @click="action('enrichment/reset')">重置元数据补全</el-button>
    <el-table :data="state.enrichmentCounts || []"><el-table-column prop="status" label="补全状态" /><el-table-column prop="total" label="文献数" /></el-table>
    <p>主采集完成后，通过公共关联接口选择 BioProject 开始关联文献发现。生成新快照后可查看最新统计。</p>
  </el-dialog>
</template>

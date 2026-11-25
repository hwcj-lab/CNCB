<script setup>
import { ref, onBeforeUnmount } from "vue";
import { ElMessage } from "element-plus";
import request from "../../../shared/api/request";
const open = ref(false), busy = ref(false), state = ref({}), error = ref("");
const form = ref({ batchSize: 100, minDelayMs: 800, maxAttempts: 3 });
let timer;
async function refresh() {
  clearTimeout(timer);
  try {
    state.value = await request.get("/collection/gsa/status"); error.value = "";
    if (open.value && state.value.running) timer = setTimeout(refresh, 2500);
  } catch (e) { error.value = e.message; }
}
async function show() { open.value = true; await refresh(); }
async function action(path, body) {
  busy.value = true;
  try { state.value = await request.post(`/collection/gsa/${path}`, body); await refresh(); }
  catch (e) { ElMessage.error(e.message); }
  finally { busy.value = false; }
}
onBeforeUnmount(() => clearTimeout(timer));
</script>
<template>
  <el-button @click="show">GSA文献采集</el-button>
  <el-dialog v-model="open" title="GSA支持文章独立采集" width="min(720px, 96vw)" @closed="clearTimeout(timer)">
    <p>采集页面全部支持文章（包含HRA、OMIX等），统一归入GSA。保留原始资源编号和项目号，作者机构按逐作者原文保存。</p>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
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
    <p>主采集完成后，通过公共关联接口选择 GSA 开始关联文献发现。生成新快照后可查看最新统计。</p>
  </el-dialog>
</template>

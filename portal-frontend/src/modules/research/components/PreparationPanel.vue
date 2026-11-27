<script setup>
import { ref, onMounted, onBeforeUnmount } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import request from "../../../shared/api/request";
const emit = defineEmits(["status"]);
const state = ref(null), error = ref(""), selected = ref([]), busy = ref(false);
let timer;
async function refresh() {
  try {
    state.value = await request.get("/research-preparation/status");
    error.value = "";
    emit("status", state.value);
  } catch (e) { error.value = e.message; emit("status", { canBuild: false }); }
}
async function action(name) {
  try {
    if (name === "discard") await ElMessageBox.confirm("丢弃未完成的整理版本并恢复上一个整理结果；原文献和已发布快照保留。", "丢弃整理任务");
    if (name === "rebuild") await ElMessageBox.confirm("重新解析选中来源的全部本地记录；未选择表示全部来源。不会请求上游。", "重建整理成员");
    busy.value = true;
    const payload = ["incremental", "start", "rebuild"].includes(name) ? { sourceCodes: selected.value, batchSize: 200 } : undefined;
    state.value = await request.post(`/research-preparation/${name}`, payload);
    emit("status", state.value);
    ElMessage.success("整理任务状态已更新");
  } catch (e) { if (e !== "cancel" && e !== "close") ElMessage.error(e.message || "操作失败"); }
  finally { busy.value = false; }
}
onMounted(() => { refresh(); timer = window.setInterval(refresh, 3000); });
onBeforeUnmount(() => window.clearInterval(timer));
</script>
<template>
  <section class="preparation-panel" aria-label="统计数据整理">
    <h3>统计数据整理</h3>
    <p>采集完成后整理本地数据，再生成快照。首次增量同步会建立基线，之后仅处理变化记录；完整扫描可校验物理删除。</p>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <template v-if="state">
      <el-select v-model="selected" multiple clearable collapse-tags placeholder="全部已接入来源" :disabled="state.running || busy" style="width: min(470px, 100%)">
        <el-option v-for="source in state.sources" :key="source.code" :label="source.name" :value="source.code" />
      </el-select>
      <div class="preparation-actions">
        <el-button type="primary" @click="action('incremental')" :disabled="state.running || busy">增量同步</el-button>
        <el-button @click="action('start')" :disabled="state.running || busy">完整扫描 / 断点继续</el-button>
        <el-button @click="action('rebuild')" :disabled="state.running || busy">重新构建成员</el-button>
        <el-button v-if="state.running" @click="action('stop')" :disabled="busy">停止</el-button>
        <el-button @click="refresh" :disabled="busy">刷新状态</el-button>
        <el-button v-if="state.job?.status === 'RUNNING'" @click="action('recover')" :disabled="busy">恢复中断状态</el-button>
        <el-button v-if="['FAILED', 'STOPPED'].includes(state.job?.status)" @click="action('discard')" :disabled="busy">丢弃未完成版本</el-button>
      </div>
      <p v-if="state.job">状态 {{ state.job.status }} · 模式 {{ state.job.scanMode }} · 来源进度 {{ state.job.sourceIndex }} · 时间游标 {{ state.job.afterUpdateTime || '—' }} / ID {{ state.job.afterId }} · 已扫描 {{ state.job.rowsScanned }} · 已更新 {{ state.job.rowsChanged }}</p>
      <el-collapse v-if="state.sourceCursors?.length">
        <el-collapse-item title="各来源读取范围与断点" name="cursors">
          <el-table :data="state.sourceCursors" size="small">
            <el-table-column prop="sourceCode" label="来源" min-width="140" />
            <el-table-column prop="scanMode" label="模式" width="140" />
            <el-table-column prop="status" label="状态" width="120" />
            <el-table-column prop="untilTime" label="本轮时间上界（不含）" min-width="200" />
            <el-table-column prop="afterUpdateTime" label="已提交时间游标" min-width="200" />
            <el-table-column prop="afterId" label="已提交ID游标" width="140" />
          </el-table>
        </el-collapse-item>
      </el-collapse>
      <el-alert v-if="state.job?.errorMessage" :title="state.job.errorMessage" type="warning" :closable="false" />
      <p>{{ state.canBuild ? `整理版本 ${state.readyRevision} 可用于新快照` : '整理未完成，暂不能生成新快照。' }}<span v-if="state.missingSources?.length"> 尚未整理：{{ state.missingSources.join('、') }}</span></p>
    </template>
  </section>
</template>
<style scoped>
.preparation-panel { border: 1px solid var(--el-border-color); border-radius: 10px; padding: 18px 22px; margin: 18px 0; }
.preparation-panel h3 { margin: 0 0 10px; }
.preparation-panel p { font-size: 13px; line-height: 1.7; }
.preparation-actions { display: flex; flex-wrap: wrap; gap: 8px; margin: 14px 0; }
.preparation-actions .el-button { margin: 0; }
</style>

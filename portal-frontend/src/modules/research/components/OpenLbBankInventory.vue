<script setup>
import { ref } from 'vue';
import request from '../../../shared/api/request';
import { ElMessage } from 'element-plus';
const open=ref(false), status=ref({}), rows=ref([]), busy=ref(false);
async function refresh(){status.value=await request.get('/collection/openlb/banks/status');rows.value=await request.get('/collection/openlb/banks/summary');}
async function show(){open.value=true;try{await refresh();}catch(e){ElMessage.error(e.message);}}
async function action(name){busy.value=true;try{const result=await request.post('/collection/openlb/banks/'+name);await refresh();}catch(e){ElMessage.error(e.message);}finally{busy.value=false;}}
</script>
<template><el-button @click="show">OpenLB来源盘点</el-button><el-dialog v-model="open" title="OpenLB查漏补缺 · 来源盘点" width="700px"><p>读取已导入的PMID任务，只保存cncbBanks；后台执行，可停止并从持久化断点继续。</p><p>{{ status.message }} · 游标 {{ status.cursor || '未开始' }}</p><el-button :disabled="status.running || busy" @click="action('start')">开始 / 继续盘点</el-button><el-button :disabled="!status.running" @click="action('stop')">停止</el-button><el-button @click="refresh">刷新进度</el-button><el-table :data="rows"><el-table-column prop="db" label="数据库"/><el-table-column prop="records" label="资源数"/><el-table-column prop="publications" label="文献数"/></el-table></el-dialog></template>

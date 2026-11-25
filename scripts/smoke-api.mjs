// 启动两个服务后执行；BASE_URL可指向MySQL profile或Vite代理。
import assert from 'node:assert/strict';
const base=process.env.BASE_URL||'http://127.0.0.1:8080';
async function get(path,status=200){
 const response=await fetch(base+path,{signal:AbortSignal.timeout(15000)});
 assert.equal(response.status,status,path);
 const body=await response.json();if(status===200)assert.equal(body.code,'OK',path);
 return body.data;
}
const system=await get('/api/system/status');console.log('Backend database:',system.database,'demoEnabled:',system.demoEnabled);
assert.equal((await get('/api/databases')).length,23);
assert.equal((await get('/api/tools/1')).linkEnabled,false);
assert.equal((await get('/api/databases/10/tools?dataType=DNA')).length,0);
assert.ok((await get('/api/tools/recommendations?dataType=PROTEIN')).some(t=>t.name==='Needle'));
const real=await get('/api/research/summary?mode=REAL');
assert.equal(real.dataMode,'REAL');assert.equal(real.coverage.totalPublications,3);assert.equal(real.metrics[2].value,null);
for(const kind of ['annual','topics','institutions','authors','funds'])assert.equal((await get('/api/research/'+kind+'?mode=REAL')).dataMode,'REAL');
await get('/api/research/annual?fromYear=2026&toYear=2020',400);await get('/api/databases/99999',404);
assert.equal((await get('/api/research/sources')).length,18);
if(system.demoEnabled){
 const demo=await get('/api/research/summary?mode=DEMO');assert.equal(demo.coverage.totalPublications,30);
 const annual=await get('/api/research/annual?mode=DEMO');assert.equal(annual.items.reduce((s,r)=>s+r.value,0),30);
 assert.ok((await get('/api/research/institutions/101/trend?mode=DEMO')).items.length);
 assert.ok((await get('/api/research/authors/101/trend?mode=DEMO')).items.length);
 assert.equal((await get('/api/research/publications?mode=DEMO&page=2&size=5')).items.length,5);
}else{await get('/api/research/summary?mode=DEMO',403);}
console.log('PASS: catalog, typed recommendations, statistics, isolation, validation, pagination and sources');

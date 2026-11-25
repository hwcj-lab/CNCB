// 从现有调研表生成目录种子，不补造上游信息。
// 重跑会覆盖database/init-data.sql和demo-data.sql。请先更新调研及显式映射并评审。
import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const read=async p=>fs.readFile(path.join(root,p),'utf8');
const parse=(text,prefix)=>text.split(/\r?\n/).filter(l=>new RegExp('^\\| '+prefix+'\\d\\d \\|').test(l)).map(l=>l.split('|').slice(1,-1).map(c=>c.trim()));
const url=s=>s.match(/\]\((https?:\/\/.*?)\)/)?.[1] || s.replaceAll(String.fromCharCode(96),'');
const q=v=>v===null||v===undefined?'NULL':typeof v==='boolean'?(v?'TRUE':'FALSE'):typeof v==='number'?String(v):"'"+String(v).replaceAll("'","''")+"'";
let sql='-- 自动生成自research；调研日期2026-11-06；目录确认不等于在线运行已验证。\n-- 仅在新库执行一次；不包含合成论文/机构/作者/基金。不要重复导入。\n';
const insert=(table,columns,values)=>{sql+='INSERT INTO '+table+' ('+columns.join(',')+') VALUES ('+values.map(q).join(',')+');\n'};
const types=[
[1,'DNA','DNA 序列','区分实际DNA序列、原始reads、元数据关联与工具输入支持',true,1],
[2,'PROTEIN','蛋白序列','仅确认真实蛋白序列/文件的资源可标记直接序列',true,2],
[3,'LITERATURE','文献数据','文献检索、关联和元数据服务，不冒充文献分析算法',true,3],
[4,'SPECIES','物种','分类信息与鉴定工具需分开',true,4],
[5,'RNA_EXPRESSION','RNA表达','表达矩阵不等于核酸序列',false,5],
[6,'METHYLATION','DNA甲基化','甲基化值不等于DNA序列',false,6],
[7,'VARIANT','基因组变异','变异和坐标数据',false,7],
[8,'GENE_ID','基因标识','ID映射与基因查询',false,8],
[9,'RNA_SEQUENCE','RNA序列','仅工具明确支持时使用',false,9],
[10,'NUCLEOTIDE','核酸序列（类型待细分）','不将病毒核酸自动归成DNA',false,10]];
for(const t of types)insert('data_type',['id','code','name','description','core_type','sort_order'],t);
for(const c of [[1,'PROJECT','项目'],[2,'SAMPLE','样本'],[3,'TOOL','工具'],[4,'DATABASE','数据库'],[5,'SEQUENCE','序列资源'],[6,'LITERATURE','文献'],[7,'TAXONOMY','分类信息'],[8,'SPECIALIZED','专题资源']])insert('resource_category',['id','code','name'],c);
const categories={'BioProject':1,'BioSample':2,'BioCode':3,'Database Commons':4,'GenBase':5,'GSA':5,'GSA-Human':5,'GWH':5,'OpenLB':6,'Taxonomy':7};
const dbs=parse(await read('research/task1_tool_recommend/database_inventory.md'),'D');
const tools=parse(await read('research/task1_tool_recommend/tool_inventory.md'),'T');
const apis=parse(await read('research/task2_research_analysis/api_inventory.md'),'A');
if(dbs.length!==23||tools.length!==32||apis.length!==18)throw Error('调研表数量改变，请评审生成器');
const dbId=new Map(dbs.map(r=>[r[1],Number(r[0].slice(1))]));
const relationSet=new Set(), toolTypeSet=new Set(), dbTypeSet=new Set();
const addDbType=(d,t,role,note)=>{const key=d+':'+t;if(dbTypeSet.has(key))return;dbTypeSet.add(key);insert('database_data_type',['database_id','data_type_id','evidence_role','evidence_note'],[d,t,role,note]);};
for(const r of dbs){
 const id=Number(r[0].slice(1));
 const state=r[13].startsWith('已验证')?'PAGE_VERIFIED':r[13].includes('索引')?'INDEX_CONFIRMED':r[13].includes('已验证')?'PARTIALLY_VERIFIED':'DOCUMENTED';
 insert('database_resource',['id','code','name','chinese_name','english_name','website','domain','description','verification_status','status_text','source_url','verified_on','data_origin','category_id'],[id,r[0],r[1],r[2],r[3],url(r[4]),r[5],r[6],state,r[13],url(r[12]),'2026-11-06','RESEARCH',categories[r[1]]||8]);
 for(const [idx,type] of [[7,1],[8,2],[9,3],[10,4]]) {
  const v=r[idx];if(/^(是|关联|工具支持|有工具文献|有库引用)/.test(v))addDbType(id,type,v.startsWith('工具支持')?'TOOL_INPUT':v.startsWith('是')?'DIRECT_OR_RECORD_SPECIFIC':'RELATED_METADATA',v);
 }
}
for(const [name,type] of [['GEN',5],['LncExpDB',5],['MethBank',6],['GVM',7],['LncBook',9],['IC4R',8],['RCoV19',10]])addDbType(dbId.get(name),type,'DOCUMENTED','见资源及工具调研；须按记录检查输入格式');
const owner={8:'GWH',9:'Taxonomy',10:'Taxonomy',11:'Taxonomy',12:'Taxonomy',13:'Taxonomy',14:'Taxonomy',15:'RCoV19',17:'MethBank',18:'MethBank',19:'MethBank',20:'GVM',21:'GVM',22:'LncBook',24:'LncBook',25:'LncBook',26:'GEN',27:'LncExpDB',28:'IC4R',29:'IC4R',30:'IC4R',31:'IC4R',32:'GenBase'};
const kinds={14:'BROWSER',16:'DETAIL',26:'ANALYSIS_PORTAL',32:'HELP'};
for(const r of tools){
 const id=Number(r[0].slice(1));
 const enabled=r[10].startsWith('是') && r[8].startsWith('已验证') && id!==1;
 const status=r[8].startsWith('已验证')?'PAGE_VERIFIED':r[8].includes('索引')?'INDEX_CONFIRMED':'DOCUMENTED';
 insert('online_tool',['id','code','name','owner_database_id','description','supported_data','input_description','entry_url','platform','service_kind','verification_status','status_text','link_enabled','run_verified','login_requirement','source_url','verified_on'],[id,r[0],r[1],owner[id]?dbId.get(owner[id]):null,r[3],r[4],r[5],url(r[6]),r[7],kinds[id]||([8,29].includes(id)?'BROWSER':'ANALYSIS'),status,r[8],enabled,false,r[9],url(r[11]),'2026-11-06']);
}
function bind(d,t,ty,kind,basis) {
 const key=[d,t,ty].join(':');if(relationSet.has(key))return;relationSet.add(key);
 const evidence=tools.find(r=>Number(r[0].slice(1))===t);
 const source=evidence?url(evidence[11]):t===33?'https://ngdc.cncb.ac.cn/openlb/home':'https://ngdc.cncb.ac.cn/icg/api';
 insert('database_tool',['database_id','tool_id','data_type_id','relation_kind','relation_basis','verification_status','source_url'],[d,t,ty,kind,basis,'RESEARCH_BASED',source]);
 const k=t+':'+ty;if(!toolTypeSet.has(k)){toolTypeSet.add(k);insert('tool_data_type',['tool_id','data_type_id','evidence_note'],[t,ty,basis]);}
}
// 数据库工具映射表另有两个文献服务，不属于32项重点分析入口；按服务类型明确区分。
for(const s of [[33,'S01','OpenLB文献检索',13,'检索与中心资源关联导航；批量及cncbbank待接入','https://ngdc.cncb.ac.cn/openlb/home','LITERATURE_SEARCH',false,'首页框架可读；数据访问不稳定'],[34,'S02','ICG PMID查询',22,'按PMID取得文献基础元数据；不是计量分析算法','https://ngdc.cncb.ac.cn/icg/api/json/publication/24810581','DATA_SERVICE',true,'调研样例JSON已验证；URL为官方PMID示例']])insert('online_tool',['id','code','name','owner_database_id','description','supported_data','input_description','entry_url','platform','service_kind','verification_status','status_text','link_enabled','run_verified','login_requirement','source_url','verified_on'],[s[0],s[1],s[2],s[3],s[4],'文献','文献关键词或PMID；具体限制见调研',s[5],s[3]===13?'OpenLB':'ICG',s[6],s[7]?'RESPONSE_VERIFIED':'DOCUMENTED',s[8],s[7],false,'未确认统一认证要求',s[5],'2026-11-06']);
const bindNames=(names,ids,types,kind,basis)=>{for(const n of names)for(const id of ids)for(const ty of types)bind(dbId.get(n),id,ty,kind,basis)};
bindNames(['GenBase','GWH'],[2,4],[1,2],'COMPATIBILITY_SUGGESTION','本项目兼容性推荐；配置页支持核酸/蛋白；不是原生自动数据联动');
bindNames(['GenBase','GWH'],[1],[1,2],'CANDIDATE','中心BLAST说明确认；入口空壳、库覆盖待验证，跳转禁用');
bindNames(['GWH'],[8],[1],'NATIVE','GWH官网原生导航；链接为官网示例组装区间');
bindNames(['Taxonomy'],[9],[1,2,9],'NATIVE','官方表单明确DNA、Protein、RNA；系统进化输出待验证');
bindNames(['Taxonomy'],[13],[1,4],'NATIVE','官方仅接受DNA序列；条形码鉴定与构树');
bindNames(['Taxonomy'],[14],[4],'NATIVE','分类信息服务；示例9605为Homo属，不等于完整物种画像');
bindNames(['RCoV19'],[15],[10],'NATIVE','病毒核酸系统进化入口；具体格式和运行待核');
bindNames(['IC4R'],[28],[10],'CANDIDATE','水稻序列比对入口；DNA/蛋白选项待核，不强行归入已确认蛋白输入');
bindNames(['LncBook'],[22],[9],'NATIVE','lncRNA比对；链接445，禁用');
bindNames(['GEN'],[26],[5],'NATIVE','转录组在线分析入口；不是DNA序列工具');
bindNames(['MethBank'],[17,18,19],[6],'NATIVE','甲基化资料输入，不是原始DNA序列');
bindNames(['LncExpDB'],[27],[5],'NATIVE','共表达网络；仅索引确认，待交互验证');
bindNames(['IC4R'],[29],[1],'NATIVE','官网旧域名浏览器；Cache miss，禁用');
bindNames(['IC4R'],[30],[8],'NATIVE','水稻基因ID转换');
bindNames(['OpenLB'],[33],[3],'NATIVE','文献检索服务；不是BIT分析工具；入口不稳定禁用');
bindNames(['ICG'],[34],[3],'NATIVE','文献查询数据服务；官方PMID示例，不支持自动换参或批量承诺');
for(const r of apis)insert('source_endpoint',['id','code','name','method','address','parameters_text','response_description','verification_status','status_text','document_url','verified_on'],[Number(r[0].slice(1)),r[0],r[1],r[2],r[3].replaceAll(String.fromCharCode(96),''),r[4],r[5],r[6].startsWith('已验证')?'RESPONSE_VERIFIED':'DOCUMENTED',r[6],url(r[7]),'2026-11-06']);
// 仅保留调研中实际看到的三条基础文献样例。不补造标题、作者、机构或基金。
const pubs=[
 [1,'34939160','10.1007/s11427-021-2033-2',null,2022,null,null,'https://ngdc.cncb.ac.cn/gwh/api/public/bioProject/PRJCA000437','RESPONSE_VERIFIED','REAL'],
 [2,'32070270','10.1186/s12864-020-6579-z',null,2020,null,null,'https://ngdc.cncb.ac.cn/gwh/api/public/bioProject/PRJCA000437','RESPONSE_VERIFIED','REAL'],
 [3,'24810581',null,null,2014,null,null,'https://ngdc.cncb.ac.cn/icg/api/json/publication/24810581','RESPONSE_VERIFIED','REAL']];
for(const p of pubs)insert('publication',['id','pmid','doi','title','pub_year','journal','authors_raw','source_url','verification_status','data_mode'],p);
for(const p of pubs)insert('publication_database',['publication_id','database_id','resource_accession','relation_kind','verification_status','evidence_url'],[p[0],dbId.get(p[0]===3?'ICG':'GWH'),p[0]===3?'PMID:24810581':'PRJCA000437',p[0]===3?'CATALOG_ENTRY':'PROJECT_REFERENCE','OBSERVED',p[7]]);
await fs.mkdir(path.join(root,'database'),{recursive:true});
await fs.writeFile(path.join(root,'database/init-data.sql'),sql,'utf8');
console.log('Generated: '+dbs.length+' databases, '+tools.length+' entries, '+apis.length+' APIs; 3 real limited samples.');
sql='-- 仅演示/测试库手动导入；真实模式默认禁止DEMO查询。全部文献、机构、人员、基金均为合成。\n';
for(let i=1;i<=5;i++)insert('author',['id','name','external_id','disambiguated','data_mode'],[100+i,'演示研究者 '+['甲','乙','丙','丁','戊'][i-1],'DEMO-AUTHOR-'+i,true,'DEMO']);
for(let i=1;i<=4;i++)insert('institution',['id','name','external_id','data_mode'],[100+i,'演示机构 '+['A','B','C','D'][i-1],'DEMO-INSTITUTION-'+i,'DEMO']);
for(let i=1;i<=3;i++)insert('fund_project',['id','project_number','agency','name','data_mode'],[100+i,'DEMO-FUND-0'+i,'演示资助方','演示基金项目 '+i,'DEMO']);
for(let i=0;i<30;i++){
 const id=1001+i, year=2018+Math.floor(i/4), a=101+(i%5), ins=101+(i%4);
 insert('publication',['id','title','pub_year','source_url','verification_status','data_mode'],[id,'演示文献 '+String(i+1).padStart(2,'0')+'（合成，仅用于展示）',year,'DEMO:synthetic','SYNTHETIC','DEMO']);
 const databaseIds=[1,2,11,12];
 insert('publication_database',['publication_id','database_id','resource_accession','relation_kind','verification_status','evidence_url'],[id,databaseIds[i%4],'DEMO-RESOURCE-'+id,'DEMO_ASSOCIATION','SYNTHETIC','DEMO:synthetic']);
 if(i%6===0)insert('publication_database',['publication_id','database_id','resource_accession','relation_kind','verification_status','evidence_url'],[id,3,'DEMO-SECOND-'+id,'DEMO_ASSOCIATION','SYNTHETIC','DEMO:synthetic']);
 insert('publication_author',['id','publication_id','author_id','author_order','raw_name'],[id,id,a,1,'演示研究者']);
 insert('publication_institution',['publication_id','institution_id','relation_role','evidence_url'],[id,ins,'AUTHOR_AFFILIATION','DEMO:synthetic']);
 insert('publication_author_institution',['publication_author_id','institution_id','raw_affiliation','evidence_url'],[id,ins,'演示署名机构','DEMO:synthetic']);
 if(i%3!==0)insert('publication_fund',['publication_id','fund_id','relation_role','evidence_url'],[id,101+i%3,'PAPER_FUNDING','DEMO:synthetic']);
}
await fs.writeFile(path.join(root,'database/demo-data.sql'),sql,'utf8');

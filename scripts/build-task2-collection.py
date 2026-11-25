from pathlib import Path

import json,re

BASE='http://127.0.0.1:8080'

def req(name,method,path,desc,body=None):

 r={'method':method,'header':[{'key':'Accept','value':'application/json'}],'url':BASE+path,'description':desc}

 if body is not None:r['header'].append({'key':'Content-Type','value':'application/json'});r['body']={'mode':'raw','raw':json.dumps(body,ensure_ascii=False,indent=2),'options':{'raw':{'language':'json'}}}

 return {'name':name,'request':r}

def folder(name,desc,items):return {'name':name,'description':desc,'item':items}

O='/api/collection/openlb';B=O+'/banks';G='/api/collection/genbase';R='/api/collection/publication-relations';S='/api/research-snapshots';I='/api/research-publications/integration'

openlb=[

req('01 导入服务端CSV中的PMID任务','POST',O+'/tasks/import-csv','【OpenLB专用·本地写入】读取服务端配置的CSV文件，不是文件上传接口。按PMID插入忽略重复；已有成功任务不重置。已有任务可跳过此步骤。'),

req('02 开始 / 继续OpenLB补缺采集（访问上游）','POST',O+'/jobs/start','【OpenLB专用·访问上游】领取未完成PMID，读取OpenLB文章，按cncbBanks.db及路由写对应文献表；空cncbBanks或未配置db保留到openlb_publication。仅主采集，不自动启动关联采集。响应立即返回，使用本组status轮询。concurrency=1..8；batchSize=1..200；minDelayMs=100..10000全局间隔；maxAttempts=1..10；stopAfterConsecutiveThrottles=1..20。目标表须已创建。',{'concurrency':3,'batchSize':60,'minDelayMs':800,'maxAttempts':3,'stopAfterConsecutiveThrottles':5}),

req('03 查询OpenLB补缺采集状态','GET',O+'/status','【OpenLB专用·只读本地】查看运行标记、限速间隔、任务状态数量、停止原因。只查询OpenLB主采集，不代表关联任务、盘点任务或快照状态。'),

req('04 停止OpenLB补缺采集','POST',O+'/jobs/stop','【OpenLB专用】请求当前批次收尾后停止。再次调用jobs/start继续未完成任务。'),

req('05 重置OpenLB失败任务供重试','POST',O+'/tasks/retry-failed','【OpenLB专用·本地写入】仅FAILED改为PENDING；不重置SUCCESS/EMPTY，不重新导入CSV。随后调用jobs/start。')]

banks=[

req('01 开始 / 继续cncbBanks来源盘点（访问上游）','POST',B+'/start','【OpenLB专用·访问上游】按openlb_pmid_task全部PMID读取OpenLB，仅保存cncbBanks各字段到openlb_bank_inventory；不修改原主采集任务状态，不分流文献。独立持久化游标；重复start从游标继续。异常或限流停止，状态中反馈。'),

req('02 查询来源盘点进度','GET',B+'/status','【OpenLB专用·只读本地】返回盘点running、message、cursor。与OpenLB主采集status独立。'),

req('03 停止来源盘点','POST',B+'/stop','【OpenLB专用】请求盘点停止，断点保留，再start继续。'),

req('04 查看已盘点数据库及文献数量','GET',B+'/summary','【OpenLB专用·只读本地】按db分组：records为cncbBanks资源行数；publications为该db去重PMID数。一个PMID可有多个资源/数据库，records通常大于publications。'),

req('05 分页查看cncbBanks明细','GET',B+'/records?page=1&size=100','【OpenLB专用·只读本地】page从1开始；size限制为1..500。返回查询PMID、bank_index、name、accession、link、db、bank_pmid。'),

req('06 本地文献重新分流100条（不访问上游）','POST',B+'/redistribute-existing?afterId=0','【OpenLB专用·本地写入】读取openlb_publication中id>afterId的最多100条，按最新路由写入目标表并复制旧OPENLB关联边，随后同步这些PMID的整合记录。返回processed、nextAfterId、pmids。下一次将URL中的afterId改为返回值，直到processed=0。新增路由后从afterId=0重新扫描；不删除OpenLB原记录。目标表和路由须预先配置。')]

genbase=[

req('01 开始 / 继续GenBase分页及元数据采集（访问上游）','POST',G+'/jobs/start','【GenBase专用·访问上游】读取支持文章分页，写genbase_publication并读取该库元数据。仅使用来源原有PMID查询详情；无PMID不反查、不补写，可按DOI/标题核对BioProject内对应文章读取基金等原始信息。停止/失败从持久化进度恢复。pageSize=10..500；minDelayMs=200..10000；maxAttempts=1..10；stopAfterConsecutiveThrottles=1..20。主采集不等于关联采集。',{'pageSize':100,'minDelayMs':800,'maxAttempts':3,'stopAfterConsecutiveThrottles':5}),

req('02 查询GenBase采集状态','GET',G+'/status','【GenBase专用·只读本地】查看jobId、nextStart、计数、补全进度及错误。与其他任务状态独立。'),

req('03 停止GenBase采集','POST',G+'/jobs/stop','【GenBase专用】当前页处理完成后停止，保留断点及已入库数据。再次jobs/start继续。'),

req('04 重置GenBase的元数据采集状态','POST',G+'/enrichment/reset','【GenBase专用·本地写入】保留文献，将主列表文献（含旧NO_PMID记录）重置为待元数据采集；随后调用GenBase jobs/start。不是清空文献，也不重置OpenLB CSV任务。')]

gsa=[

req('01 开始 / 断点继续GSA主采集','POST','/api/collection/gsa/jobs/start','【GSA专用·访问上游】读取statistics/statistics.json中的article.publications全部支持文章（含HRA/OMIX），统一归入GSA。固定JSON快照缓存到服务器磁盘，流式分批入库，IMPORT游标及ENRICH状态持久化。优先PMID用OpenLB补全，缺失时回退PubMed；无PMID有DOI时尝试匹配PMID，不猜测多匹配结果。保留逐作者机构及原始基金；完成后关联文献另启公共接口。batchSize=1..200，minDelayMs=350..10000，maxAttempts=1..5。先执行GSA专用建表升级SQL。',{'batchSize':100,'minDelayMs':800,'maxAttempts':3}),

req('02 查询GSA阶段及补全状态','GET','/api/collection/gsa/status','【GSA专用·本地只读】running、message、currentRecord、currentDelayMs、job.phase/status/nextIndex、enrichmentCounts。SUCCESS表示主采集流程结束，NO_PMID/EMPTY表示未获得完整元数据，不代表数据丢弃；关联采集状态另查公共关联jobs/status。'),

req('03 安全停止GSA采集','POST','/api/collection/gsa/jobs/stop','【GSA专用】下载/导入/补全阶段请求停止；已提交批次不回滚。再次jobs/start续做。'),

req('04 新建GSA全量列表扫描','POST','/api/collection/gsa/jobs/restart','【GSA专用·访问上游】显式放弃旧扫描游标，下载新的统计JSON；不删除任何文献，不重置成功补全标记。列表未变的记录不重复补全。用于缓存文件丢失或需要重新扫描；正常中断使用jobs/start。',{'batchSize':100,'minDelayMs':800,'maxAttempts':3}),

req('05 重置GSA元数据补全','POST','/api/collection/gsa/enrichment/reset','【GSA专用·本地写入】将GSA主文献补全状态重置为PENDING，保留作者基金原始数据。随后jobs/start。不会重置OpenLB CSV任务，也不会自动启动关联发现。')]

common='【公共关联流程·适用于已注册真实来源】databaseCode填路由的source_code（如GSA/GWH/BIOCODE/DATABASECOMMONS/BIOPROJECT/ASCANCERATLAS/GENBASE等），不是cncbBanks.db显示名。OpenLB已不作为统计来源，不使用OPENLB启动新库任务。新增路由不自动实现资源解析器；已接入BioCode、Database Commons、BioProject、GSA、GWH、ASCancerAtlas（ASC接口当前实测404，失败会记录，不当作空结果）。其他库可走已有页面回退，但不保证动态页面可解析。'

bpbase='/api/collection/bioproject'

bioproject=[

req('01 BioProject全列表采集 / 断点继续','POST',bpbase+'/jobs/start','【BioProject专用·访问上游】官方NGDC项目列表磁盘快照→每项目全部出版信息→PMID优先OpenLB、空结果回退PubMed→本地表UPSERT→全局表增量同步。projectAccessions不传/空数组扫描官方NGDC列表；传数组只处理指定项目。STOPPED/FAILED恢复原作业，请勿同时更换项目范围；SUCCESS后start新建扫描。提交单位保留在原始载荷中，绝不直接当成作者署名。',{'batchSize':100,'minDelayMs':800,'maxAttempts':3,'projectAccessions':[]}),

req('02 BioProject重新扫描（保留文献）','POST',bpbase+'/jobs/restart','【BioProject专用·访问上游】放弃旧作业游标、新建固定列表。示例只处理一个项目用于小批验证。保留旧文献、关系及OpenLB任务；同篇按PMID/DOI/稳定来源键更新。',{'batchSize':20,'minDelayMs':800,'maxAttempts':3,'projectAccessions':['PRJCA039050']}),

req('03 BioProject安全停止','POST',bpbase+'/jobs/stop','【BioProject专用】等待当前请求/批次收尾后停止，已提交游标保留；jobs/start断点继续。'),

req('04 BioProject状态及断点','GET',bpbase+'/status','【BioProject专用·本地只读】running、phase、nextIndex、currentRecord、enrichmentCounts；nextIndex是已提交的项目数，rowsImported是出版信息导入次数。'),

req('05 BioProject重置元数据补全','POST',bpbase+'/enrichment/reset','【BioProject专用·本地写入】只将主文献元数据状态设为PENDING，不删论文/关系。之后jobs/start执行；不改OpenLB导入任务。')]

relations=[

req('01 从指定库领取待关联文献（无需手填PMID）','POST',R+'/jobs/start-pending-batch',common+'\n【访问上游】从指定来源表领取PENDING，includeFailed=true同时领取FAILED。batchSize=1..5000；maxPublications=1..100000为本次处理节点上限，达到后PAUSED；requestDelayMs=100..60000。每次作业选择一个来源库。',{'databaseCode':'GSA','batchSize':100,'includeFailed':False,'maxPublications':1000,'requestDelayMs':800}),

req('02 单篇刷新 / 重新发现关联文献','POST',R+'/jobs/start',common+'\n【访问上游】identifiers是该库已有文献的唯一值。通用同结构库使用pmid；GENBASE可用paperId、pmid或doi；GSA/BIOPROJECT可用pmid、doi或sourceKey。refreshPublication控制是否重新拉文章正文；refreshRelations控制是否重新发现关联边。两者均true用于单篇全部刷新，false/true只重查关联。请将示例PMID替换为目标库真实存在值。',{'databaseCode':'GSA','identifiers':{'pmid':'41028458'},'refreshPublication':False,'refreshRelations':True,'maxPublications':1000,'requestDelayMs':800}),

req('03 查询公共关联作业状态','GET',R+'/jobs/status?jobId=1','【公共关联·只读本地】jobId替换为启动返回的真实作业ID；不传jobId时查询当前/最近作业。状态包括pendingCount、processedCount、discoveredCount、relationCount、lastError。该status仅共用于关联采集，不是所有任务的统一状态。'),

req('04 停止当前公共关联作业','POST',R+'/jobs/stop','【公共关联】安全停止当前关联作业，不停止OpenLB主采集、GenBase主采集、来源盘点或快照任务。'),

req('05 断点恢复指定公共关联作业','POST',R+'/jobs/1/resume','【公共关联·访问上游】将路径1替换为真实jobId；恢复PAUSED/STOPPED/FAILED任务并处理队列未完成项。重试来源表中的失败文献也可使用start-pending-batch并传includeFailed=true。'),

req('06 分页查询某文献的双向关联文献','GET',R+'/publications/GSA/PMID/41028458?page=1&size=20','【公共查询·只读本地】GSA替换为实际source_code；PMID替换为identifierType（PMID/PAPER_ID/DOI/SOURCE_KEY），后段填实际唯一值。DOI包含斜杠时优先用同组查询参数接口，避免服务器路径编码限制。page从1开始；size=1..100。返回title、pmid、doi、identifierValue等；页面据PMID或DOI生成外部论文跳转。')]

relations.append(req('07 关联文献查询（查询参数形式，支持DOI斜杠）','GET',R+'/publications?databaseCode=GSA&identifierType=DOI&identifier=10.1234%2Fabc&page=1&size=20','【公共关联·本地只读】与路径查询同一功能；DOI包含斜杠时优先用这个接口。GSA没有PMID/DOI时可传identifierType=SOURCE_KEY、identifier=该论文的完整稳定source_record_key（如TITLE:...）。请替换示例唯一值。'))

integration=[

req('01 查询整合表全量重建状态','GET',I+'/status','【公共统计准备·只读本地】返回running、message、processed、total。与快照状态独立。'),

req('02 人工全量重建整合表（不访问上游）','POST',I+'/rebuild','【公共统计准备·本地写入】后台重新读取所有当前注册的真实数据库来源，按PMID/DOI等全局去重更新research_stat_publication；保留既有文献主键，旧数据先标记非活动再重新激活。并非TRUNCATE。用于迁移分流后去除旧OpenLB来源；平常采集变化已增量同步，无需每次快照前执行。构建快照时不允许同时重建。立即返回，轮询本组status。')]

maintenance=[

req('01 查看当前注册的统计数据库','GET',S+'/sources','【公共统计·只读本地】返回来源code/name，供查看可用databaseCode。路由启用且表已创建的库动态接入，OpenLB兜底表不参与新快照；快照不提供勾选数据库选项。'),

req('02 查询快照构建状态（生成前及轮询）','GET',S+'/status','【公共统计·只读本地】running为true时禁用生成按钮；只要有任意BUILDING记录即为运行中。返回current、latestSuccess、message。与整合表重建status独立。'),

req('03 生成新快照（读取本地数据）','POST',S+'/build','【公共统计·本地写入】生成所有注册库各自统计及全局去重总统计；不爬取上游，也不在此重建整合表。name为最多100字的名称；includeRelated=false仅主文献，true纳入纯关联发现文献。已经是主文献且同时有关联标记的记录，在false口径下仍计入。基金统计优先agency；为空时仅以基金编号匹配本地字典full_name，无匹配则舍弃，不额外统计acronym。生成中不允许重复启动。',{'name':'科研统计快照 2026-11-24','includeRelated':False}),

req('04 停止正在生成的快照','POST',S+'/stop','【公共统计】安全停止快照生成，未完成版本不参与成功结果展示，已有成功版本保留。'),

req('05 查询快照版本列表','GET',S,'【公共统计·只读本地】返回最近100条版本，包括成功、失败、停止等状态。取真实id供其他接口路径使用。'),

req('06 查询指定版本元数据','GET',S+'/1','【公共统计·只读本地】路径1替换为真实snapshotId。返回名称、状态、来源、文献数、覆盖计数、开始/完成时间及错误。'),

req('07 查询最近成功版本元数据','GET',S+'/latest','【公共统计·只读本地】返回最近成功版本，不返回构建中半成品；尚无成功版本时可能404。'),

req('08 修改版本名称','PATCH',S+'/1/name','【公共统计·本地写入】路径1替换为真实snapshotId；名称1..100字。不重新计算统计。',{'name':'所有已接入数据库 · 第一版'}),

req('09 删除指定快照及维度穿透关系','DELETE',S+'/1','【公共统计·删除本地版本】路径1替换为要删除的真实snapshotId。删除快照及其维度、穿透关系；保留各库文献和全局整合文献。构建中的版本不能删除。此请求仅在需要时单独调用。')]

total=[

req('01 最近成功版本总统计仪表盘','GET',S+'/latest/result?limitPerDimension=50','【总统计·只读本地】一次返回最近成功版本和总范围图表。总统计文献按全局唯一值去重；主题按来源数据库映射research_topic_category，不使用论文关键词。作者/机构/基金最多已保存Top50；年度及总主题不应按排名截断。'),

req('02 指定版本总统计仪表盘','GET',S+'/1/result?limitPerDimension=50','【总统计·只读本地】路径1替换为snapshotId。返回指定版本总统计；limitPerDimension常用10/20/30/40/50。接口校验1..1000，实际排名最多保存50项。'),

req('03 查询指定总统计维度','GET',S+'/1/dimensions/AUTHOR?limit=50','【总统计·只读本地】snapshotId替换真实值；AUTHOR可换YEAR/TOPIC/INSTITUTION/FUND，也可用内部DATABASE查询各库数量。该接口默认TOTAL范围，不是分库维度查询；分库用databases/{databaseCode}/result。limit=1..1000，常用10..50。')]

databases=[

req('01 查询某快照的数据库总览','GET',S+'/1/databases','【分数据库·只读本地】路径1替换为snapshotId。返回本快照已保存的数据库code、name、publicationCount；来源配置新增后需新快照才形成对应分库结果。'),

req('02 从总主题标签进入所属数据库','GET',S+'/1/topics/SEQUENCE/databases','【总主题→分数据库·只读本地】SEQUENCE替换为research_topic_category中的topic_code。不是数据库code。返回已接入且映射到该主题的库；映射关系从当前配置读取，数据库文献数取该快照。'),

req('03 指定库独立五维统计','GET',S+'/1/databases/GSA/result?limitPerDimension=50','【分数据库·只读本地】snapshotId及GSA替换真实值；各库按自己来源表统计并按自己业务键穿透。分库主题来自关键词/MeSH（与总主题标签口径不同），机构/作者/基金显示Top10..50；年度保持完整。')]

drill=[req('01 图表维度穿透到具体论文（总统计/分库共用）','GET',S+'/1/dimensions/1/publications?page=1&size=10','【公共穿透·只读本地】第一个1为snapshotId，第二个1为图表项返回的真实dimensionId。总图表返回的ID读全局整合文献，分库图表返回的ID读各库来源文献；不要把维度类型或文献ID填成dimensionId。page从1开始、size=1..100。继续查看这些论文的关联文献时，用采集部分“公共关联→分页查询双向关联文献”。外部论文跳转由返回sourceUrl或PMID/DOI生成。')]

collection={'info':{'name':'任务二 · 采集与科研统计全部有效接口 · 2026-11-24','description':'按当前后端Controller整理的Postman Collection v2.1，可导入ApiPost/Postman。所有请求为http://127.0.0.1:8080完整地址，不依赖环境变量。示例ID=1、PMID=41028458须手动换成真实值。\n调用顺序：建表路由→PMID导入/来源盘点→OpenLB分流或GenBase/GSA/BioProject独立主采集→公共关联任务→必要时本地重建整合表→生成快照→轮询状态→总统计/分库统计→文献穿透。\n启动、停止、重置、删除均为真实操作，请按需要逐个调用，不要将整个集合批量执行。后台启动HTTP成功不等于作业完成，需查询对应status。不存在的旧/api/research接口与replay-pending不包含在本文件。\n每个数据库的其他独立官网主采集接口（GVM/OMIX/BioSample/GEN/EWAS等）当前尚未实现；只有建表与路由不会自动从这些官网采集。','schema':'https://schema.getpostman.com/json/collection/v2.1.0/collection.json'},'item':[

folder('01 采集部分','采集器状态各自独立；公共关联采集在主文献入库后另行启动。',[

folder('01 公共 · 关联文献采集与查询',common,relations),folder('02 专用 · OpenLB来源盘点及本地分流','盘点只取cncbBanks；本地分流不访问上游。',banks),folder('03 专用 · OpenLB主文献查漏补缺','只作为补充数据入口，不再作为统计数据库。',openlb),folder('04 专用 · GenBase主文献采集与补全','GenBase支持文章分页与元数据采集，不反查缺失PMID。',genbase),folder('05 专用 · GSA主文献采集与补全','页面全部支持文章统一归入GSA；先执行task2-gsa-collection-20261124.sql。',gsa),folder('06 专用 · BioProject项目文献采集与补全','先执行task2-bioproject-collection-20261124.sql，支持指定项目或官方NGDC列表。',bioproject)]),

folder('02 统计部分','全部已注册真实来源参与；总统计与分数据库统计分别查询。',[

folder('01 公共 · 全局去重整合表', '平常采集后增量同步；人工全量重建作为补救及来源变化后的处理。',integration),folder('02 公共 · 快照生成与版本维护','使用本地数据生成历史版本。',maintenance),folder('03 总统计 · 仪表盘及单维度','TOTAL范围；主题使用数据库标签。',total),folder('04 分数据库 · 总览及独立统计','每库各自统计；主题使用论文关键词/MeSH。',databases),folder('05 公共 · 图表到论文穿透','由维度ID决定总范围或来源库范围。',drill)])]}

out=Path('docs/apipost/task2-all-interfaces-20261124.json');out.write_text(json.dumps(collection,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

# 对照Controller的实际HTTP方法和路径，检查集合未遗漏或添加不存在的端点。

controllers=['OpenLbCollectionController','OpenLbBankInventoryController','GenBaseCollectionController','PublicationRelationCollectionController','ResearchPublicationIntegrationController','ResearchSnapshotController','GsaCollectionController','BioProjectCollectionController']

expected=set()

for name in controllers:

 text=next(Path('portal-backend/src/main/java/com/example/bioplatform/research').rglob(name+'.java')).read_text(encoding='utf-8')

 prefix=re.search(r'@RequestMapping\("([^"]+)"\)',text).group(1)

 for m in re.finditer(r'@(Get|Post|Patch|Delete)Mapping(?:\("([^"]+)"\))?',text):expected.add((m.group(1).upper(),prefix+(m.group(2) or '')))

def flatten(items):

 for i in items:

  if 'item' in i:yield from flatten(i['item'])

  else:yield i['request']

requests=list(flatten(collection['item']));covered=set()

for r in requests:

 path=r['url'].removeprefix(BASE).split('?')[0]

 matches=[(method,route) for method,route in expected if r['method']==method and re.fullmatch(re.sub(r'\{[^}]+\}',r'[^/]+',route),path)]

 if not matches:raise RuntimeError('不存在的端点：'+r['url'])

 covered.update(matches)

if expected-covered:raise RuntimeError('漏掉端点：'+str(expected-covered))

incremental={'info':dict(collection['info'],name='任务二 · GSA与BioProject新增及关联接口 · 2026-11-24'),'item':[folder('01 GSA独立采集','全部支持文章统一归入GSA。',gsa),folder('02 BioProject独立采集','每项目全部出版信息。',bioproject),folder('03 共用关联发现与查询','主采集后按需另行启动；databaseCode可填GSA或BIOPROJECT。',relations)]}

Path('docs/apipost/task2-gsa-bioproject-20261124.json').write_text(json.dumps(incremental,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

print(f'{out}: {len(requests)} requests; {len(expected)} Controller endpoints covered; JSON parsed successfully')


# GSA独立主文献采集（2026-11-24）

## 口径与上游

用户选择采集GSA支持文章页面全部条目，包含HRA、OMIX等其他资源编号，统一归入GSA支持文章来源，不进行CRA筛选。

官方页面 https://ngdc.cncb.ac.cn/gsa/statistics 的js/cra/statistics.js使用 statistics/statistics.json。文章数据为article.publications，字段包括pubmedId、doi、articleTitle、journal、pDate、craAccession、proAccession；两个资源字段可能是HTML、多链接和多编号。当前上游是完整静态JSON，不存在已确认的服务端文章分页接口。页面分页是浏览器本地分页。

本次仅读取页面、官方脚本、HTTP响应头和约70KB统计JSON片段核查字段，未运行整批采集、历史回填或修改真实库。

## 部署准备

1. 执行database/task2-gsa-collection-20261124.sql：保留既有gsa_publication，允许pmid为空，添加专用列表/补全字段及索引；新增gsa_collection_job。
2. 新增字段都有表内注释。gsa_record_key优先PMID，其次DOI，均无则标题SHA256；同一稳定键不重复插入。列表缺少所有标识及标题会报错停止，避免随意合并。
3. GSA原始JSON保存gsa_raw_payload，元数据保存raw_payload；gsa_source_hash只检测列表变化，source_hash检测元数据，二者不混用。
4. application.yml使用完整缓存路径F:/project/bio-portal-suite/docs/gsa-cache，可按机器修改；无环境变量依赖。固定快照只作为文件缓存，不新增文献暂存表。

## 主流程

```text
POST /api/collection/gsa/jobs/start
  → DOWNLOAD：下载官方统计JSON为本作业固定文件
  → IMPORT：Jackson流式遍历article.publications
      → HTML清洗、抽取全部资源号和项目号
      → 分批查已存在文献、按稳定键/PMID/DOI合并
      → 批量写gsa_publication，变化才标PENDING
      → 同事务同步research_stat_publication并推进next_index
  → ENRICH：分批领取本表PENDING元数据
      → 有PMID：OpenLB详情
      → OpenLB没有该文献：PubMed efetch XML
      → 无PMID：保持为空，不通过DOI反查；保留DOI与资源编号用于核对和跳转
      → 都没有：仍保留原文献，标NO_PMID
      → 保存作者各自原始机构，不用项目提交者替代作者
      → 优先原文grants，funding作为编号后备；已有基金不覆盖
      → 基金不足时根据明确项目号补项目基金
      → 批量写作者/机构/基金/主题，随后按稳定来源键增量同步总库
  → COMPLETE / SUCCESS
```

不反查缺失PMID；无法补全会明确标NO_PMID或EMPTY，不丢掉原始论文。没有任何标识时标题键只是保守兜底，无法保证标题改变后仍识别为同一文献。

同篇同作者保留自己的affiliation及原始authors_json，affiliation_info仅为汇总。机构解析和agency优先的基金名称解析仍在公共快照阶段进行，不在每次采集时调用ROR。

## 接口与断点

| 方法 | 地址 | 用途 |
| --- | --- | --- |
| POST | /api/collection/gsa/jobs/start | 首次/停止/失败续做；上次SUCCESS后再调用会新建列表扫描 |
| GET | /api/collection/gsa/status | 运行、当前文献、阶段、列表游标、补全计数及失败原因 |
| POST | /api/collection/gsa/jobs/stop | 安全停止，当前网络调用有25秒读超时 |
| POST | /api/collection/gsa/jobs/restart | 显式放弃旧游标并下载新快照，不删除文献，不强制重查未变元数据 |
| POST | /api/collection/gsa/enrichment/reset | 主动重置本库主文献为待元数据补全，不清空文献；随后start |

start/restart请求体：{"batchSize":100,"minDelayMs":800,"maxAttempts":3}。

下载超时、HTTP429/5xx会降速并有限重试，达到次数停止；message可轮询当前记录和错误。STOPPED/FAILED再次start继续同一作业；IMPORT仍使用同一磁盘文件，避免线上排序变化造成漏采。缓存丢失时禁止拿新线上文件套旧索引，需显式restart。ENRICH通过表内状态续做，成功记录不重查。成功完成后删除本次缓存文件。未完成缓存保留以支持恢复，旧废弃扫描文件可在确认不再恢复后人工清理。

## 关联文献

GSA主采集不自动启动关联作业，主采集完成后调用现有公共入口：

POST /api/collection/publication-relations/jobs/start-pending-batch

{"databaseCode":"GSA","batchSize":100,"includeFailed":false,"maxPublications":1000,"requestDelayMs":800}

公共队列支持GSA的PMID、DOI及SOURCE_KEY根文献。主文献无PMID时，也能按其cncbBanks中的项目号继续找关联论文。新发现论文仍写gsa_publication并标为关联发现；已有主文献保留主身份。关联边按来源和无序业务唯一值去重，不用全局整合表自增ID。

DOI含斜杠或无PMID的论文可通过新增公共查询参数入口查看关联：
GET /api/collection/publication-relations/publications?databaseCode=GSA&identifierType=DOI&identifier=10.xxxx%2Fxxx&page=1&size=20

identifierType=SOURCE_KEY时填完整稳定键，例如TITLE:哈希。停止/恢复/查询状态沿用公共关联接口。遇到未实现/不可用的资源解析仍会记录失败，不能保证所有库动态资源页面都可解析。

## 总库与快照

GsaResearchPublicationSource适配本表，参与总整合、分库五维统计及穿透；缺PMID的DOI/标题键记录也同步总库。原OpenLB分流进GSA的旧记录通过PMID兼容读取，不要求历史回填稳定键。

正常采集按批次增量同步整合表；主采集完成后可生成新快照，无需每次全量重建。是否纳入纯关联文献仍由includeRelated控制。历史快照保持历史值，不会自动变成新口径。

## 文件

- GsaCollectionController / GsaCollectionServiceImpl：独立任务入口和阶段控制。
- GsaCollectionMapper.xml：批量列表UPSERT、元数据更新、短事务持久化断点。
- GsaStatisticsReader / GsaPublicationParser：流式快照及原始文章解析。
- GsaMetadataClient / GsaHttpTransport / PubMedXmlMetadataParser：元数据请求、重试、逐作者机构和基金原始对象提取。
- GsaResearchPublicationSource / GsaSourceMapper：专用统计来源和业务键穿透。
- 前端GsaCollectionPanel：科研统计页的主采集控制面板。
- docs/apipost/task2-gsa-collection-20261124.json：本次新增及公共关联复用接口。
- docs/apipost/task2-all-interfaces-20261124.json：更新后全部45个任务二接口。

最新约束：不反查、不补写来源缺失的PMID。源数据原来提供的PMID照常保存；源数据后续更新原生标识时才随来源更新。资源和文献的统计模式以最新约定为准，不能把无文献资源伪造成文章。

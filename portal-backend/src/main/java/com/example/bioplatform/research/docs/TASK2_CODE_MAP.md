# 任务二代码结构与扩展位置（2026-11-24）

## 先理解三个采集入口

- POST /api/collection/openlb/jobs/start：读openlb_pmid_task的待处理PMID，从OpenLB取主文献，按cncbBanks.db分流到目标库。OpenLB是查漏补缺入口。
- POST /api/collection/genbase/jobs/start：独立读取GenBase支持文章列表，写genbase_publication，再按PMID通过OpenLB补全该表文献。这两个入口没有必须先后顺序，可以分别执行。
- POST /api/collection/publication-relations/jobs/start-pending-batch：所有接入数据库共用的关联采集入口，databaseCode决定处理哪个来源表、关联论文写到哪个来源表。主采集不会自动启动关联作业。

## 实际代码树

根目录：F:/project/bio-portal-suite/portal-backend/src/main/

```text
java/com/example/bioplatform/
├── controller/
│   ├── OpenLbCollectionController.java               [专用] PMID任务导入、OpenLB主采集、状态/停止/重试
│   ├── OpenLbBankInventoryController.java            [专用] cncbBanks盘点、本地历史文献分流
│   ├── GenBaseCollectionController.java              [专用] GenBase列表采集、PMID补全及状态控制
│   ├── PublicationRelationCollectionController.java [公共] 指定库关联作业、恢复、状态、关联文献查询
│   ├── ResearchPublicationIntegrationController.java [公共] 全局去重整合表重建与状态
│   └── ResearchSnapshotController.java               [公共] 快照、总统计、分库统计、文献穿透
├── service/
│   ├── ResearchPublicationSource.java                [扩展接口] 每个统计来源的读取、分页、计数、业务键查询
│   ├── RelatedResourceResolver.java                  [扩展接口] 每种资源链接如何提取关联PMID及基金
│   └── impl/
│       ├── OpenLbCollectionServiceImpl.java          [专用采集] HTTP取PMID详情，批量持久化及同步整合表
│       ├── OpenLbBankInventoryService.java           [专用采集] 仅盘点cncbBanks，独立持久化游标
│       ├── OpenLbRoutingService.java                 [分流] 按openlb_bank_route写目标表；未配置/空关联存OpenLB
│       ├── GenBaseCollectionServiceImpl.java         [专用采集] 支持文章分页及OpenLB PMID补全
│       ├── PublicationRelationCollectionServiceImpl.java [公共关联] 队列、按节点递归发现、正文补全、关系去重
│       ├── BioCodeRelatedResourceResolver.java       [专用资源解析] 工具API→publications/grants
│       ├── DatabaseCommonsRelatedResourceResolver.java [专用资源解析] dbId引用API→PMID/funding
│       ├── BioProjectRelatedResourceResolver.java    [专用资源解析] 项目API→listPublication/listGrants
│       ├── GsaRelatedResourceResolver.java           [专用资源解析] CRA搜索详情→BioProject→文献/基金
│       ├── GwhRelatedResourceResolver.java           [专用资源解析] Assembly详情/接口→论文链接及项目
│       ├── AscancerRelatedResourceResolver.java      [专用资源解析] knowledge分页接口；当前线上返回404
│       ├── RelatedResourceHttpClient.java            [公共辅助] 新解析器共享限速、超时、有限缓存、异常反馈
│       ├── ResourcePublicationParser.java            [公共辅助] 明确论文链接、项目链接提取，不拿普通数字当PMID
│       ├── BioProjectMetadataClient.java             [辅助] 原有采集阶段的BioProject补全客户端
│       ├── ResearchPublicationSourceRegistry.java   [公共注册] Spring来源适配器+启用的路由配置
│       ├── RoutedResearchPublicationSource.java     [通用来源适配] 与OpenLB表结构相同的目标库
│       ├── GenBaseResearchPublicationSource.java    [专用来源适配] GenBase原生字段/业务键
│       ├── OpenLbResearchPublicationSource.java     [停用统计来源] enabled=false，OpenLB兜底表不参与统计
│       ├── ResearchPublicationIntegrationServiceImpl.java [公共整合] 变化文献增量同步、全局去重、人工全量重建
│       └── ResearchSnapshotServiceImpl.java          [公共统计] 批量读各来源、构建快照及Top50/穿透
├── mapper/
│   ├── OpenLbCollectionMapper.java                   [专用SQL] PMID任务、OpenLB兜底文献
│   ├── OpenLbRoutingMapper.java                      [分流SQL] 盘点、路由读取、按目标库批量写入
│   ├── GenBaseCollectionMapper.java                  [专用SQL] GenBase主采集及PMID补全状态
│   ├── PublicationRelationCollectionMapper.java     [公共SQL] 关联作业/队列、当前库文献、双向关系
│   ├── RoutedPublicationMapper.java                  [公共SQL] 同结构目标表的分页/计数/业务键读取
│   └── ResearchSnapshotMapper.java                   [公共SQL] 整合、快照、候选聚合及穿透
└── common/
    ├── GrantPayloads.java                            [公共辅助] 原始grants保留、funding编号包装与合并
    └── ResearchPublicationJsonMerger.java             [公共辅助] 多来源文献JSON合并
resources/mapper/                                     复杂SQL对应XML；部分Mapper使用注解SQL
```

## 关联写入的正确调用

```http
POST http://127.0.0.1:8080/api/collection/publication-relations/jobs/start-pending-batch
Content-Type: application/json

{
  "databaseCode": "GSA",
  "batchSize": 100,
  "includeFailed": false,
  "maxPublications": 1000,
  "requestDelayMs": 800
}
```

请求只选择一个库。databaseCode使用source_code，不是显示名：GSA、GWH、BIOCODE、BIOPROJECT、DATABASECOMMONS、ASCANCERATLAS、GENBASE等。

```text
选择GSA
  → gsa_publication中领取PENDING
  → 读取每篇文章的cncbBanks
  → 按链接db选择RelatedResourceResolver
  → 找到PMID后用OpenLB补全相关论文
  → 写入gsa_publication（标记为关联发现文献）
  → research_publication_relation保存database_code=GSA及两端唯一值
  → 将新发现文献加入持久化队列继续发现
```

注意：OpenLB主文献分流按cncbBanks.db选择所属库；关联采集按当前job.databaseCode保留发现来源。因此GSA作业即使遇到BioCode链接，新发现的关联论文仍保存在GSA表并标记关联发现，不会因为资源解析器是BioCode就改存BioCode表。关联文献不必本来就属于GSA；默认includeRelated=false的统计不计纯关联文献。

每次batchSize仅领取一批根文献；maxPublications限制本次处理节点数，不是整个库上限。返回PAUSED时用resume继续原作业；作业完成后再次调用start-pending-batch领取后续根记录。当前只有一个公共关联作业运行槽，各库关联作业依次启动。

状态：GET /api/collection/publication-relations/jobs/status?jobId=实际作业ID
停止：POST /api/collection/publication-relations/jobs/stop
恢复：POST /api/collection/publication-relations/jobs/实际作业ID/resume
失败来源重领：start-pending-batch传includeFailed=true

## 哪些已实现，哪些只是框架

| 能力 | 当前实现                                      |
| --- |-------------------------------------------|
| 从OpenLB按PMID补缺并分流到各库 | 已实现，依赖已建目标表和路由                            |
| GenBase官网支持文章列表独立采集 | 已实现，其后可用OpenLB补全                          |
| GSA官网支持文章全列表采集 | 已实现独立GsaCollectionController，详见TASK2_GSA_COLLECTION.md |
| BioProject官网项目出版文献独立采集 | 已实现BioProjectCollectionController，支持全部项目或指定编号，详见TASK2_GSA_BIOPROJECT_WORKFLOW.md |
| GWH等其他官网独立主文献采集 | 尚未建立独立主采集器；资源详情解析不等于全量主采集 |
| BioCode/Database Commons/BioProject/GSA/GWH资源详情关联解析 | 已有专用解析器；并不代表官网全量主采集已实现                    |
| ASCancerAtlas资源关联 | 已写接口适配，当前官方knowledge接口404，任务会报错停止，尚未验证可取数 |
| GVM/OMIX/BioSample/GEN/EWAS资源专用关联解析 | 尚无专用解析器，仅有通用页面回退，不能保证得到全部关联论文             |
| 对已配置同结构目标库统计、整合、穿透 | 公共适配器支持；前提是该库表有数据                         |

## 新增数据库时改哪里

1. 只想接收OpenLB补缺：创建同结构xxx_publication、配置openlb_bank_route；配置research_database_topic以参与总主题标签。
2. 需要从该官网完整采集主文献：新增对应Controller/Service/Mapper与持久化任务进度，写本库表后调用公共syncIdentifiers同步整合表。
3. 需要支持一种新资源链接：新增RelatedResourceResolver的Spring组件，实现db别名及PMID/基金提取；不需要给每个库再建关联Controller。
4. 新库字段和唯一值与模板不同：新增ResearchPublicationSource适配器；若关联队列读取/写入也不同，补PublicationRelationCollectionMapper的专用分支，不能只靠路由套模板。
5. 新库进入统计无需改快照聚合算法；注册来源后生成新快照。旧快照保留原范围。

接口总JSON：docs/apipost/task2-all-interfaces-20261124.json。


2026-11-24新增GSA独立主采集：GsaCollectionController → GsaCollectionServiceImpl → GsaCollectionMapper；GsaStatisticsReader流式导入固定快照，GsaMetadataClient补全元数据，GsaResearchPublicationSource接入公共统计。详见同目录TASK2_GSA_COLLECTION.md。


2026-11-24补充BioProject独立主采集、指定项目范围、全部出版信息导入、PMID优先与页面回退、无PMID稳定键关联。GSA与BioProject完整流程、字段和部署顺序详见同目录TASK2_GSA_BIOPROJECT_WORKFLOW.md。

# 任务二：采集、整理、快照

## 运行顺序

1. 原采集接口仍写各数据库自己的原始文献表，OpenLB按`openlb_bank_route`分流。关联采集仍维护`research_publication_relation`。不需要为了整理重新采集。
2. `POST /api/research-preparation/start`，首次body可用`{"sourceCodes":[],"batchSize":200}`。空来源表示全部启用来源；也可以指定`["GSA"]`等已注册编码。这里只读本地来源适配器，不请求上游。
3. `GET /api/research-preparation/status`观察任务。`canBuild=true`才可生成新快照；初始化时所有已启用来源至少要整理一次，空表也算已检查。
4. 使用原`POST /api/research-snapshots/build`生成新快照。前端按钮与原图表接口保留，总统计和分库入口仍分开。

导入同目录`task2-preparation.postman_collection.json`可查看接口用途和请求样例。不要整集合运行；采集、停止、丢弃、删除是不同场景的操作。

## 包结构

```text
research/
  collection/                         原采集流程
    openlb/                            PMID任务、cncbBanks分流
    genbase/ gsa/ bioproject/           原独立采集器
    common/service/SourceMembership    显式数据库归属判定
    common/service/relation/           原关联发现、写入和边关系
  preparation/                         新增：独立本地整理
    controller/PreparationController   整理、状态、停止、续传、丢弃、重建
    service/PreparationService         每批事务、来源/行游标、版本发布
    service/PreparedMetadataParser     作者机构、基金、关键词的纯解析
    model/                             任务、整理文献事实、成员结构
    mapper/PreparationMapper           SQL统一位于PreparationMapper.xml
  statistics/prepared/                 新增：默认快照实现
    service/PreparedSnapshotService    统一算法，TOTAL或单来源过滤
    mapper/PreparedStatisticsMapper    SQL统一位于PreparedStatisticsMapper.xml
  statistics/service/impl/             旧快照实现留作参考，已取消Spring注册
```

新来源只需注册适配器/分流配置并创建其原始表。整理、快照不维护固定数据库名单，不自动建原始表。当前实际启用了12个来源，并非要求截图中的六个。

## 表与字段

| 表 | 作用 |
| --- | --- |
| `research_preparation_job` | 整理任务及断点；`source_codes_json`冻结来源顺序，`source_index`为当前来源下标，`after_id`为来源适配器游标，`batch_size`最多200，`rows_scanned`为提交扫描量，`rows_changed`为新增/替换文献版本量。`running_slot`生成列防止同时创建两个RUNNING任务。 |
| `research_prepared_document` | 一条来源文献的整理版本和跳转摘要，不是作者表。`source_code + source_hash`标识同来源业务记录；`source_key`保留原库业务键；`document_key`提供PMID/DOI/来源业务键身份；`primary_flag`是相对于此来源的本库归属；`record_kind`区分文献和无文献资源。 |
| `research_prepared_author` | 一篇来源文献的一位作者与一个机构。多机构作者分多行；保留`author_order`和空机构。只按姓名机构署名组合统计，不宣称完成真实人物消歧。 |
| `research_prepared_institution` | 文献涉及的机构独立去重。多位作者同机构不增加该机构的文献数；两家机构分别计一篇。 |
| `research_prepared_fund` | 一篇文献的一个资助方。优先agency；缺失时按grantId在本地基金字典查全称，找不到舍弃。不把acronym当基金项目。 |
| `research_prepared_topic` | 文献关键词和MeSH去重成员，用于分数据库主题统计。复用已有通用主题清洗逻辑。 |
| `research_prepared_year` | 已知发表年份关系；从文献元数据生成，年度查询不经过作者或基金。 |
| `research_prepared_identity` | 每个成功整理版本的DOI→PMID无歧义映射。只有同DOI恰好对应一个PMID才建立桥接；冲突PMID不强行合并。 |
| `research_prepared_snapshot` | 快照绑定的整理版本、来源名称和主题映射。只存小规模配置，不复制文献/作者关系。 |
| `research_stat_snapshot / research_stat_dimension` | 复用现有版本元数据和聚合结果。新版不再写candidate或dimension_publication。 |

每个成员表都有`document_id`（来源文献版本）、`source_code`、`document_key`、`pmid`、`doi`、`source_url`，可直接分页穿透。`member_key`是规范名称摘要，只在文献版本内唯一；不同来源同篇文献允许各自保留关系。

文献事实的`valid_from / valid_until`是整理版本的半开区间，`NULL valid_until`表示当前有效。成员随文献版本保持不可变。`content_hash`包含文献展示字段及解析结果，不包含任务号和游标；重复整理未变化数据只更新`seen_run`，不复制所有成员。

## 统计口径

- 分库：先按来源过滤，再按规范文献身份去重。同一篇文章在A、B两库可以各计一次。
- 总量：跨库按PMID去重；无PMID按规范DOI及无歧义桥接；再缺失则按来源和自身业务键，避免把不同数据库无标识资源混为一条。
- 作者、机构、基金、关键词：分别查询自己的成员表，`COUNT(DISTINCT 文献身份)`。不把五张成员表一起JOIN，不生成作者×基金×机构的组合。
- 年份：同一统计范围内同文献存在多个年份时，取最早已知年份；年度合计不会把同文献重复放进不同年份。
- 本库与关联：所有记录都整理。本库记录即使同时被关联发现，关闭`includeRelated`也计入；非本库记录只有开启时才计入。一个PMID在B库是主记录，不会因此把它在A库的关联记录也判为A库主记录。
- 无文献资源：`includeResources=true`才加入，按来源和原库业务键；已有文献的资源由原资源适配器排除，不多算资源本身。
- 总统计主题保留原业务口径：固定`research_topic_category`标签全部列出，包括0；标签值为其所属各数据库自身去重数量之和，因此不同标签/来源可以重复涉及同一文献，这不是全局文献总量。分库主题来自关键词/MeSH。
- 排名在数据库端取前50，内存只保留单批原始记录和小规模排名。穿透读取生成时冻结的整理版本，后续更新不会改变旧快照结果。主题穿透同其按来源累计的标签口径一致。

## 更新、停止和断点

- 成功后原库有更新：再次`start`扫描选中来源，跳过未变化记录，替换变化记录的完整成员集合；源记录已删除时在该来源扫描结束后关闭其旧版本。
- 失败/停止：不换来源直接`start`或`retry`，沿用同一任务ID、来源顺序和已提交游标。失败批次的成员和游标一同回滚。
- 进程退出遗留RUNNING：确认旧进程已停止后`recover`，再`start`。不要对仍在其他实例运行的任务调用恢复接口。
- 取消未完成整理：先停止，再`discard`，恢复之前发布的整理版本。原始数据和成功快照保留。
- 修改解析规则后：`rebuild`强制为选中来源生成新成员版本。不是TRUNCATE，旧快照依赖的版本保留。
- 整理失败不会发布readyRevision，也不会允许生成依赖半成品的新快照；已成功快照仍可读取。
- 采集与整理有意分开：建议原采集结束后整理。整理期间原表再次更新的内容，下一次整理会同步；不会把当前扫描伪装成原表全局一致性事务。

## SQL与兼容性

依次执行`database/task2-preparation-20261126.sql`、`database/task2-gsa-pmid-nullable-20261126.sql`。本次已在当前本地数据库执行：新增空整理表；只修改GSA的PMID空值约束。未执行历史整理或上游采集。

原文献表、旧整合表和关联边表保留，原采集继续同步旧整合表以兼容其他用途。新快照不从它们拆JSON；只有无文献资源通过原资源适配器在整理阶段读取。旧格式快照保留，但不作为新版可回放版本，首次需要整理后生成新快照。

旧`research_author_*`残留表不读取、不删除。任务一Java/前端逻辑不改；H2演示仅追加独立整理表结构，以保持原任务一集成检查可运行。

历史整理版本占用持久化空间，这是保持旧快照穿透不变的代价；本次不自动删除仍可能被旧快照使用的版本。后续可按快照保留期增加安全清理任务。

## 验证范围

验证包含干净编译、前端构建、模块边界、OpenLB分流回归、纯解析测试和隔离MySQL端到端测试。隔离测试只创建随机`codex_prepared_test_*`库，结束删除，不向业务文献表写测试记录。真实万级/十五万级全量整理与快照耗时没有在业务库执行，不能以小样本耗时代替生产性能结论。


## 增量整理（2026-11-26追加）

新增 `POST /api/research-preparation/incremental`，body与start相同。start仍是完整扫描，rebuild仍是强制重建；stop、retry、recover、discard、status全部共用，不另建一套任务控制接口。

- 每个来源保存独立的update_time/id游标。当前所有注册来源及无文献资源已接入；未来适配器可实现supportsIncremental和loadChangedRecordsPage，不支持者会明确以FULL模式扫描。
- 首次/升级后自动建立一次基线，复用原有未变化成员，同时补齐来源物理行ID。后续仅查询窗口内变化记录，不扫描全部原文献JSON。
- 新表research_preparation_source_cursor保存每个任务每库的from_time、until_time、after_update_time、after_id与状态。只有SUCCESS任务的COMPLETE游标才作为下一次基线，失败或discard不会推进已发布增量位置。
- from_time是上一成功上界减默认5秒的回看区间，until_time是本次开始时数据库时间的整秒上界（不含）。时间相同则按ID严格递增分页；当前秒及本轮之后提交的变化留给下一轮。5秒内少量记录可能重读，摘要相同不重建成员。可通过app.research-preparation.overlap-seconds调整。
- 整理文献事实新增source_row_id（无文献资源包装器使用原ID的奇偶编码）。同一物理行的业务键变化时关闭旧记录，不保留两份活动成员；该元数据不参与内容摘要。
- 增量不执行expireMissing；没读到某条记录不代表被删除。无文献资源的is_active停用/恢复可以同步；物理DELETE/TRUNCATE需使用start完整校验。
- 原表写入需正常维护update_time。手动回改时间、超过回看区间才提交的长事务不能靠时间游标保证捕获；建议采集事务完成后再整理，必要时完整扫描补偿。基金字典、主题解析规则、归属路由配置变更不一定更新原表时间，也需完整扫描/重建。
- 整理过程中失败会回滚当前批的成员和游标，继续同一冻结窗口。来源不变时同一body可重试；切换来源或模式需要先完成或丢弃当前任务。
- 新快照仍只读取成功整理版本，历史快照和未选择来源不被覆盖。

现有安装执行database/task2-preparation-incremental-20261126.sql及database/task2-preparation-incremental-indexes-20261126.sql。全新安装的基础DDL已包含新字段和游标表，不需重复执行ALTER列语句。索引脚本针对当前启用来源；新增来源请增加对应的(update_time,id)索引。无文献资源有(record_kind,source_codes,update_time,id)索引。

独立整理接口JSON现在位于docs/apipost/task2-preparation-20261126.json，可直接导入Apipost/Postman；原总集合的整理分组也已更新。

关联采集已调整为单层：只展开输入根文献的Publications，新发现记录只保存及建立边关系，以SUCCESS叶节点登记去重，不加入下一层待采队列。批次显式选择的根文献仍逐条处理；旧作业恢复时结束其遗留深层待采节点。关联文献仍只写当前作业库，不按其cncbBanks向其他来源表分流。


## 数据库关系图与字段注释

完整功能流程图、逻辑关系图、字段含义及两种穿透路径见 [PREPARATION_TABLE_RELATIONS.md](PREPARATION_TABLE_RELATIONS.md)。已有库的字段注释脚本为 `database/task2-preparation-comments-20261126.sql`；建议整理任务结束后执行。

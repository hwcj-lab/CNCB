# 科研产出统计与整合表设计

更新时间：2026-11-19

## 核心数据流

```text
OpenLB / GenBase 来源表
        │ 采集新增或更新后，按 PMID / DOI / 来源记录号同步受影响记录
        ▼
research_stat_publication（全局唯一文献整合表）
        │ 创建快照时仅读取，不再重做跨库去重或写入整合表
        ▼
research_stat_snapshot → research_stat_dimension → research_stat_dimension_publication
```

关联文献使用 `research_publication_relation` 保存数据库代码和两端的标识类型、标识值。关系按无方向的标识对去重；例如 A→B 和 B→A 归为同一条关系。统计穿透关系使用`research_stat_dimension_publication.publication_key`：总统计存全局文献键，分库统计存来源表自己的记录键。读取分库穿透时按维度`scope_code`选择对应来源适配器，再按来源键回查原始记录；不会把分库计数映射到另一库或仅映射到跨库合并行。关联文献详细记录仍留在各来源表中。

## 来源采集与整合表同步

- OpenLB、GenBase 的采集流程以各自的唯一键做批量 upsert。来源摘要未变时不覆盖字段；摘要变化时更新该来源记录。
- 来源记录批次写入后，采集服务将本批标识传给整合服务。整合服务按 PMID、DOI 查找跨来源的同篇记录，批量合并后写入 `research_stat_publication`。
- 整合表使用 `publication_key` 唯一约束：优先 `PMID:{pmid}`，其次规范化的 `DOI:{doi}`，再退化为 `{来源代码}:{来源记录标识}`。
- 新唯一键插入一行；已有唯一键且内容摘要相同，不改动内容和更新时间；摘要变化时更新该行。现有整合行 ID 保持不变。
- 数据库来源/关联标识改变导致旧键不再出现时，增量同步无法自动判断是否应删除旧身份；全量重建会对来源当前记录重新归并，并将不再出现的整合行设为 `is_active=0`，不物理删除，避免历史快照失去引用。
- 同一来源表更新一条或一批记录时只同步受影响的标识；整库采集仍按采集页/批次同步，不逐条提交。

## 全量重建与快照的区别

### 全局整合表全量重建

入口：`POST /api/research-publications/integration/rebuild`。后台读取所有已注册来源的当前记录，在事务中先把旧整合行标记为非活动，再按全局唯一键批量 upsert：

1. 唯一键已存在、内容没变化：不重复插入，不改内容和 `update_time`，原 ID 保持不变，并恢复为活动。
2. 唯一键已存在、内容发生变化：更新该行内容和摘要，原 ID 保持不变。
3. 唯一键不存在：插入新行。
4. 来源中已不存在的旧键：保留该行并标记 `is_active=0`，供旧快照关系继续引用。

因此这里的“全量”指完整重读所有来源并校准整合表，不是删除再插入，也不会让同一文献产生重复行。新数据不会在事务提交前暴露为半完成状态。任务执行期间禁止开始新快照；状态可通过 `GET /api/research-publications/integration/status` 查询。

### 创建统计快照

快照只读取 `research_stat_publication` 生成总统计维度，并读取各来源表生成分库维度；只写本次快照的版本、维度和有限的穿透关系。它不会向 `research_stat_publication` 重新合并或插入文献。若采集已完成但整合表尚未同步，应先执行整合重建，再生成快照。

总统计范围为 `scope_code='TOTAL'`；分库统计使用对应来源代码。`research_stat_dimension` 只保留一个 `scope_code`，不再使用冗余 `scope_type`。维度类型为 YEAR、TOPIC、AUTHOR、INSTITUTION、FUND，另有用于总览分库数量的 DATABASE 展示维度。每个维度的文献穿透都保存在同一张关系表中，不建立 `SNAPSHOT_ALL` 伪维度，不为总统计逐篇保存一份“快照全量文献关系”。

年度维度保留全部年份；主题、作者、机构和基金在总范围及各分库范围分别保留前50项。关系行只对应这些实际统计维度的成员唯一值；分库维度按该库记录键建立关系，所以`publication_count`与穿透关系数量一致。前端选择展示 Top 10/20/30/40/50 不会再扩大存储量。

## 唯一键和主题口径

- 全局去重优先 PMID；无 PMID 时使用规范化 DOI；两者都没有时按来源记录键区分，不依据标题猜测同篇。
- `research_stat_publication` 的主题从真实采集字段 `keyword_list` 和 `mesh_headings_json` 提取，不从数据库名称推断主题。
- 作者以“作者姓名 + 该作者自己的署名机构”作为维度；作者 JSON 未提供对应机构时记录为机构未提供。
- 机构、基金暂按原始文本做保守清理，没有完成权威名称归一化；报表不代表已完成别名消歧。
- 关联发现本身不是论文统计维度。关联论文是否计入快照由 `include_related` 口径控制；被选中的记录仍需有可解析唯一标识，并在全局表中只存一行。

## 扩展新文献来源

1. 新增来源专属采集表、Mapper 和采集流程。
2. 实现 `ResearchPublicationSource` 以批量读取该来源记录。
3. 来源写入后调用 `ResearchPublicationIntegrationService`，传入本批 PMID、DOI 或来源记录标识。
4. 完整重建会自动发现注册来源；快照服务不需要为新来源增加硬编码分支。

## 相关接口

- `GET /api/research-publications/integration/status`：查询整合任务状态。
- `POST /api/research-publications/integration/rebuild`：启动整合表全量重建。
- `GET /api/research-snapshots/status`：快照状态。
- `POST /api/research-snapshots/build`：新建统计快照。
- `GET /api/research-snapshots/{id}/result?limitPerDimension=50`：读取指定版本的总统计。
- `GET /api/research-snapshots/{id}/databases/{databaseCode}/result?limitPerDimension=50`：读取指定版本的单库统计。
- `GET /api/research-snapshots/{id}/dimensions/{dimensionId}/publications`：按统计维度分页穿透文献。
- `DELETE /api/research-snapshots/{id}`：删除非构建中的快照；统计维度及其穿透行由外键级联删除，整合表文献保留。

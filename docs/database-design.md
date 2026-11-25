# 当前数据库设计

更新日期：2026-11-16。正式环境使用MySQL 8。

## 核心表（13张）

| 模块 | 表 | 职责 |
|---|---|---|
| 资源推荐 | `data_type` | 可扩展的数据类型 |
| 资源推荐 | `database_resource` | 数据库目录 |
| 资源推荐 | `online_tool` | 工具和跳转入口 |
| 资源推荐 | `database_data_type` | 数据库多类型标签 |
| 资源推荐 | `database_tool` | 数据库推荐工具 |
| OpenLB采集 | `openlb_pmid_task` | PMID去重、断点、重试状态 |
| OpenLB采集 | `openlb_publication` | OpenLB原始响应解析字段和原文 |
| GenBase采集 | `genbase_collection_job` | 分页偏移量和作业状态 |
| GenBase采集 | `genbase_publication` | GenBase记录及OpenLB补全字段 |
| 统计 | `research_stat_snapshot` | 名称、版本、状态、所选来源、覆盖率 |
| 统计 | `research_stat_publication` | 跨来源全局去重文献；一篇文献只保存一次 |
| 统计 | `research_stat_dimension` | 年度、主题、作者、机构、基金聚合 |
| 统计 | `research_stat_dimension_publication` | 统计维度到范围内文献业务唯一值的穿透关系 |
| 关联文献 | `research_publication_relation` | 按来源库和两端唯一标识保存去重后的关联边，不复制关联文献全文 |

旧版 `publication`、`author`、`institution`、`fund_project` 及其关系表已退出代码路径，
不应为了旧接口重新创建。

## 快照状态

- `BUILDING`：正在读取来源或批量写入。
- `SUCCESS`：完整版本，可供页面读取。
- `FAILED`：发生异常或应用中断，不参与默认展示。
- `STOPPED`：用户主动停止，不参与默认展示。

页面通过 `status='SUCCESS' ORDER BY version_no DESC LIMIT 1` 读取最近完整版本。

## 去重

1. 有PMID：使用 `PMID:{pmid}`。
2. 无PMID但有DOI：使用规范化 `DOI:{doi}`。
3. 两者都没有：使用 `{来源}:{来源记录ID}`，不按标题猜测合并。

`research_stat_publication` 在 `publication_key` 上全局唯一。来源采集新增或更新后，采集流程按受影响的标识
批量同步整合行：新键插入，摘要相同不更新，摘要变化才更新。创建统计快照只读此整合表，不再做跨库归并。
手工全量重建会重读所有来源，在事务内按唯一键 upsert；不存在的旧行标记为 `is_active=0`，不物理删除，
使历史快照中的穿透外键仍有效。相同键保留整合行ID，未变化时内容和更新时间不变。

## 维度索引

`dimension_key` 保留可解释的归一化文本；`dimension_hash` 是SHA-256生成列。
唯一索引使用 `(snapshot_id, dimension_hash)`，避免对最长700字符文本反复维护大型唯一索引。
原始展示文本仍保存在 `display_name`。

`research_stat_dimension_publication`使用`(dimension_id, publication_key)`联合主键，不再引用全局文献数值ID。
总统计维度保存全局`publication_key`；分库维度保存对应来源的`source_record_key`，并由维度的`scope_code`
确定唯一值应从哪个来源表读取。删除快照时，外键只级联清理该快照的维度及穿透关系；穿透表不再约束来源文献表或全局整合表，可独立维护文献数据。

`research_stat_dimension.scope_code`区分统计范围：总统计使用`TOTAL`，分数据库统计使用来源代码。
来源代码由Spring自动注册的
`ResearchPublicationSource`适配器提供，快照构建器不维护数据库名称白名单。新增或移除来源适配器后，
新快照自动反映变化，历史快照继续保留生成时的来源代码。

快照不写 `research_stat_publication`。`research_stat_dimension_publication` 只存实际统计维度成员的关系，
不创建 `SNAPSHOT_ALL` 维度来逐篇关联全部文献。

## 迁移顺序

- `V10__add_research_stat_snapshots.sql`：快照基础表。
- `V11__fix_snapshot_dimension_key_collation.sql`：统一应用键与MySQL唯一性口径。
- `V12__optimize_snapshot_dimension_unique_index.sql`：固定长度哈希唯一索引。
- `V13__add_snapshot_dimension_publication.sql`：图表文献穿透关系与快照级联删除。
- `V14__add_snapshot_name.sql`：增加可修改的快照展示名称。
- `V15__structure_author_institution.sql`：结构化保存作者/机构，并在快照文献中保留作者、机构与基金原始结构。
- `V18__bind_snapshot_drillthrough_by_business_key.sql`：将维度—文献关系由全局文献数值ID改为统计范围内的文献业务唯一键。
- `V19__fix_dimension_publication_key_comment.sql`：修复Windows终端执行V18时可能造成的中文字段注释编码问题。
- `task2-reset-global-statistics.sql`：人工执行的一次性重建脚本，将快照文献改为全局唯一；会删除旧快照但保留来源采集数据。

`research_stat_publication.authors_json`保存一篇文章的完整作者数组及每位作者自己的`affiliation`；`affiliation_info`保存去重后的文章机构摘要；`grants_json`保存基金原始结构。不能使用普通逗号拆作者或机构，因为机构名称本身包含大量逗号。

AUTHOR类型的`research_stat_dimension`记录同时保存`author_name`和`institution_name`，`display_name`只用于展示。同名作者在不同机构形成不同维度；作者自己的机构为空时`institution_name`保持NULL，不把第一作者机构错误分配给其他作者。

除年度维度外，新快照只持久化主题、机构、作者和基金各自按文献数排序的前50项，以及这些项的文献穿透关系。这样仍可用规范化关系表分页、索引和级联删除，同时控制关系数量；不使用逗号拼接文献ID。

已有数据库按顺序只执行未执行的迁移，不重复执行完整 `schema.sql`。

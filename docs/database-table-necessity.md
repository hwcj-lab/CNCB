# 当前数据库表用途与保留建议

整理日期：2026-11-16

本文依据当前后端 Mapper、Controller 和前端实际调用整理，不仅依据表名判断。

## 一、当前核心必需表

### 1. 数据资源与工具推荐（5张）

| 表名 | 用途 | 当前结论 |
|---|---|---|
| `data_type` | 保存DNA、蛋白质、文献、物种及以后新增的数据类型 | 必需 |
| `database_resource` | 保存CNCB及合作方数据库目录 | 必需 |
| `online_tool` | 保存在线工具和跳转地址 | 必需 |
| `database_data_type` | 数据库与数据类型多对多标签 | 必需 |
| `database_tool` | 数据库与推荐工具多对多关系 | 必需 |

这5张表直接支撑任务一页面，不能删除。

### 2. 原始文献采集（4张）

| 表名 | 用途 | 当前结论 |
|---|---|---|
| `openlb_publication` | OpenLB原始文献及作者、机构、基金、主题等原始字段 | 必需 |
| `openlb_pmid_task` | OpenLB的PMID去重、断点、失败重试状态 | 必需 |
| `genbase_publication` | GenBase支持文章及OpenLB补全结果 | 必需 |
| `genbase_collection_job` | GenBase分页偏移量、断点和作业状态 | 必需 |

原始表和任务表职责不同。任务表不能用原始文献表替代，否则中断后无法可靠恢复，也无法区分“未处理、空结果和失败”。

### 3. 新版统计快照（3张，粘贴SQL中缺失）

| 表名 | 用途 | 当前结论 |
|---|---|---|
| `research_stat_snapshot` | 一次统计的版本、状态、来源和字段覆盖率 | 必需 |
| `research_stat_publication` | 跨来源全局去重后的文献主表；内容不变不重复写入 | 必需 |
| `research_stat_dimension` | 年度、主题、作者、机构、基金等预聚合结果 | 必需 |

建表脚本：`database/migrations/V10__add_research_stat_snapshots.sql`。

这3张表解决两个问题：页面不必每次扫描原始JSON；每次更新生成新版本，可解释历史报表变化。

## 二、已退出运行链路的旧版统计表

| 表名 | 当前用途 | 建议 |
|---|---|---|
| `publication` | 旧 `/api/research/*` 的文献主表 | 不再创建 |
| `publication_database` | 旧统计的文献—数据库关系 | 不再创建 |
| `author` | 旧作者排行 | 不再创建 |
| `institution` | 旧机构排行 | 不再创建 |
| `fund_project` | 旧基金排行 | 不再创建 |
| `publication_author` | 旧文献—作者关系 | 不再创建 |
| `publication_institution` | 旧文献—机构关系 | 不再创建 |
| `publication_fund` | 旧文献—基金关系 | 不再创建 |

科研分析前端已经切换到 `/api/research-snapshots/latest/result`，旧Controller、Service、Mapper和实体已经删除。这8张表不再属于当前运行结构；已有数据库若存在历史数据，先归档再决定是否删除。

它们不是OpenLB、GenBase采集所必需，也不是新版统计快照所必需。

## 三、可暂缓或可移除的表

| 表名 | 原设计 | 代码现状 | 建议 |
|---|---|---|---|
| `publication_author_institution` | 精确表达某作者在某篇论文中的所属机构 | 当前没有Mapper查询、写入或接口使用 | 新环境不必创建；已有表先保留，确认无历史数据后再删除 |
| `source_endpoint` | 展示上游接口来源和验证信息 | 仅 `/api/research/sources` 查询使用 | 如果页面保留“数据来源”按钮则保留，否则可在移除接口后删除 |

`publication_author_institution` 的模型本身并非错误，但目前项目没有作者—机构精确映射流程，建表后也不会产生有效数据，所以当前没有必要。

## 四、建议的最终结构

最终建议保留12张核心表：

1. 任务一5张：`data_type`、`database_resource`、`online_tool`、`database_data_type`、`database_tool`。
2. 采集4张：`openlb_publication`、`openlb_pmid_task`、`genbase_publication`、`genbase_collection_job`。
3. 快照3张：`research_stat_snapshot`、`research_stat_publication`、`research_stat_dimension`。

`source_endpoint` 是否保留取决于是否继续显示来源说明。旧版规范化统计8张表在前端迁移完成前保留，迁移完成后再退役。

## 五、当前粘贴SQL需要注意的问题

1. 它缺少新版统计快照3张表，执行后无法使用统计快照接口。
2. `CREATE TABLE IF NOT EXISTS` 后单独执行 `CREATE INDEX` 并不完全幂等；第二次执行可能因索引已存在而失败。
3. 时间字段同时使用 `created_at/updated_at` 和 `create_time/update_time`，这是旧模型与新模型混合造成的，新增表统一使用后者即可。
4. 文件开头写了“H2 MODE=MySQL”，但 `UNSIGNED`、MySQL `JSON`、部分索引和外键行为不能据此保证与H2完全一致；真实模式应以MySQL 8验证为准。
5. 不建议把这份汇总SQL反复覆盖到已有数据库。已有环境应按版本执行 `database/migrations` 下尚未执行的迁移。

## 六、当前执行顺序

对于你已有的 `bio_portal` 数据库，不要重新执行整份汇总建表文件。只需要确认原有表存在，然后执行尚未执行的：

```sql
-- 本次新增统计快照
SOURCE F:/project/bio-portal-suite/database/migrations/V10__add_research_stat_snapshots.sql;
```

执行后通过 `POST /api/research-snapshots/build` 创建第一个统计版本。

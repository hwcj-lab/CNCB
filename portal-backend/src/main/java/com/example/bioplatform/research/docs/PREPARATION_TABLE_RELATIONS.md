# 任务二数据库表关系与功能

本图按当前代码整理。箭头多数是应用层逻辑关联，并非数据库外键；本次没有添加外键、变更统计口径或执行历史回填。

## 业务路径

```mermaid
flowchart TB
  subgraph COLLECT[1 采集与关联文献]
    O[OpenLB PMID采集] --> R[openlb_bank_route<br/>db到来源编码的路由]
    N[GenBase / GSA / BioProject独立采集] --> RAW[各来源 *_publication<br/>原始文献与JSON]
    R --> RAW
    O --> FALLBACK[openlb_publication<br/>空或未匹配路由的暂存记录]
    INV[OpenLB独立盘点] --> BANKS[openlb_bank_inventory<br/>cncbBanks条目与PMID盘点]
    RAW --> RELATE[关联文献采集]
    RELATE --> RAW
    RELATE --> EDGE[research_publication_relation<br/>来源范围内的文献对及证据]
    RAW -. 采集仍同步维护 .-> OLD[research_stat_publication<br/>原全量去重整合表]
  end
  subgraph PREPARE[2 独立数据整理]
    JOB[research_preparation_job<br/>任务与整理修订号] --> CURSOR[research_preparation_source_cursor<br/>每个来源的时间窗口与断点]
    RAW --> PARSE[本地解析与版本维护]
    OLD -->|RESOURCE行经资源适配器读取| PARSE
    JOB --> PARSE
    DICT[research_fund_dictionary<br/>基金编号到名称的字典] --> PARSE
    PARSE --> DOC[research_prepared_document<br/>来源文献或资源基本信息及版本]
    DOC --> AUTHOR[research_prepared_author<br/>文献 作者 机构]
    DOC --> INST[research_prepared_institution<br/>文献 机构]
    DOC --> FUND[research_prepared_fund<br/>文献 资助方]
    DOC --> TOPIC[research_prepared_topic<br/>文献 关键词或MeSH主题]
    DOC --> YEAR[research_prepared_year<br/>文献 年份]
    DOC --> IDENTITY[research_prepared_identity<br/>每个修订的DOI与PMID桥接]
  end
  subgraph SNAPSHOT[3 统计快照与查询]
    DOC --> AGG[各维度分别聚合<br/>分库限定来源 总量跨来源去重]
    AUTHOR --> AGG
    INST --> AGG
    FUND --> AGG
    TOPIC --> AGG
    YEAR --> AGG
    IDENTITY --> AGG
    CATEGORY[research_topic_category<br/>固定主题分类] --> MAPPING[research_database_topic<br/>数据库到分类的映射]
    MAPPING --> AGG
    AGG --> SNAP[research_stat_snapshot<br/>版本状态 来源选择及汇总量]
    AGG --> RANK[research_stat_dimension<br/>排名 年度 分类统计结果]
    AGG --> CONFIG[research_prepared_snapshot<br/>冻结整理修订 来源名称 分类范围]
    SNAP --> UI[前端图表及版本选择]
    RANK --> UI
    CONFIG --> DRILL[排名穿透<br/>固定版本的维度表加document]
    UI --> DRILL
    UI --> RELATED[关联文献按钮<br/>查询当前关系及原始来源数据]
    EDGE --> RELATED
    RAW --> RELATED
  end
```

`openlb_publication`及盘点表不是新统计的数据源。`research_stat_publication`目前仍被采集同步；普通文献从各来源表整理，RESOURCE行仍由资源适配器从该表读取。新快照直接读取整理表，不在快照阶段从旧表展开维度，因此旧表目前不能删除。旧的`research_stat_dimension_candidate`和`research_stat_dimension_publication`不参与这条新统计路径。

## 新增表的逻辑关系

```mermaid
erDiagram
  research_preparation_job ||--o{ research_preparation_source_cursor : "id = job_id"
  research_preparation_job ||--o{ research_prepared_document : "id = valid_from 生效修订"
  research_preparation_job ||--o{ research_prepared_identity : "id = revision"
  research_preparation_job ||--o{ research_prepared_snapshot : "id = preparation_revision"
  research_prepared_document ||--o{ research_prepared_author : "id = document_id"
  research_prepared_document ||--o{ research_prepared_institution : "id = document_id"
  research_prepared_document ||--o{ research_prepared_fund : "id = document_id"
  research_prepared_document ||--o{ research_prepared_topic : "id = document_id"
  research_prepared_document ||--o{ research_prepared_year : "id = document_id"
  research_stat_snapshot ||--o| research_prepared_snapshot : "id = snapshot_id"
  research_stat_snapshot ||--o{ research_stat_dimension : "id = snapshot_id"
```

- 文献版本在`valid_from <= 快照修订`，且`valid_until`为空或大于快照修订时有效。
- `research_prepared_identity`在查询时按`快照修订 + DOI`与document逻辑关联，不通过document_id关联。
- `research_prepared_document.source_code`和维度表的`source_code`是来源编码，不是另一张全量表的ID。
- 不同数据库可以保存相同PMID；总统计按规范化文献键去重。来源内统计只读取该来源。
- 总统计的固定主题分类读取数据库映射；分库研究主题读取整理后的关键词/MeSH成员，两者口径不同。

## 关键字段与唯一性

| 字段 | 用途 | 不应混淆为 |
| --- | --- | --- |
| document.id | 某来源某条记录的整理版本ID | 全局文献唯一ID |
| document.source_row_id | 原始来源适配器的扫描行ID，可包含类型编码 | 一定等于原始表物理主键 |
| source_code + source_hash | 来源内业务记录身份；source_hash为source_key摘要 | 跨数据库去重键 |
| document_key | PMID、DOI或来源业务键形成的去重候选键；查询时还使用identity桥接 | 所有场景直接按此字段就能去重 |
| document_id | 维度成员所属的document版本ID | 作者ID或排名ID |
| member_key | 当前维度规范化值的摘要；作者维度为作者姓名加机构 | 可靠的自然人唯一身份 |
| author_order | 作者解析后的署名次序，从1开始；多机构展开保持同序号 | 作者统计排名 |
| revision / valid_from / valid_until | 整理版本、记录生效和失效边界 | 发表年份或时间戳 |

维度表唯一键是`(document_id, member_key)`。同一来源同篇文献中的同机构、同资助方、同主题只计一次；作者维度保留每位作者对应的机构，不能用第一位作者的机构代表整篇文献。

五张维度表复用了相同的成员结构，因此机构/基金/主题表中的`author_name`、`institution_name`、`author_order`目前为空；机构名称本身存`display_name`。`stat_year`目前只在年份表赋值。这些保留字段并非再次解析得到的数据。

## 两种穿透

1. 点击排名：使用快照冻结的修订和范围，经维度表`document_id`关联document，按文献标识去重，返回标题、来源及跳转地址。不会查询旧的维度-文献穿透关系表。
2. 点击文献的“关联文献”：以数据库编码及PMID/DOI/来源业务键查询`research_publication_relation`，再读取该来源原始文献表的详情。这条路径读取当前数据，并未冻结到快照修订。

## 字段注释SQL

- 新建库：`database/task2-preparation-20261126.sql`，10张表、113个字段均带中文注释。
- 已有库：`database/task2-preparation-comments-20261126.sql`，只修改已有字段/表注释，不回填数据；可重复执行。必须先具备现有整理表及增量字段。
- 原增量结构升级：`database/task2-preparation-incremental-20261126.sql`也已补注释；已有字段不要重复执行ADD COLUMN。

注释脚本带5秒元数据锁等待超时，并要求`ALGORITHM=INPLACE, LOCK=NONE`，不自动退化为复制表。建议整理和快照任务停止后执行；锁超时可以稍后重新执行。脚本不会启动采集、整理或快照任务。

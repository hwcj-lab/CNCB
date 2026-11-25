# 文献来源表字段保留审计

调研日期：2026-11-17。

`openlb_publication`与`genbase_publication`已经按“统计必要字段 + 关联发现字段 + 完整原始JSON”精简。旧宽表由用户保留为`openlb_publication1`、`genbase_publication1`，新表通过`database/task2-compact-publication-rebuild.sql`迁移，不重新请求上游。

## 当前统计直接使用

- PMID、DOI、标题、发表年份、期刊。
- `authors_json`：作者以及每位作者自己的`affiliation`。
- `affiliation_info`：由作者数组汇总的文章机构摘要，便于查询和复核。
- `grants_json`：基金机构、项目号等原始结构。
- `keyword_list`、`mesh_headings_json`：研究主题。
- 来源记录主键及CNCB关联字段：去重和可追溯关系。

## 保留的恢复与关联字段

- 摘要：后续摘要主题和详情需要。
- `raw_payload`、`openlb_raw_payload`：上游字段变化和解析错误的恢复证据。
- 来源时间、采集时间、状态、重试次数：增量采集、断点续采和排错需要。
- GenBase的BioProject和accession：研究主题及资源穿透需要。

## 已从新表删除

- OpenLB：owner、状态/模式、ISSN、卷期页码、版权、语言、国家、文献类型、PII/PMCID/PPRID、license、category、来源日期、dataBanks、chemicals、references及重复计数等。
- GenBase：publisher、原始月份、volume、missionId、accessLink、OpenLB内部ID、authorString、语言、国家、文献类型和PMCID等。

这些字段仍存在于完整`raw_payload`或旧`*1`表中，可离线恢复。

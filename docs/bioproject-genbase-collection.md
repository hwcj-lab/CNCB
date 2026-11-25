# BioProject 与 GenBase 文献采集分析

调研日期：2026-11-13。

## BioProject 可用入口

| 入口 | 数据范围 | 验证状态 |
| --- | --- | --- |
| `GET https://ngdc.cncb.ac.cn/bioproject/json/ngdcBrowse.json` | 全量目录；项目号、标题、数据类型、物种、提交单位 | 已验证，JSON |
| `GET https://ngdc.cncb.ac.cn/bioproject/browse/{PRJCA}` | 详情；发布日期、描述、出版信息、基金、提交者、提交单位、提交日期及关联资源 | 已验证，服务端HTML |
| `GET https://ngdc.cncb.ac.cn/gsa-human/hra/getAjax/searchByPrjAccession?prjAccession={PRJCA}` | BioProject关联的GSA-Human数据集 | 已验证，但不是BioProject详情接口 |
| `GET https://ngdc.cncb.ac.cn/gwh/api/public/bioProject/{PRJCA}` | GWH覆盖项目的结构化详情 | 官方文档确认；不能代表全量BioProject |

目录JSON不含完整出版、基金和提交者字段；完整BioProject详情目前未发现等价的公开JSON接口。因此后续如实施，应先用目录JSON建立PRJCA任务，再低速解析详情HTML。HTML解析必须保留原始页面或字段证据，并在结构变化时停止而不是写入错误数据。

### 指标字段可行性

| 指标 | BioProject详情字段 | 可行性 |
| --- | --- | --- |
| 年度文献趋势 | 出版信息中的发表年份 | 可实现，但只覆盖填写了出版信息的项目 |
| 研究主题 | 涉及领域、数据类型、标题、描述、样品范围 | 可实现多口径分类，口径需在统计层固定 |
| 核心机构 | 提交单位 | 只能表示项目提交机构，不能等同论文作者机构 |
| 核心作者 | 提交者 | 只能表示项目提交者；不能等同论文完整作者列表 |
| 基金项目 | 资助机构、项目类型、授权项目ID、名称 | 可实现项目资助统计 |

### 暂不实现的增量策略

当前目录JSON未提供可靠的发布日期、更新时间或稳定游标，不能安全地仅按页面顺序增量。

`GenBaseCollectionServiceImpl` 中已保留 TODO：如果官方后续提供稳定的发布日期筛选、更新时间或游标，再以水位方式增量；在此之前不能把“当前排序”当作不变边界。

## GenBase 采集流程

数据源：`GET https://ngdc.cncb.ac.cn/genbase/statistics/getSupportPaperList?draw=1&start={offset}&length={pageSize}`。

流程：

1. 创建或恢复 `genbase_collection_job`。
2. 按 `start/length` 顺序请求一页。
3. 将一页数据转换后通过单条多值 `INSERT IGNORE` 写入 `genbase_publication`。
4. 在同一事务中更新 `next_start`，保证中断后从最后提交页继续。
5. 列表扫描结束后，批量领取有PMID且尚未补全的GenBase记录，调用OpenLB PMID接口补充摘要、作者、机构、关键词、MeSH和基金。
6. GenBase已经提供数据库关联证据，因此OpenLB的`cncbBanks`为null或空数组时仍保存其他文献字段。
7. 发生限流、超时或 502/503/504 时指数降速；达到阈值自动停止。
8. 列表和OpenLB补全都结束后状态为 `SUCCESS`。再次启动会创建新全量扫描，但唯一索引不会重复插入文献。

### 唯一性

- `paper_id`：GenBase接口记录标识，唯一。
- `pmid`：接口中的 `pubmedId`，确认为PubMed ID；非空时唯一。
- `doi`：规范化后非空时唯一。
- `bioproject_accession`：不能唯一。一个BioProject可以关联多篇论文，一篇论文也可能关联多个项目，因此只建立普通索引。

当前表是来源原始表，尚未同步到科研统计的 `publication/author/institution/fund_project` 规范化模型。

## 管理接口

| 方法 | 地址 | 说明 |
| --- | --- | --- |
| POST | `/api/collection/genbase/jobs/start` | 启动新扫描或继续断点 |
| POST | `/api/collection/genbase/jobs/stop` | 当前页完成后停止 |
| GET | `/api/collection/genbase/status` | 查询最近作业进度 |


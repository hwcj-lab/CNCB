# 当前代码结构

> 2026-11-25：代码已拆分为任务一、任务二模块，当前目录以 [代码模块说明](MODULE_STRUCTURE.md) 为准。下文为早期设计记录，其中目录与数据流程不代表当前实现。

更新日期：2026-11-16。

## 后端业务边界

```text
controller
├─ DatabaseController / ToolController       资源与工具推荐
├─ OpenLbCollectionController                OpenLB采集任务
├─ GenBaseCollectionController               GenBase采集与OpenLB补全
└─ ResearchSnapshotController                多来源统计快照

service
├─ CatalogService                            资源目录查询和推荐
├─ OpenLbCollectionService                   OpenLB断点采集
├─ GenBaseCollectionService                  GenBase断点采集
├─ ResearchPublicationSource                 文献来源扩展点
└─ ResearchSnapshotService                   快照任务和历史版本

service/impl
├─ OpenLbResearchPublicationSource           OpenLB来源适配器
├─ GenBaseResearchPublicationSource          GenBase来源适配器
├─ ResearchPublicationSourceRegistry         自动收集全部来源实现
└─ ResearchSnapshotServiceImpl               去重、聚合、批量写入
```

旧 `ResearchAnalysisController/Service/Mapper` 已删除。科研分析页面不再查询不存在的
`publication/author/institution/fund_project` 旧规范化表。

## 文献数据流

```text
上游接口
  ├─ OpenLB采集任务  → openlb_publication
  └─ GenBase采集任务 → genbase_publication
                           ↓
             ResearchPublicationSource适配器
                           ↓
             PMID → DOI → 来源记录ID去重
                           ↓
       research_stat_snapshot（一次执行/一个版本）
          ├─ research_stat_publication（文献证据）
          └─ research_stat_dimension（图表聚合）
                           ↓
          /api/research-snapshots/latest/result
```

## 批量任务控制

- `POST /api/research-snapshots/build`：启动后台任务；同一进程只允许一个任务。
- `GET /api/research-snapshots/status`：查询阶段和进度文本。
- `POST /api/research-snapshots/stop`：请求安全停止；当前批次结束后标记 `STOPPED`。
- 应用重启时，遗留的 `BUILDING` 自动标记 `FAILED`。
- 文献证据和统计维度均分批写入；只有 `SUCCESS` 版本会成为页面默认版本。
- 失败或停止不会影响上一个成功版本，也不会修改OpenLB/GenBase原始表。

## 前端

- `views/tool-recommend`：资源推荐页面。
- `views/research-analysis`：只读取快照总汇和快照文献分页。
- `api/research.js`：快照创建、停止、状态、总汇、文献分页。
- 图表组件只负责渲染，不直接访问数据库或旧统计接口。

## 新增文献来源

新增来源只需要：

1. 建立该来源自己的原始表和采集Controller/Service/Mapper。
2. 实现一个 `ResearchPublicationSource` 适配器，批量映射为 `SourcePublicationRecord`。
3. Spring自动注册；汇总入口不增加来源判断分支。

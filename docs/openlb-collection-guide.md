# OpenLB 文献采集使用说明

更新日期：2026-11-13。

## 数据流程

1. `docs/openlb_pmid.csv` 是PMID输入，原文件不会被改写。
2. 导入接口使用PMID唯一索引写入 `openlb_pmid_task`，重复PMID自动忽略。
3. 启动接口立即返回，后台只选取 `PENDING` 任务。
4. 请求 `https://ngdc.cncb.ac.cn/openlb/api/pmid?pmid={pmid}`，只有 `cncbBanks` 为非空数组才写入 `openlb_publication`。
5. 任务最终状态为 `SUCCESS` 或 `EMPTY`；失败先延迟重试，达到次数上限后为 `FAILED`。

数据库写入以批次为单位：CSV任务和有效文献都使用 `INSERT IGNORE INTO`，依赖PMID唯一索引在数据库层去重。CSV每500条一个JDBC batch；每批网络请求结束后，在一个事务中批量插入文献，再分别批量更新SUCCESS、EMPTY和FAILED状态。工作线程不单条写数据库。
默认MySQL连接串已启用 `rewriteBatchedStatements=true`，使Connector/J尽量把JDBC batch合并发送。如果通过 `DB_URL` 覆盖连接串，应保留该参数。

## 执行顺序

重启后端后，使用ApiPost或Swagger依次调用：

1. `POST http://localhost:8080/api/collection/openlb/tasks/import-csv`
2. `POST http://localhost:8080/api/collection/openlb/jobs/start`
3. `GET http://localhost:8080/api/collection/openlb/status`
4. 需要停止时：`POST http://localhost:8080/api/collection/openlb/jobs/stop`
5. 人工确认上游恢复后，重置最终失败任务：`POST http://localhost:8080/api/collection/openlb/tasks/retry-failed`

启动请求示例：

```json
{
  "concurrency": 3,
  "batchSize": 60,
  "minDelayMs": 350,
  "maxAttempts": 3,
  "stopAfterConsecutiveThrottles": 5
}
```

## 断点与去重

- `openlb_pmid_task.pmid` 和 `openlb_publication.pmid` 都有唯一索引。
- CSV重复PMID由 `INSERT IGNORE INTO` 在数据库层忽略，不先逐条查询是否存在。
- `SUCCESS` 和 `EMPTY` 不再请求，因此重复导入CSV不会重复采集。
- 应用异常终止时可能留下 `RUNNING`；下次启动会先把它们恢复为 `PENDING`。
- 任务选取使用 `(status,next_retry_time,id)` 组合索引，数量增长时不需要全表扫描PMID。

## 限流处理

- 所有工作线程共享全局请求间隔，不会每个线程各自无限发送。
- 收到HTTP 429或503时，间隔翻倍，最高10秒。
- 连续限流达到配置阈值后自动停止，`status.message` 说明停止原因。
- 普通失败使用延迟重试；达到 `maxAttempts` 后改为 `FAILED`，不会无限等待。

## 字段与后续同步

`openlb_publication.grants_json` 保存OpenLB响应中的论文基金数组，其中包含 `grantId`、`acronym`、`agency`和`country`。`cncb_banks_json` 保存资源登录号、数据库名和跳转链接。本阶段不同步到 `publication`、`fund_project` 等统一分析表。

# OpenLB查漏补缺流程（2026-11-23）

先执行 database/task2-openlb-routing-20261123.sql，再启动后端。此版本不自动执行历史回填或采集。

## 来源盘点

前端科研统计页的“OpenLB来源盘点”按钮，或 POST /api/collection/openlb/banks/start 启动。
读取openlb_pmid_task全部PMID（不改变原采集状态），仅存cncbBanks字段到openlb_bank_inventory。
GET /status 查询当前PMID、停止原因、持久化游标；POST /stop 请求停止；再次start断点继续。
上游错误、429、超时立即停止保留游标，不长时间静默重试。每次请求间隔800ms，超时25秒。
GET /summary 返回数据库类别、资源数、去重文献数；GET /records?page=1&size=100 查看明细。
盘点按PMID字符串排序；结束后新增小于游标的PMID需要人工将openlb_bank_inventory_job.cursor_pmid重置为空重新盘点，明细按PMID和数组下标覆盖。

## 目标配置及采集

根据盘点结果建表：同结构目标使用 CREATE TABLE xxx_publication LIKE openlb_publication。
配置openlb_bank_route：bank_db严格对应cncbBanks.db，source_code为大写字母数字下划线，表名固定为小写代码_publication。
GenBase使用既有genbase_publication专用适配，不要用模板覆盖现有表。
一个文献多个db：每库一份，单库PMID唯一；空cncbBanks或存在未配置db时保存到openlb_publication兜底。
未知db和空cncbBanks统一保存到openlb_publication；配置目标表和路由后，调用redistribute-existing，从afterId=0重新扫描本地数据，不请求上游。一个文献部分db已配置、部分未配置时，同时写入已配置目标和OpenLB兜底表；重复分流按PMID更新去重。
原采集/jobs/start仍处理原PMID任务，无需重新导入CSV。已SUCCESS的历史任务不会被重新请求。

## 历史文献与关系

POST /redistribute-existing?afterId=0 读取原OpenLB表100条本地记录分流，返回nextAfterId供下一批调用。
同步复制匹配PMID端点的旧OPENLB关联边到目标库代码，唯一索引忽略重复边。
旧表留作归档不自动删除；关联边的另一端尚未存在于目标表时，后续公共关联采集会补全详情。
本地分流接口由调用者分批执行，未在本次修改中运行。

## 公共关联与统计

目标表记录待关联，通过现有/api/collection/publication-relations接口传source_code运行。
通用同结构库共用原OpenLB关联解析流程，GenBase保留原生唯一值适配；资源解析器仍按链接类型扩展。
新库同结构来源由路由配置自动注册，供整合、年度、作者机构、主题、基金及穿透使用。
OpenLB兜底表不注册统计来源；全局重建不会再读取它。
完成本地分流后调用现有整合表全量重建，再生成新快照，旧快照保持原口径。
请给实际库代码配置research_database_topic主题映射，OpenLB本身不参与总主题统计。

## 新接口测试JSON

docs/apipost/task2-openlb-routing-20261123.json，仅本次新增接口。

## 移除独立暂存表

replay-pending接口已移除。旧暂存表有数据时先执行database/task2-remove-openlb-route-pending.sql迁移到OpenLB，再删除暂存表。新部署只需原建表SQL，已不再创建暂存表。

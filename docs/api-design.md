# REST API设计

基地址：http://localhost:8080/api 。Swagger：http://localhost:8080/swagger-ui/index.html ，OpenAPI JSON：/v3/api-docs。快照模块包含显式触发、停止和删除管理接口，其余业务查询以GET为主。

## 科研统计快照

| 方法与路径 | 业务内容 |
| --- | --- |
| POST `/research-snapshots/build` | 后台生成新快照 |
| POST `/research-snapshots/stop` | 安全停止当前构建 |
| GET `/research-snapshots/latest/result?limitPerDimension=50` | 最近成功快照；主题、机构、作者、基金默认Top 50 |
| GET `/research-snapshots/sources` | 查询生成快照时可选择的本地文献来源 |
| POST `/research-snapshots/build` | 可传`name`与`sourceCodes`；未传来源时默认全选 |
| PATCH `/research-snapshots/{snapshotId}/name` | 修改快照展示名称，不重新统计 |
| GET `/research-snapshots/{id}/dimensions/{dimensionId}/publications?page=1&size=10` | 从图表维度穿透到具体文献 |
| DELETE `/research-snapshots/{id}` | 删除非BUILDING快照，级联删除文献、维度和穿透关系 |

TOPIC来自论文关键词与PubMed MeSH主题词，是多标签统计。作者维度组合作者姓名和该作者自己的署名机构；尚未做人名身份消歧。

## 统一响应

```json
{"code":"OK","message":"成功","data":{},"timestamp":"2026-11-06T08:00:00Z"}
```

错误保持相同结构，data=null。HTTP 400为参数错误，404为资源/身份不存在，403为当前环境禁止演示，500为内部错误（不返回堆栈/凭证），405为请求方式不支持。

## 资源目录

| 方法与路径 | 参数 | 内容 |
| --- | --- | --- |
| GET /system/status | 无 | 应用/数据库连通、demoEnabled、catalogProvenance、调研日期、cncbbankStatus、目录数量 |
| GET /resource-recommendations | `keyword`可选，最长100字符 | 后端按数据库短名、中英文标题过滤并按数据类型分组；空分类不返回 |
| POST /resource-recommendations/search-results | CNCB搜索响应中的 `result.databases` 数组，1—2000项 | 按每项字符串 `id` 精确匹配官方 `database_resource.db_id`；复合值按分号分词，只返回存在命中数据库的分类并附带工具，空分类省略 |
| GET /data-types | 无 | 类型列表，coreType为四类核心数据，其他类型作为辅助 |
| GET /databases | dataType可选；keyword可选≤100字符 | 数据库和tools；不传类型返回全部目录。类型证据仅在详情接口返回 |
| GET /databases/{id} | 正整数 | 资源详情、所有类型与关联入口；不存在404 |
| GET /databases/{id}/tools | dataType可选 | 只取该库匹配类型的工具关系 |
| GET /tools/recommendations | dataType必填 | 按已整理tool_data_type推荐；不是调用外部算法 |
| GET /tools/{id} | 正整数 | 工具部署实例、来源、linkEnabled和runVerified |

dataType来自/data-types的code，如DNA、PROTEIN、LITERATURE、SPECIES。未知code返回400。tool.serviceKind区分ANALYSIS、BROWSER、DETAIL、HELP、ANALYSIS_PORTAL、LITERATURE_SEARCH、DATA_SERVICE。linkEnabled控制可展示入口，runVerified本轮均false；二者不得混用。relationKind与basis说明数据库关联是原生还是本项目兼容建议。

## 科研统计通用筛选

| 参数 | 规则 |
| --- | --- |
| mode | REAL默认 / DEMO；后端demoEnabled=false时DEMO返回403 |
| fromYear / toYear | 可选1500–2200；fromYear不得晚于toYear |
| databaseId | 可选正整数，先筛选包含该数据库的文献集合；不存在404 |
| limit | 排名条数1–50，默认10 |
| page / size | 文献分页；page 1–10000，size 1–50，默认1/10 |

| 方法与路径 | 业务内容 |
| --- | --- |
| GET /research/summary | 五项指标与字段覆盖数，缺身份/基金不是0而是null/PENDING |
| GET /research/annual | 年份、去重文献数；未知年单独返回 |
| GET /research/topics | 资源关联类别，多标签；不是学科学术主题 |
| GET /research/institutions | AUTHOR_AFFILIATION署名机构排名 |
| GET /research/authors | 仅已消歧作者排名 |
| GET /research/funds | PAPER_FUNDING基金项目排名 |
| GET /research/institutions/{id}/trend | 当前模式下指定机构年度趋势 |
| GET /research/authors/{id}/trend | 当前模式下指定已消歧人员年度趋势 |
| GET /research/publications | 统计来源文献分页，模式与来源URL可追踪 |
| GET /research/sources | 18项官方API注册表；不发起外部请求 |

图表返回对象：
```json
{
 "dataMode":"REAL",
 "status":"PARTIAL",
 "message":"有限调研样例；不是中心全量成果。",
 "totalPublications":3,
 "coverageCount":3,
 "unknownYearCount":0,
 "items":[{"id":null,"name":null,"year":2014,"value":1}]
}
```

此处数值是初始REAL有限样例口径。实际从SQL计算，非Controller常量。排名items包含id/name/value，年度items包含year/value。总量不因多库重复关联增加；类别计数允许多标签。日期过滤生效时未知年份不属于该年份集合，unknownYearCount只描述当前集合，不是全库缺失率。

## 状态语义

| 状态 | 含义 |
| --- | --- |
| DEMO | 合成数据，有独立来源标记；不是实际科研成果 |
| PARTIAL | 有限真实样例或有限字段覆盖；不能当全量 |
| PENDING | 集合内有文献，但此指标没有可用关联字段 |
| EMPTY | 当前筛选集合没有文献 |

summary.metrics[].value可为null；summary.coverage提供实际已登记字段的覆盖计数。不能用nullValue || 0把待接入展示成已确认0成果。真实不存在身份的趋势请求404，不悄悄退到DEMO。

## 扩展接口约定

保留对前端的只读边界，不将 `/publication` 页面当API，也不实现猜测的cncbbank参数或通用URL代理。OpenLB采集只使用已实际验证的PMID接口，作为离线管理任务运行，并具备超时、重试、幂等和分批入库。

OpenLB首批采集使用已验证的 `GET https://ngdc.cncb.ac.cn/openlb/api/pmid?pmid={pmid}`。独立的 `OpenLbCollectionController` 和 `OpenLbCollectionService` 从 `docs/openlb_pmid.csv` 导入持久化PMID任务，后台并发请求后仅写入 `cncbBanks` 非空记录。采集模块支持限速、重试、断点续采、主动停止和限流自动停止；不与前端统计查询混用。

CSV任务和OpenLB文献均使用 `INSERT IGNORE INTO` 与PMID唯一索引批量去重；采集工作线程只返回内存结果，每个批次结束后才在单个事务中批量插入文献和批量更新任务状态，不执行逐PMID的存在性查询或单条提交。

新增接口时同时更新Controller的Operation、DTO校验、Service缺失语义、Mapper参数化查询和文档。代理与CORS只用于本机开发，不等于生产认证或数据许可。

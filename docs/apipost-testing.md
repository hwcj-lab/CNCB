# ApiPost接口测试说明

更新日期：2026-11-13。

## 导入文件

`docs/apipost/bio-portal-api.json`：项目原有的完整接口测试集合。所有请求均使用 `http://localhost:8080` 完整地址，不需要导入环境变量。

后续接口变更不再覆盖该总集合，而是在 `docs/apipost/` 下按日期生成“仅包含本次新增接口”的增量JSON。2026-11-13新增GenBase接口请导入 `2026-11-13-genbase-collection-added.json`。

ApiPost中选择“导入/导入数据”，格式选择Postman，只导入这一个JSON即可。

## 启动后端

```powershell
$env:DB_USERNAME='root'
$env:DB_PASSWORD='你的MySQL密码'
.\scripts\Start-Backend.ps1 -Profile mysql
```

不要把真实密码写入ApiPost请求。当前接口没有登录认证，请求中不需要数据库账号或密码。

## 当前业务接口

| 模块 | 方法 | 地址 | 用途 |
| --- | --- | --- | --- |
| 系统 | GET | `/api/system/status` | 检查应用和数据库连接 |
| 推荐 | GET | `/api/resource-recommendations` | 一次返回数据类型、数据库和工具 |
| 推荐 | GET | `/api/data-types` | 数据类型列表 |
| 推荐 | GET | `/api/databases` | 数据库列表，可用dataType、keyword筛选 |
| 推荐 | GET | `/api/databases/{id}` | 数据库详情 |
| 推荐 | GET | `/api/databases/{id}/tools` | 数据库关联工具 |
| 工具 | GET | `/api/tools/{id}` | 工具详情 |
| 科研 | GET | `/api/research/summary` | 科研概览 |
| 科研 | GET | `/api/research/annual` | 年度文献趋势 |
| 科研 | GET | `/api/research/topics` | 资源关联类别分布 |
| 科研 | GET | `/api/research/institutions` | 机构排名 |
| 科研 | GET | `/api/research/authors` | 作者排名 |
| 科研 | GET | `/api/research/funds` | 基金统计 |
| 科研 | GET | `/api/research/institutions/{id}/trend` | 机构年度趋势 |
| 科研 | GET | `/api/research/authors/{id}/trend` | 作者年度趋势 |
| 科研 | GET | `/api/research/publications` | 来源文献分页 |
| 科研 | GET | `/api/research/sources` | 官方接口登记清单 |
| 文档 | GET | `/v3/api-docs` | OpenAPI JSON |

## 科研分析公共参数

| 参数 | 类型 | 限制 | 默认值 |
| --- | --- | --- | --- |
| mode | REAL/DEMO | mysql正式模式通常只允许REAL | REAL |
| fromYear | 整数 | 1500～2200 | 空 |
| toYear | 整数 | 1500～2200，且不能小于fromYear | 空 |
| databaseId | 正整数 | 必须存在 | 空 |
| limit | 整数 | 1～50 | 10 |
| page | 整数 | 1～10000 | 1 |
| size | 整数 | 1～50 | 10 |

## 用例分组

- `01 系统与文档`：状态和OpenAPI文档。
- `02 数据资源与工具`：目录总接口、筛选、详情和工具关系。
- `03 OpenLB文献采集`：CSV导入、启动、停止、重试和状态。
- `04 科研产出分析 REAL`：所有真实模式统计接口。
- `05 参数与异常`：400、403和404场景。

统一成功响应：

```json
{
  "code": "OK",
  "message": "成功",
  "data": {},
  "timestamp": "2026-11-11T00:00:00Z"
}
```

趋势接口中，文件内写死的测试ID在真实数据库不存在时返回404，这是数据未接入状态，不是服务异常。`DEMO`用例在mysql profile下预期403；显式开启演示模式时允许200。

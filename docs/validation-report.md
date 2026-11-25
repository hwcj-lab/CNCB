# 验证记录

## 2026-11-11 回归验证

恢复 `application-demo.yml` 后重新执行 `scripts/Verify.ps1`：后端9项测试全部通过，前端4项测试全部通过，后端JAR与前端生产构建成功。H2 demo profile继续按顺序加载 `schema.sql`、`init-data.sql`、`task1-relations.sql` 和 `demo-data.sql`。本次是代码与构建回归验证，没有重新执行下文2026-11-06的整套浏览器截图验收；下文浏览器记录保留为历史验证证据。

验收日期：2026-11-06（Asia/Shanghai）。以下为实际执行结果，不将生成代码或静态阅读当作启动/运行成功。

## 环境

| 项目 | 本次实际环境 |
| --- | --- |
| 系统 | Windows，PowerShell |
| JDK | 项目内Microsoft OpenJDK 17.0.21.0.2+1；编译release 17 |
| Maven | 3.9.11，依赖/临时目录均在项目.local |
| Node / npm | 实际构建Node 26.13.0 / npm 11.20.0；前端要求满足package.json engines，日常建议24 LTS |
| Spring Boot / MyBatis-Plus | 3.5.16 / 3.5.18 |
| MySQL | 8.0.18，独立数据目录.local/mysql-validation/data，端口13307，验证库bio_portal_validation |
| 浏览器 | 已安装Microsoft Edge，无头自动化；桌面1440×1080、移动390×844 |
| 服务 | H2后端8080、MySQL后端8081、Vite5173，用于本次验收 |

未读取或改动用户现有3306库，也未声称已配置其账号。隔离MySQL验证库用本机临时初始化账号，不是生产账号方案。

## 构建与接口

| 检查 | 实际执行 | 结果 |
| --- | --- | --- |
| 调研读取 | 8份现有文档，按任务分目录 | 已读取；原内容保留，导航链接修复 |
| 后端干净编译 | JDK17 Maven clean test；修复后test package | 通过，34个Java主源码可编译，生成可执行JAR |
| 后端集成测试 | PlatformIntegrationTest 7项 + DemoDisabledTest 1项 | 8项通过，0失败，0错误 |
| H2启动 | scripts/Start-Backend.ps1 -Profile demo | 正常启动8080，并执行SQL初始化 |
| MySQL连接与启动 | mysql profile JAR，8081，DEMO_ENABLED默认false | health实际返回MySQL；查询成功 |
| H2 HTTP联调 | node scripts/smoke-api.mjs | 目录、类型约束、统计、分页、异常、来源、趋势通过 |
| MySQL HTTP联调 | BASE_URL=http://127.0.0.1:8081 同上 | REAL统计/目录通过；DEMO请求按配置403 |
| Vite代理联调 | BASE_URL=http://127.0.0.1:5173 同上 | 前端同源/api转发H2成功 |
| Swagger | MockMvc /v3/api-docs、/swagger-ui/index.html | 200且OpenAPI结构有效 |
| 前端依赖 | npm install（workspace cache）生成package-lock | 成功，Vue/Element Plus/Axios/ECharts可解析 |
| 前端测试 | npm test | 4项通过：安全外链、年份间隔、分类多标签、排名转换 |
| 前端生产构建 | npm run build | 成功，输出portal-frontend/dist；有>500kB分包提示，不是错误 |

后端集成测试具体验证：23目录资源、10类型、34工具/服务；MethBank不会因DNA相关标签得到序列工具；禁用入口和run_verified分开；REAL3与DEMO30隔离；同论文多库关系不膨胀总量；年份/库筛选；机构/作者趋势模式隔离；真实身份缺失返回null/PENDING；无效年份/分页/类型返回400；未知资源404；禁演示403；SUBMITTER机构与未消歧作者不参与排名；未知年计数正确。

## 浏览器正常流程17项

| 检查 | 结果 |
| --- | --- |
| 资源页面与真实后端API联通 | 通过 |
| 四类数据类型来自API | 通过 |
| 蛋白序列切换可看到有依据的Needle推荐 | 通过；不是外站运行成功 |
| 数据库GWH关键词搜索 | 通过 |
| 数据库详情真实名称与证据 | 通过 |
| 工具href为官方URL | 通过 |
| 新窗口target与noopener | 通过 |
| 真实模式显示3条样例 | 通过 |
| 未知机构显示“—”而非伪造0 | 通过 |
| 合成模式折线/环图/机构/作者四图canvas实际渲染 | 通过 |
| 合成数据有显著标记 | 通过 |
| 选择机构后年度趋势实际渲染 | 通过 |
| 选择人员后年度趋势实际渲染 | 通过 |
| 刷新/research-analysis深路由仍正常，恢复真实模式 | 通过 |
| 390px资源页无横向溢出 | 通过 |
| 390px分析页无横向溢出，移动菜单切换 | 通过 |
| 正常流程控制台与页面脚本错误 | pageerror=0，console.error=0 |

另有6项定向交互检查通过：来源抽屉18项、2018年筛选显示4篇DEMO、组合BioProject后显示1篇、文献下一页、模拟503后不保留旧指标/不静默回退演示、重试恢复。模拟503是主动故障注入，不能与正常流程“无console.error”混为一谈。

## 外部导航限制

对Needle进行了一次实际点击：新窗口机制触发，目标为已记录官方地址https://ngdc.cncb.ac.cn/bit/needle，但外站加载失败，浏览器报告net::ERR_EMPTY_RESPONSE。**不能把href正确说成外站加载成功。** 已列入问题表B13，界面使用“调研页面已核验”而非保证当前可达。未逐站复测全部外链，未提交任何序列、分析或登录任务；外部计算结果仍未验证。

## 截图与证据

以下截图经过人工目视检查，未见主要布局溢出、重叠或占位错误。截图采集于本次联调；最后将工具状态文案澄清为历史调研核验，并将分页small属性改为size="small"以消除组件弃用提示，主体布局不变。

- [资源推荐桌面](screenshots/tool-recommend-desktop.png)
- [真实样例分析桌面](screenshots/research-analysis-real-desktop.png)
- [合成演示分析桌面](screenshots/research-analysis-demo-desktop.png)
- [资源推荐移动](screenshots/tool-recommend-mobile.png)
- [分析页移动](screenshots/research-analysis-mobile.png)
- [机器可读验证摘要](validation-results.json)

构建与测试原始日志在.local/backend-verified.log及portal-backend/target/surefire-reports，机器本地过程记录不作为业务数据提交。相关命令可按README、scripts/Verify.ps1与smoke-api.mjs复现。浏览器验证使用当前已安装Edge与本机Playwright运行时；没有给项目运行增加浏览器依赖。

## 未作出的验收承诺

未接入OpenLB批量文献、未确认cncbbank、未完成真实作者/机构/基金数据补全、未验证外部计算成功、未做大规模性能或公网生产部署验收。完整问题与下一步见[未完成项表](issues-and-limitations.md)。本次核心页面、SQL、应用启动和接口通信已有实测证据。


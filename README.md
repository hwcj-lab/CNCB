# 生物数据资源推荐与科研产出分析平台

基于项目内真实调研构建的前后端分离应用。包含资源与工具推荐、科研产出分析两个页面，可在 IntelliJ IDEA 中打开整个目录管理。

任务一、任务二现已按功能模块分目录管理，目录、依赖边界及验证说明见 [代码模块说明](docs/MODULE_STRUCTURE.md)。启动方式和现有接口不变。

**目录数据来自调研；科研统计默认展示3条有限真实样例。完整图表可切换“合成演示”，演示数据从独立SQL加载，不能当成中心真实成果。OpenLB与cncbbank尚未接入。**

## 技术与运行模式

- 后端：Java 17编译目标、Spring Boot 3.5.16、Maven 3.9.11、MyBatis-Plus 3.5.18、Lombok、Validation、springdoc 2.8.17。
- 前端：Vue 3.5.42、Vite 8.2.2、JavaScript、Vue Router 4.6.3、Element Plus 2.14.7、Axios 1.21.0、ECharts 6.1.0；具体以package-lock.json为准。
- MySQL 8是正式存储，当前默认profile为`mysql`并连接本机`bio_portal`。`demo` profile仍可显式启用内存H2，**不是MySQL验证的替代品**。
- Node建议24 LTS（满足 `^22.18.0 || >=24.12.0`）；JDK17。若系统java指向8，请在IDEA与启动脚本中选择17或兼容的21。

## 快速启动

Windows PowerShell在项目根目录打开两个终端。脚本不更改系统全局配置；缓存写在项目.local目录。

后端：

```powershell
# 首次需要可联网下载Maven/依赖；已有Maven可跳过引导。
.\scripts\Bootstrap-Maven.ps1
$env:DB_USERNAME='root'
$env:DB_PASSWORD='你的本机MySQL密码'
.\scripts\Start-Backend.ps1 -Profile mysql
```

前端：

```powershell
cd portal-frontend
npm ci
npm run dev
```

也可在根目录执行 `.\scripts\Start-Frontend.ps1`，缺依赖时会安装。后端默认[8080](http://localhost:8080/api/system/status)，前端[5173](http://localhost:5173/tool-recommend)，[Swagger](http://localhost:8080/swagger-ui/index.html)。

显式使用`-Profile demo`时会创建内存库，加载schema.sql、init-data.sql和demo-data.sql。重启后内存数据恢复初始状态；真实与演示记录按data_mode严格过滤。MySQL profile默认禁止DEMO查询。

## MySQL 8构建与初始化

使用独立空库。不要向已有业务库重复导入初始化脚本；schema无DROP，种子数据重复执行会唯一键冲突。所有脚本使用UTF-8。

```powershell
mysql -u root -p --default-character-set=utf8mb4
```

在MySQL客户端中依次执行（项目路径不同时修改绝对路径）：

为应用配置已有独立数据库账号（不要在源码中保存密码），然后启动：

```powershell
$env:DB_URL='jdbc:mysql://localhost:3306/bio_portal?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai'
$env:DB_USERNAME='你的应用账号'
$env:DB_PASSWORD='在本机输入密码，不提交到版本库'
.\scripts\Start-Backend.ps1 -Profile mysql
```

MySQL模式不自动建表、不自动插入种子、不主动爬取外部网站。需要演示时只在独立测试库导入demo-data.sql并设置 `DEMO_ENABLED=true`。

## 页面功能

- [资源与工具推荐](http://localhost:5173/tool-recommend)：四类核心数据切换、数据库搜索、资源详情、按输入类型约束的推荐工具、官方来源、链接核验状态。失效或未充分核实的入口禁用；“页面已核验”不等于“任务运行已验证”。
- [科研产出分析](http://localhost:5173/research-analysis)：真实/演示切换、年份与数据库筛选、指标卡、年度趋势、资源关联类别环图、机构/作者排名、基金统计、机构/人员年度趋势、来源文献分页与接口来源抽屉。
- 真实样例缺少机构、已消歧作者、基金时显示“— / 待接入”，不生成伪排名。没有提供专利、科技成果分析或完整学科主题分析。

## 项目目录

```text
portal-backend/   Maven后端；com.example.bioplatform
portal-frontend/  Vue前端；两个独立路由和复用图表组件
database/         正式结构、真实目录/有限样例种子、独立演示种子
research/         保留的前期调研，按任务分目录
docs/             架构、使用、数据库、API、流程、验证与问题表
scripts/          Windows启动、Maven引导、种子生成、联调检查
.local/           本机依赖缓存和验证数据，忽略提交
pom.xml           IDEA可导入的Maven聚合工程
```

## 文档

- [系统结构与设计](docs/project-structure.md)
- [使用说明](docs/user-guide.md)
- [数据库设计和扩展](docs/database-design.md)
- [API设计](docs/api-design.md)
- [开发与部署指南](docs/development-guide.md)
- [数据接入与业务流程](docs/workflows.md)
- [调研结论到实现的对应关系](docs/research-baseline.md)
- [验证记录](docs/validation-report.md)
- [未完成项与问题表](docs/issues-and-limitations.md)

## 验证

```powershell
.\scripts\Verify.ps1
# 后端与前端运行后：
node scripts/smoke-api.mjs
```

首次依赖下载需要互联网；下载受阻可使用本机Maven/IDEA缓存。验证记录区分H2、MySQL、浏览器和未验证的外部工具运行。调研维护入口为[research/README.md](research/README.md)。

正式使用前应完成真实数据许可/覆盖率核实、cncbbank字段契约、人员机构消歧与部署访问控制。本项目只提供只读业务API，未加入登录、管理写接口或外部采集任务。

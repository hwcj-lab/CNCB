# 开发与部署指南

## IntelliJ IDEA

1. 打开项目根目录，导入根pom.xml或portal-backend/pom.xml为Maven工程。
2. Project SDK与Language Level设为17，Maven Runner JRE也选17；确认不要使用系统默认Java 8。
3. Lombok如IDE显示访问器错误，启用Annotation Processing；使用Maven构建确认，不手写重复getter。
4. 运行com.example.bioplatform.BioPlatformApplication；默认demo profile无需MySQL。
5. 正式连接用Active profiles=mysql，通过Run Configuration环境变量设置DB_URL、DB_USERNAME、DB_PASSWORD。不把密码写进application.yml、.idea或文档。
6. 前端在IDEA Terminal执行npm ci、npm run dev；也可增加npm Run Configuration。

## Maven与Node

使用Maven 3.6.3+，项目wrapper固定3.9.11；Spring Boot 3.5要求Java17+。首选JDK17，编译release=17。原始Boot4/Java21/JPA脚手架已替换为Boot3/Java17/MyBatis-Plus，不保留多套启动类。

Windows受限环境若PowerShell的wrapper下载TLS失败，可运行scripts/Bootstrap-Maven.ps1：用Node从Maven Central下载ZIP并验证SHA512，解压到项目.local。不关闭证书校验，不更改系统Java或PATH注册表。启动脚本只设置当前进程环境与工作区缓存。

Node建议24 LTS，依赖由package-lock固定。修改package.json后npm install，其他机器用npm ci；不要混用多个包管理器。npm缓存权限不足时使用--cache ../.local/npm-cache。

## 本地启动与联调

- scripts/Start-Backend.ps1 -Profile demo：找到JDK17+与Maven，设置项目内缓存/临时目录，启动8080。
- scripts/Start-Frontend.ps1：启动5173；Vite将/api转发到127.0.0.1:8080。
- 自定义后端端口：设置SERVER_PORT；前端开发代理目标通过VITE_API_PROXY_TARGET设置。构建部署API基址用VITE_API_BASE_URL，默认/api。
- 默认CORS仅localhost:5173与127.0.0.1:5173，可通过CORS_ORIGINS逗号列表调整，不使用任意来源+凭证。
- 源码修改后后端重启；前端Vite热更新。SQL初始化修改不会自动更新已运行H2，需重启后端。

## 真实与演示模式

运行profile和数据mode是两层开关。H2 demo profile载入全部种子，默认页面查看REAL；DEMO只在用户主动切换时获取。MySQL profile不自动初始化且默认DEMO_ENABLED=false。只在独立演示库导入demo-data.sql并显式开启。

不能在网络失败时自动退回演示数据；Axios错误显示重试。页面切换模式或筛选会清空旧结果，响应序号防止迟到的演示响应覆盖真实模式。

## 开发顺序

新增数据库资源→核验URL/输入/证据→写增量数据和关联→API验证→页面验证。新增统计维度→先取得真实字段并定义覆盖率→写参数化聚合→测试去重/模式/角色→复用图表组件。不要为填图表编造机构或基金。

scripts/build-research-seeds.mjs用于重新生成初始SQL，不是增量迁移；会覆盖init-data.sql与demo-data.sql，请先评审调研与映射，再运行。正式数据库变更放database/migrations，按版本记录，先备份并在测试库验证。

## 测试

- 后端：mvn test，SpringBoot+MockMvc真实执行SQL，覆盖目录、类型约束、REAL/DEMO隔离、多库去重、未知身份、角色过滤、年份校验、Swagger与禁演示开关。
- 前端：npm test运行纯图表转换与安全外链检查；npm run build验证Vue/CSS/模块打包。
- 联调：启动后node scripts/smoke-api.mjs检查API及Vite代理；浏览器检查两个路由、筛选、图表、移动布局和控制台。
- scripts/Verify.ps1串行执行本地测试与构建。验证结论、环境和限制见validation-report.md。

## 部署

后端mvn package后使用java -jar portal-backend/target/portal-backend-1.0.0.jar --spring.profiles.active=mysql。前端npm run build输出dist，交由静态服务器托管。Vite开发服务器不用于正式生产。

Nginx示意配置（按实际目录、域名与TLS配置调整）：

```nginx
server {
    listen 80;
    root /srv/bio-portal/dist;
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
    location / { try_files $uri $uri/ /index.html; }
}
```

proxy_pass保留/api前缀；SPA回退保证刷新/research-analysis仍可访问。此项目没有生产登录/管理权限，部署到公共网络前应使用组织网关/访问控制，并限制Swagger暴露。不要将H2演示作为正式持久化存储。

## 常见问题

| 问题 | 定位/处理 |
| --- | --- |
| java版本8、UnsupportedClassVersionError | 检查JAVA_HOME与IDEA Maven Runner，使用17+ |
| mvnw TLS失败 | 本地Maven或Bootstrap-Maven；不跳过证书检查 |
| npm缓存EPERM | 使用项目.local/npm-cache；不要要求管理员改系统权限 |
| 数据库连接失败 | 检查MySQL服务、端口、库和账号授权，DB_URL含时区/UTF-8 |
| 初始化重复键/索引冲突 | 基线只对新库一次执行；已有库使用增量迁移，不清空旧库 |
| Unknown column / table missing | 检查实际选库和schema版本，不临时在Controller硬编码数据 |
| 图表为空/值为— | 查看mode、过滤范围和coverage；真实缺失正常显示PENDING |
| 外链打不开 | 真实链接不等于当前可达，查看调研状态与网站登录要求 |
| 浏览器刷新404 | 配置SPA try_files回退并保留/api代理 |
| ECharts/Element Plus分包体积提示 | 当前按路由和组件包拆分；警告不是编译失败，大规模上线前可继续按需引入UI组件 |

依赖兼容依据：[Spring Boot 3.5](https://docs.spring.io/spring-boot/3.5/system-requirements.html)、[MyBatis-Plus Boot3](https://baomidou.com/getting-started/)、[springdoc兼容矩阵](https://springdoc.org/v2/)、[Vite](https://vite.dev/guide/)。

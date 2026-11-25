import fs from "node:fs";
import path from "node:path";

const root = path.resolve(import.meta.dirname, "..");
const outputDir = path.join(root, "docs", "apipost");
fs.mkdirSync(outputDir, { recursive: true });

const absoluteUrl = (url) => `http://localhost:8080${url}`
  .replaceAll("{{databaseId}}", "1")
  .replaceAll("{{toolId}}", "1")
  .replaceAll("{{institutionId}}", "101")
  .replaceAll("{{authorId}}", "101");

const okTest = [
  "pm.test('HTTP 200', () => pm.response.to.have.status(200));",
  "const body = pm.response.json();",
  "pm.test('统一响应code为OK', () => pm.expect(body.code).to.eql('OK'));",
  "pm.test('响应包含data和timestamp', () => { pm.expect(body).to.have.property('data'); pm.expect(body).to.have.property('timestamp'); });",
];
const listTest = [...okTest, "pm.test('data为数组', () => pm.expect(pm.response.json().data).to.be.an('array'));" ];
const errorTest = (status) => [
  `pm.test('HTTP ${status}', () => pm.response.to.have.status(${status}));`,
  "const body = pm.response.json();",
  "pm.test('错误响应不返回业务数据', () => pm.expect(body.data).to.eql(null));",
  "pm.test('包含错误码和消息', () => { pm.expect(body.code).to.be.a('string').and.not.empty; pm.expect(body.message).to.be.a('string').and.not.empty; });",
];
const request = (name, url, description, tests = okTest, method = "GET", body) => ({
  name,
  request: {
    method,
    header: [{ key: "Accept", value: "application/json" }, ...(body ? [{ key: "Content-Type", value: "application/json" }] : [])],
    url: absoluteUrl(url),
    description,
    ...(body ? { body: { mode: "raw", raw: JSON.stringify(body, null, 2), options: { raw: { language: "json" } } } } : {}),
  },
  event: [{ listen: "test", script: { type: "text/javascript", exec: tests } }],
});
const folder = (name, items, description) => ({ name, description, item: items });

const collection = {
  info: {
    _postman_id: "d31cf17b-7ca2-4f68-b4c4-bioportal2026",
    name: "生物数据资源推荐与科研产出分析平台 API",
    description: "ApiPost可直接导入的Postman Collection v2.1。全部请求使用http://localhost:8080完整地址，不依赖环境变量。",
    schema: "https://schema.getpostman.com/json/collection/v2.1.0/collection.json",
  },
  item: [
    folder("01 系统与文档", [
      request("系统状态", "/api/system/status", "验证Spring Boot和数据库连接，并返回数据库、资源和工具数量。", [...okTest, "pm.test('包含数据库统计', () => { const d=pm.response.json().data; pm.expect(d).to.have.property('databaseCount'); pm.expect(d).to.have.property('toolEntryCount'); });"]),
      request("OpenAPI JSON", "/v3/api-docs", "Swagger/OpenAPI原始文档。", ["pm.test('HTTP 200', () => pm.response.to.have.status(200));", "pm.test('OpenAPI 3文档', () => pm.expect(pm.response.json().openapi).to.match(/^3\\./));"]),
    ]),
    folder("02 数据资源与工具", [
      request("推荐页总接口", "/api/resource-recommendations", "返回存在关联数据库的可见数据类型以及每个数据库的工具；空分类不返回。", [...listTest, "pm.test('每组均包含非空databases', () => pm.response.json().data.forEach(x => { pm.expect(x).to.have.property('dataType'); pm.expect(x.databases).to.be.an('array').and.not.empty; }));"]),
      request("推荐页后端关键词搜索", "/api/resource-recommendations?keyword=GenBase", "后端按数据库名称或中英文标题过滤，仍按类型分组，并省略空分类。", [...listTest, "pm.test('只返回DNA中的GenBase', () => { const data=pm.response.json().data; pm.expect(data).to.have.length(1); pm.expect(data[0].dataType.code).to.eql('DNA'); pm.expect(data[0].databases[0].name).to.eql('GenBase'); });"]),
      request("根据搜索结果推荐", "/api/resource-recommendations/search-results", "请求体直接使用CNCB搜索响应的result.databases数组；按官方db_id匹配，只返回有命中数据库的分类。", [...listTest, "pm.test('GenBase只命中DNA分类', () => { const data=pm.response.json().data; pm.expect(data).to.have.length(1); pm.expect(data[0].dataType.code).to.eql('DNA'); });"], "POST", [{ id: "nucbank_nuc2", name: "GenBase", description: "Genetic Sequence Database", hitCount: 1, categories: ["Sequence"] }]),
      request("查询数据类型", "/api/data-types", "返回可见且启用的数据类型。", listTest),
      request("查询全部数据库", "/api/databases", "返回全部可见数据库及其类型和工具关系。", listTest),
      request("按DNA类型查询数据库", "/api/databases?dataType=DNA", "只返回database_data_type中关联DNA的数据资源。", listTest),
      request("按关键词查询数据库", "/api/databases?keyword=Genome", "按短名称、中英文标题模糊查询。", listTest),
      request("数据库详情", "/api/databases/{{databaseId}}", "完整地址内使用示例databaseId=1，可直接修改URL中的ID。", okTest),
      request("数据库关联工具", "/api/databases/{{databaseId}}/tools", "通过database_tool查询工具，不再直接关联数据类型。", listTest),
      request("工具详情", "/api/tools/{{toolId}}", "完整地址内使用示例toolId=1，可直接修改URL中的ID。", okTest),
    ]),
    folder("03 OpenLB文献采集", [
      request("查询OpenLB采集状态", "/api/collection/openlb/status", "查看后台任务是否运行、当前限速间隔和各持久化状态数量。"),
      request("导入OpenLB PMID CSV", "/api/collection/openlb/tasks/import-csv", "从服务端docs/openlb_pmid.csv导入去重任务；重复PMID不覆盖。", okTest, "POST"),
      request("启动OpenLB后台采集", "/api/collection/openlb/jobs/start", "立即返回，不等待全量采集完成；默认3并发、350ms最小间隔。", okTest, "POST", { concurrency: 3, batchSize: 60, minDelayMs: 350, maxAttempts: 3, stopAfterConsecutiveThrottles: 5 }),
      request("停止OpenLB采集", "/api/collection/openlb/jobs/stop", "请求安全停止；未完成记录保留为可续传状态。", okTest, "POST"),
      request("重试OpenLB失败任务", "/api/collection/openlb/tasks/retry-failed", "仅将FAILED重置为PENDING，不重复SUCCESS或EMPTY。", okTest, "POST"),
    ]),
    folder("04 科研产出分析 REAL", [
      request("科研概览", "/api/research/summary?mode=REAL", "真实数据模式概览；缺失指标允许value为null。"),
      request("年度文献趋势", "/api/research/annual?mode=REAL", "按发表年份统计。"),
      request("年度趋势带年份范围", "/api/research/annual?mode=REAL&fromYear=2020&toYear=2026", "验证年份范围筛选。"),
      request("资源关联类别分布", "/api/research/topics?mode=REAL", "按数据库的数据类型关系统计，不等同学科主题。"),
      request("核心机构", "/api/research/institutions?mode=REAL&limit=10", "只统计论文署名机构。"),
      request("核心作者", "/api/research/authors?mode=REAL&limit=10", "只统计已消歧作者。"),
      request("基金项目", "/api/research/funds?mode=REAL&limit=10", "只统计论文资助关系。"),
      request("机构年度趋势", "/api/research/institutions/{{institutionId}}/trend?mode=REAL", "如果真实库不存在该机构，预期404；请把institutionId替换为真实ID。", ["pm.test('HTTP 200或404', () => pm.expect(pm.response.code).to.be.oneOf([200,404]));"]),
      request("作者年度趋势", "/api/research/authors/{{authorId}}/trend?mode=REAL", "如果真实库不存在该作者，预期404；请把authorId替换为真实ID。", ["pm.test('HTTP 200或404', () => pm.expect(pm.response.code).to.be.oneOf([200,404]));"]),
      request("分页查询文献", "/api/research/publications?mode=REAL&page=1&size=10", "页码从1开始，size最大50。"),
      request("官方来源接口登记", "/api/research/sources", "只返回登记表，不调用外部接口。", listTest),
    ]),
    folder("05 参数与异常", [
      request("未知数据类型-400", "/api/databases?dataType=UNKNOWN", "验证UNKNOWN_DATA_TYPE。", errorTest(400)),
      request("数据库不存在-404", "/api/databases/999999/tools", "验证DATABASE_NOT_FOUND。", errorTest(404)),
      request("工具不存在-404", "/api/tools/999999", "验证TOOL_NOT_FOUND。", errorTest(404)),
      request("年份范围错误-400", "/api/research/annual?mode=REAL&fromYear=2026&toYear=2020", "开始年份不能晚于结束年份。", errorTest(400)),
      request("年份越界-400", "/api/research/annual?mode=REAL&fromYear=1499", "年份允许范围1500到2200。", errorTest(400)),
      request("分页大小超限-400", "/api/research/publications?mode=REAL&page=1&size=51", "size最大50。", errorTest(400)),
      request("非法数据模式-400", "/api/research/summary?mode=UNKNOWN", "mode只允许REAL或DEMO。", errorTest(400)),
      request("正式环境访问DEMO-403", "/api/research/summary?mode=DEMO", "mysql profile默认demo-enabled=false；如果主动启用了DEMO，本用例会返回200。", ["pm.test('正式环境应为403；显式启用演示时允许200', () => pm.expect(pm.response.code).to.be.oneOf([200,403]));"]),
    ]),
  ],
};

fs.writeFileSync(path.join(outputDir, "bio-portal-api.json"), JSON.stringify(collection, null, 2));
console.log(`generated one collection with ${collection.item.reduce((n, f) => n + f.item.length, 0)} requests and absolute URLs`);

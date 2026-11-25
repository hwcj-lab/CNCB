# 公开接口清单与逐库检查

调研日期：2026-11-06；每行访问日期均为本日。接口分为已验证JSON、官方文档确认但未验证、未找到。**本次没有获得可用于全库文献采集的分页接口。** “未找到”仅指本轮公开检索范围，不代表数据库没有内部接口。

## 已发现18个文档化接口模式

表中花括号仅表示路径占位符，不是实际URL参数。认证：官方文档未说明统一认证要求；A02/A03/A13/A17本次网页读取无需提供凭证。其他接口匿名调用能力未验证。限流/配额：除A18的单次条数约束外均未找到明确说明；不推断无限调用。API可读不代表可商业再分发。

| ID | 接口名称 | 请求方式 | 请求地址 | 参数 | 返回字段/内容 | 验证状态 | 官方文档 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| A01 | GWH Genome | GET（按官方可访问URL） | `https://ngdc.cncb.ac.cn/gwh/api/public/genome/{genome_id}` | 路径 genome_id；官方例1 | id、organism、taxId、representativeAssemblyAccession（文档） | 官方文档确认；请求被工具判不可访问 | [文档](https://ngdc.cncb.ac.cn/gwh/api_documents) |
| A02 | GWH Assembly | GET | `https://ngdc.cncb.ac.cn/gwh/api/public/assembly/{assembly_accession}` | 路径 assembly_accession；例GWHAAAA00000000 | assemblyAccession、bioprojectAccession、biosampleAccession、submitterOrganization、publication、publicationLink、ftpPathDna/Protein等 | 已验证application/json | [文档](https://ngdc.cncb.ac.cn/gwh/api_documents) |
| A03 | GWH BioProject | GET | `https://ngdc.cncb.ac.cn/gwh/api/public/bioProject/{accession}` | 路径项目号；例PRJCA000437 | prjAccession、listPublication[].pubmedId/doi/year/authorList、listGrants、submitter、taxons | 已验证JSON；基金样本为空 | [文档](https://ngdc.cncb.ac.cn/gwh/api_documents) |
| A04 | GWH BioSample | GET（按官方示例） | `https://ngdc.cncb.ac.cn/gwh/api/public/bioSample/{accession}` | 路径样本号；例SAMC012932 | accession、name、sampleAttribute、message（文档） | 官方文档确认；请求超时 | [文档](https://ngdc.cncb.ac.cn/gwh/api_documents) |
| A05 | GenBase FASTA | GET（按官方示例） | `https://ngdc.cncb.ac.cn/genbase/api/file/fasta` | 查询acc:string；例C_AA004835.1 | 文档：byte[]，FASTA文件 | 官方文档确认；本次工具返回text/html、1行，未取得有效FASTA | [文档](https://ngdc.cncb.ac.cn/genbase/restapihelp) |
| A06 | GenBase GBF | GET（按官方示例） | `https://ngdc.cncb.ac.cn/genbase/api/file/gbf` | 查询acc:string；同上 | 文档：byte[]，GBF文件；参考文献字段需读取文件确认 | 官方文档确认；未实际请求文件 | [文档](https://ngdc.cncb.ac.cn/genbase/restapihelp) |
| A07 | EWAS文献 | GET | `https://ngdc.cncb.ac.cn/ewas/rest/publication` | 查询pmid，必填；例29535343 | 文献及相关研究；JSON键名待验证 | 官方文档确认；示例请求不可访问 | [文档](https://ngdc.cncb.ac.cn/ewas/api) |
| A08 | EWAS研究 | GET | `https://ngdc.cncb.ac.cn/ewas/rest/study` | 查询studyId，必填；例ES00033 | 研究、关联和队列；字段待验证 | 官方文档确认；未请求 | [文档](https://ngdc.cncb.ac.cn/ewas/api) |
| A09 | EWAS探针 | GET | `https://ngdc.cncb.ac.cn/ewas/rest/probe` | 查询probeId，必填；例cg05575921 | 探针、关联和转录本；字段待验证 | 官方文档确认；未请求 | [文档](https://ngdc.cncb.ac.cn/ewas/api) |
| A10 | EWAS基因 | GET | `https://ngdc.cncb.ac.cn/ewas/rest/gene` | 查询geneSymbol，必填；例A1BG-AS1 | 基因相关关联；字段待验证 | 官方文档确认；未请求 | [文档](https://ngdc.cncb.ac.cn/ewas/api) |
| A11 | EWAS区间 | GET | `https://ngdc.cncb.ac.cn/ewas/rest/pos` | 查询chr、start、end，均必填；例1、1、100000 | 区间关联；字段待验证 | 官方文档确认；未请求 | [文档](https://ngdc.cncb.ac.cn/ewas/api) |
| A12 | EWAS富集 | POST | `https://ngdc.cncb.ac.cn/ewas/rest/enrichment` | input:String 探针换行分隔；mode:0所有性状/1自定义；mode=1时traits:List；请求编码未说明 | 富集结果；结构待验证 | 官方文档确认；未提交 | [文档](https://ngdc.cncb.ac.cn/ewas/api) |
| A13 | ICG文献 | GET | `https://ngdc.cncb.ac.cn/icg/api/json/publication/{pmid}` | 路径pmid；官方例24810581 | code、message、result.pmid/title/author/journal/pubYear/abst等 | 已验证code=200、message=Success!、application/json | [文档](https://ngdc.cncb.ac.cn/icg/api) |
| A14 | ICG物种 | GET | `https://ngdc.cncb.ac.cn/icg/api/json/species/{species_accession}` | 路径物种登录号；例ICG00115 | 返回键名待验证 | 官方文档确认；未请求 | [文档](https://ngdc.cncb.ac.cn/icg/api) |
| A15 | ICG研究 | GET | `https://ngdc.cncb.ac.cn/icg/api/json/project/{project_accession}` | 路径研究登录号；例ICG00115-Pro-1 | 返回键名待验证 | 官方文档确认；未请求 | [文档](https://ngdc.cncb.ac.cn/icg/api) |
| A16 | ICG基因 | GET | `https://ngdc.cncb.ac.cn/icg/api/json/gene/{gene_accession}` | 路径基因登录号；例ICG00115-Pro-1-EF1a | 返回键名待验证 | 官方文档确认；未请求 | [文档](https://ngdc.cncb.ac.cn/icg/api) |
| A17 | IC4R基因 | GET | `https://ngdc.cncb.ac.cn/ic4r/api/json/gene/{LOCUS}` | 路径LOCUS；例IC4R-OSJ01G000600 | locus、synonym、description、source、type、chromosome、strand、location、geneLength | 已验证application/json；jbrowse与symbol为null | [文档](https://ngdc.cncb.ac.cn/ic4r/api) |
| A18 | RCoV19基因组检索 | GET | `https://ngdc.cncb.ac.cn/ncov/api/es/genome/query` | q、start、length等；官方要求length<10000；见下 | 具体JSON字段未获取 | 官方文档确认；start=0&length=1请求不可访问 | [文档](https://ngdc.cncb.ac.cn/ncov/genome/search) |

### RCoV19官方参数范围

来源：[基因组检索页面](https://ngdc.cncb.ac.cn/ncov/genome/search)。官方列出的参数还有accession、country、province、host、source、minCollectDate、maxCollectDate、minSubmitDate、maxSubmitDate、qc、complete、minLength、maxLength、ns、ds、lineage、whoLabel。日期例为2023-01-01，qc为0/1，complete为Complete/Partial。未确认参数是否可全部组合、必填项、返回总数字段或限流，不能直接据此编写生产采集程序。

### 文档与实际返回的重要差异

- GWH BioSample文档的URL文字误写为`bioProject`，但示例链接是`bioSample/SAMC012932`。本表保留示例路径，实测超时，需维护方确认。
- GWH BioProject文档示例的listPublication为空，但本次实际JSON非空。因此字段存在不代表数据填充率，缓存样本也不能当全库契约。
- GWH项目PRJCA000437示例实际含PMID 34939160、32070270等，authorList有空值；listGrants为空。与项目题材不完全一致的关联须人工抽检，不自动剔除或视为真实数据使用关系。
- GenBase FASTA请求本次工具标作text/html且无可识别FASTA正文，不能记成有效文件获取；GBF仅文档确认。

## 23项优先资源检查矩阵

独立API与GWH代理接口分开记录。表中的“机构”指明所属角色；不能以提交人代替论文作者或用库建设基金代替论文基金。公开网页检索仅是本轮初查，完整覆盖和内部接口仍需维护方提供。

| 数据库ID | 数据库 | 公开API检查结果/方法 | PMID或文献关联 | 机构 | 作者 | 基金 | 批量数据下载 | 来源 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| D01 | BioProject | 本库独立API未找到；GWH代理A03仅覆盖其关联项目，不是全库保证 | 文档PMID/DOI；A03实证 | 项目提交者机构；非论文机构 | A03部分作者字符串 | 文档确认；样本空 | 待验证 | [来源](https://ngdc.cncb.ac.cn/databasecommons/database/id/6010) |
| D02 | BioSample | 独立API未找到；GWH代理A04有文档且超时 | 需经项目关联 | 待验证 | 待验证 | 待验证 | 待验证 | [来源](https://ngdc.cncb.ac.cn/gsub/) |
| D03 | GenBase | A05–A06；acc下载；未找到PMID反查 | 文档确认收集出版资料；程序化关联待验证 | 待验证 | 待验证 | 待验证 | 官方确认网页多选及FTP/API；文件未验证 | [来源](https://ngdc.cncb.ac.cn/genbase/document) |
| D04 | GSA | 本库官网/官方索引及站内API检索；未找到可确认的公开文献API；非不存在结论 | 待验证 | 待验证 | 待验证 | 待验证 | 待验证 | [来源](https://ngdc.cncb.ac.cn/gsub/) |
| D05 | GSA-Human | 本库官网/官方索引及站内API检索；未找到可确认的公开文献API；非不存在结论 | 待验证 | 待验证 | 待验证 | 待验证 | 待验证 | [来源](https://ngdc.cncb.ac.cn/gsub/) |
| D06 | OMIX | 本库官网/官方索引及站内API检索；未找到可确认的公开文献API；非不存在结论 | 待验证 | 待验证 | 待验证 | 待验证 | 待验证 | [来源](https://ngdc.cncb.ac.cn/omix/methods) |
| D07 | GWH | A01–A04；组装/项目号查询，未找到PMID反查参数 | 项目→文献已验证；PMID反查未证实 | 提交机构已验证（非论文机构） | 项目关联文献部分authorList | listGrants样本空 | 文档列DNA/蛋白文件路径；未下载 | [来源](https://ngdc.cncb.ac.cn/gwh/api_documents) |
| D08 | GVM | 本库官网/官方索引及站内API检索；未找到可确认的公开文献API；非不存在结论 | 待验证 | 待验证 | 待验证 | 待验证 | 待验证 | [来源](https://ngdc.cncb.ac.cn/gvm/) |
| D09 | GEN | 本库官网/官方索引及站内API检索；未找到可确认的公开文献API；非不存在结论 | 文档PMID/DOI及Publications入口 | 待验证 | 待验证 | 待验证 | 首页Download入口；未下载 | [来源](https://ngdc.cncb.ac.cn/gen/documentation) |
| D10 | MethBank | 本库官网/官方索引及站内API检索；未找到可确认的公开文献API；非不存在结论 | Projects表PMID列；未确认API | 待验证 | 待验证 | 待验证 | Meta Data Download按钮和Download入口；未下载 | [来源](https://ngdc.cncb.ac.cn/methbank/projects) |
| D11 | BioCode | 本库官网/官方索引及站内API检索；未找到可确认的公开文献API；非不存在结论 | 待验证 | 待验证 | 待验证 | 待验证 | 待验证 | [来源](https://ngdc.cncb.ac.cn/biocode/) |
| D12 | Database Commons | 本库官网/官方索引及站内API检索；未找到可确认的公开文献API；非不存在结论 | 数据库介绍文献；不是全部数据使用论文 | 资源建设机构 | 资源联系人/介绍文献作者；需区分 | 资源资助号（GenBase详情例2021YFF0703700） | 待验证 | [来源](https://ngdc.cncb.ac.cn/databasecommons/database/id/8197) |
| D13 | OpenLB | 公开API、cncbbank筛选语法均未找到；页面访问失败 | 官方索引见PMID详情与资源链接 | 官方索引见作者机构 | 官方资料确认 | 官方索引见Grants | 批量导出未确认 | [来源](https://ngdc.cncb.ac.cn/openlb/publication/OLB-PM-32460027) |
| D14 | Taxonomy | 本库官网/官方索引及站内API检索；未找到可确认的公开文献API；非不存在结论 | 待验证 | 待验证 | 待验证 | 待验证 | 待验证 | [来源](https://ngdc.cncb.ac.cn/taxonomy/index_new) |
| D15 | Cell Taxonomy | 本库官网/官方索引及站内API检索；未找到可确认的公开文献API；非不存在结论 | 待验证 | 待验证 | 待验证 | 待验证 | 待验证 | [来源](https://ngdc.cncb.ac.cn/celltaxonomy/species) |
| D16 | LncBook | 本库官网/官方索引及站内API检索；未找到可确认的公开文献API；非不存在结论 | 待验证 | 待验证 | 待验证 | 待验证 | Downloads入口及帮助资料；未下载 | [来源](https://ngdc.cncb.ac.cn/lncbook/) |
| D17 | EWAS Atlas | A07–A12；pmid→文献及研究，未成功读取 | 官方文档确认，待验证 | 待验证 | 待验证 | 待验证 | 未验证 | [来源](https://ngdc.cncb.ac.cn/ewas/api) |
| D18 | GWAS Atlas | 本库官网/官方索引及站内API检索；未找到可确认的公开文献API；非不存在结论 | 待验证 | 待验证 | 待验证 | 待验证 | 待验证 | [来源](https://ngdc.cncb.ac.cn/gwas/documentation) |
| D19 | BrainBase | 本库官网/官方索引及站内API检索；未找到可确认的公开文献API；非不存在结论 | FAQ确认PMID/Source链接 | 待验证 | 待验证 | 待验证 | 官方FAQ列FTP下载；未下载 | [来源](https://ngdc.cncb.ac.cn/brainbase/faq) |
| D20 | LncExpDB | 本库官网/官方索引及站内API检索；未找到可确认的公开文献API；非不存在结论 | 帮助资料列项目PMID | 待验证 | 待验证 | 待验证 | 官方downloads页面列压缩包；未下载 | [来源](https://ngdc.cncb.ac.cn/lncexpdb/help) |
| D21 | RCoV19 | A18；基因组筛选；未确认PMID查询 | 待验证 | 待验证 | 待验证 | 待验证 | 官网download入口；未下载 | [来源](https://ngdc.cncb.ac.cn/ncov/genome/search) |
| D22 | ICG | A13–A16；PMID→文献 | 已验证 | 待验证 | author字符串已验证 | 待验证 | 批量API未确认 | [来源](https://ngdc.cncb.ac.cn/icg/api) |
| D23 | IC4R | A17；LOCUS查基因，未确认PMID反查 | 待验证 | 待验证 | 待验证 | 待验证 | 官网Download入口；未下载 | [来源](https://ngdc.cncb.ac.cn/ic4r/api) |

## 实际请求记录

| 请求URL | 结果 | 状态 |
| --- | --- | --- |
| https://ngdc.cncb.ac.cn/gwh/api/public/assembly/GWHAAAA00000000 | 可读application/json | 已验证 |
| https://ngdc.cncb.ac.cn/gwh/api/public/bioProject/PRJCA000437 | 可读application/json，message=SUCCESS | 已验证 |
| https://ngdc.cncb.ac.cn/icg/api/json/publication/24810581 | 可读JSON，code=200、Success! | 已验证 |
| https://ngdc.cncb.ac.cn/ic4r/api/json/gene/IC4R-OSJ01G000600 | 可读JSON，含基因注释；不是PMID接口 | 已验证 |
| https://ngdc.cncb.ac.cn/gwh/api/public/genome/1 | 网页工具不可访问；不是确认服务端404 | 待人工核实 |
| https://ngdc.cncb.ac.cn/gwh/api/public/bioSample/SAMC012932 | Timeout | 待人工核实 |
| https://ngdc.cncb.ac.cn/ewas/rest/publication?pmid=29535343 | 网页工具不可访问 | 待人工核实 |
| https://ngdc.cncb.ac.cn/genbase/api/file/fasta?acc=C_AA004835.1 | 工具返回text/html、1行，未读到有效FASTA | 待人工核实 |
| https://ngdc.cncb.ac.cn/ncov/api/es/genome/query?start=0&length=1 | 网页工具不可访问 | 待人工核实 |

网络验证路径：使用网页读取工具访问上述官方URL；它可能使用缓存/代理，网页结果带有抓取时间，因此“已验证”只表示本次工具读到指定格式与字段，不能视为本机curl、浏览器跨域或后端网络已连通。未记录工具未提供的原站HTTP状态、耗时或响应头。本机PowerShell对导航的一次HTTPS请求还出现TLS Authentication failed，未绕过证书校验。

## 未完成范围与所需资料

数据库清单末尾其他导航线索尚未逐库检查API，不计为完成。需要中心提供资源目录导出及API目录/维护人，才能核查全量119条混合资源。优先补齐：OpenLB字段与文献批量导出；EWAS文献接口可达性；GWH PMID反查与分页；ICG文献→研究关联和批量文献列表；限流、版本、更新与使用许可。


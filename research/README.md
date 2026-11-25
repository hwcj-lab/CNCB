# 生物数据资源推荐与科研产出分析平台前期调研

调研日期：**2026-11-06（Asia/Shanghai）**。本轮进行了互联网搜索、官方页面读取、公开接口示例读取及附件只读提取，未创建Spring Boot/Vue/MySQL项目、数据库表或业务SQL。

## 阅读入口

| 文档 | 内容 |
| --- | --- |
| [数据库清单](task1_tool_recommend/database_inventory.md) | 23项优先资源的官网、类型、四类数据与机构/作者/基金线索；其他导航资源待查列表 |
| [工具清单](task1_tool_recommend/tool_inventory.md) | BIT 148项目录、32项重点工具/服务入口、六类需求覆盖及运行验证边界 |
| [数据库与工具映射](task1_tool_recommend/database_tool_mapping.md) | 可供推荐页面内容评审的16条候选映射；原生关系与跨库推荐建议分开 |
| [文献来源](task2_research_analysis/literature_source_inventory.md) | OpenLB入口、检索、作者机构基金样例、cncbbank核实结果和替代来源 |
| [接口清单](task2_research_analysis/api_inventory.md) | 18个官方文档化接口模式、23项优先资源初查矩阵、实际请求记录 |
| [调研数据模型](task2_research_analysis/research_data_model.md) | 概念实体、真实源字段与建议业务属性的区别、质量和去重规则 |
| [分析可行性](task2_research_analysis/research_analysis_feasibility.md) | 七项指标逐项条件、缺口、偏差和下一阶段先后顺序 |

## 本轮结论及统计口径

1. **数据库：重点整理23项资源。** 官网导航还有大量其他资源线索，已另列待查目录；没有把工具平台、合作库、DMS镜像或全球目录收录项全部算作中心数据库。导航索引“119 entries”是混合资源条数，直接页面仅有模板，未完成全量独立数据库核验。
2. **工具：BIT首页目录148项，完整名称按10类整理；另有32项重点工具/服务入口记录。** 两者有重叠，不能相加；32中含分类浏览、帮助和分析入口，不是32个已运行在线工具。没有提交计算任务，成功运行数量未知。
3. **四类数据：** DNA序列优先GenBase、GWH，GSA提供原始reads；蛋白序列已确认GenBase和GWH文件路径；文献优先OpenLB，ICG/GWH有实际JSON，GEN/MethBank/LncExpDB/BrainBase有文献关联线索；物种优先Taxonomy，并有BioProject/BioSample/GWH/GEN/MethBank物种元数据。数据格式和输入兼容性需逐记录验证。
4. **OpenLB：** 重试能读取首页/部分页面框架，官方索引可看到机构、基金及GSA关联样例；未获得可用批量文献数据或cncbbank非空清单。高级检索和指定详情有445/超时，部分可读页面仅模板。
5. **cncbbank：未确认。** 未找到官方字段定义、类型、空值筛选语法或原始非空样例。不能用“Links to CNCB-NGDC Resources”区块直接代替字段定义。
6. **公开接口：** GWH（Assembly、BioProject）、ICG（PMID文献）、IC4R（gene）共4个示例读取到有效JSON。GenBase、EWAS Atlas、RCoV19有官方接口文档，但所测试下载/查询未获可验证有效数据；GWH Genome/BioSample也未成功。后者不能计为本轮实测可用。
7. **分析：** 年度趋势和资源关联类别分布可先做有限样本验证；完整中心成果、真实学科主题、可靠机构/作者/基金排名及人员机构长期趋势尚缺关键数据。不得用提交机构冒充论文机构，或将资源类别叫作已完成的研究主题分析。
8. **人工介入：** 优先需要OpenLB字段字典与授权导出、数据库资源ID映射、接口维护方说明及普通浏览器的工具任务验证。具体资料清单见下。
9. **下一步：** 先完成OpenLB/cncbbank数据契约与代表性样本验证，确定成果总体与关联语义，再评审逻辑模型及接口；随后进入开发。

关键官方来源：[中心资源门户](https://www.cncb.ac.cn/)、[NGDC导航](https://ngdc.cncb.ac.cn/databases)、[BIT](https://ngdc.cncb.ac.cn/bit/)、[GWH API](https://ngdc.cncb.ac.cn/gwh/api_documents)、[ICG API](https://ngdc.cncb.ac.cn/icg/api)、[IC4R API](https://ngdc.cncb.ac.cn/ic4r/api)、[OpenLB](https://ngdc.cncb.ac.cn/openlb)。逐项来源与日期在各清单中。

## 验证状态定义

| 状态 | 本报告含义 | 不能推出 |
| --- | --- | --- |
| 已验证 | 本次工具读到页面具体内容、配置表单或有效JSON；注明验证对象 | 所有功能、浏览器交互、服务端网络、全量字段或生产可用已验证 |
| 官方文档确认 | 官方帮助/API说明等明确记载能力；实际请求未成功或未执行 | 接口目前一定可调用、字段一定非空 |
| 官方索引确认 | 搜索引擎返回官方URL的内容；直接页面失败/模板化或未完整读取 | 本次直接读到了真实数据、完成页面或批量调研 |
| 待人工核实 / 待验证 | 动态模板、认证未知、网络失败、缺样本、未实际运行 | 服务不存在或无权限可访问 |
| 未找到 | 已做本轮指定范围的公开查找，没有找到足够证据 | 全网不存在、维护方未提供内部API |
| 设计建议 / 推测 | 为本项目提出的映射、模型、统计处理，或明确列出的候选解释 | 官方字段、API参数、已建立数据关联 |

为避免含混，搜索索引单列为“官方索引确认”，不升级为直接实测。所有文档表格的统一访问日为2026-11-06；来源网页的抓取时间可能早于访问日。网页工具可能经代理/缓存，结果不是本地端到端请求日志。仅记录工具实际暴露的错误，不猜测原站HTTP状态。

## 访问失败与不足记录

| 对象/URL | 本轮观察 | 状态及影响 |
| --- | --- | --- |
| [NGDC根主页](https://ngdc.cncb.ac.cn/)及[英文入口](https://ngdc.cncb.ac.cn/?lang=en) | 根页一次超时；英文页可读，但资源区有模板；中文搜索索引有目录 | 官方导航部分可读，未解析出完整实时全库清单 |
| [数据库导航](https://ngdc.cncb.ac.cn/databases) | 直接读取只见{{r.shortName}}等模板；索引显示119条 | 未完成全量资源逐项核验 |
| [OpenLB尾斜杠入口](https://ngdc.cncb.ac.cn/openlb/)与[/home](https://ngdc.cncb.ac.cn/openlb/home) | 初次超时；无尾斜杠/openlb重试跳转后首页框架可读，Latest Literature仍Loading | 介绍可读，文献数据获取不稳定 |
| [OpenLB高级检索](https://ngdc.cncb.ac.cn/openlb/publication/search) | 多次445 Unknown Status Code | 未验证可选字段、PMID语法、cncbbank筛选 |
| [OpenLB 32460027](https://ngdc.cncb.ac.cn/openlb/publication/OLB-PM-32460027)、[36420893](https://ngdc.cncb.ac.cn/openlb/publication/OLB-PM-36420893) | 445；31373609详情重试可读框架但含动态占位符 | 真实关联、机构基金仅官方索引证据；不能批量获取 |
| [BioProject](https://ngdc.cncb.ac.cn/bioproject)、[BioSample](https://ngdc.cncb.ac.cn/biosample/)、[GSA](https://ngdc.cncb.ac.cn/gsa/) | 445 | 用官方提交指南/代理API等交叉核对，不能称主页完成实测 |
| [GSA-Human](https://ngdc.cncb.ac.cn/gsa-human/) | 多次404；/index工具不可访问 | 该入口本环境失败，不证明数据库不存在；需维护方提供当前入口 |
| [GWH首页](https://ngdc.cncb.ac.cn/gwh)、[OMIX](https://ngdc.cncb.ac.cn/omix/)、[Database Commons](https://ngdc.cncb.ac.cn/databasecommons/)、[Cell Taxonomy](https://ngdc.cncb.ac.cn/celltaxonomy/)、[GWAS Atlas](https://ngdc.cncb.ac.cn/gwas/) | 首页超时；部分帮助/详情/API另可读 | 单独记录替代证据，不把替代证据当原页面访问成功 |
| [Taxonomy根入口](https://ngdc.cncb.ac.cn/taxonomy) | 工具不可访问；/index_new可读但统计有模板 | 使用真实已读新入口；数据覆盖数字未知 |
| [BarcodeBLAST](https://ngdc.cncb.ac.cn/cgir/tool/barcode/) | 从BIT点击后超时 | 仅目录确认，不承诺可用 |
| [VersionMap](https://ngdc.cncb.ac.cn/gvmhp/versionmap) | 502 Bad Gateway | 暂不作为可用推荐 |
| [LncBook BLAST](https://ngdc.cncb.ac.cn/lncbook/tools/blast)及[Classification](https://ngdc.cncb.ac.cn/lncbook/tools/classification) | 445 | 导航确认，运行待核 |
| [中心BLAST](https://ngdc.cncb.ac.cn/blast/home) | 0行正文 | 只确认入口，不能验证输入/登录/比对运行 |
| [GenBase校验帮助](https://ngdc.cncb.ac.cn/genbase/gbthelp) | 超时 | 不能确认独立在线校验服务 |
| [IC4R旧浏览器链接所在官网](https://ngdc.cncb.ac.cn/ic4r/) | 目标bigd.big.ac.cn/browser返回Cache miss | 需确认迁移地址；完整旧URL见工具清单 |
| [BIT手册](https://ngdc.cncb.ac.cn/bit/example/NGDC_BIT_User_Manual_v3.pdf)、[数据库手册](https://ngdc.cncb.ac.cn/static/file/NGDC-DB.pdf) | 分别约24.97MB、34.36MB，超过网页工具内容限制 | 未完成手册全文阅读；可人工提供精简版/相关章节 |
| [NCBI说明NBK25499](https://www.ncbi.nlm.nih.gov/books/NBK25499/)及[NBK25501](https://www.ncbi.nlm.nih.gov/books/NBK25501/) | 浏览器校验页 | 外部补全仅候选，未验证实际文献补全 |
| 本机PowerShell对NGDC导航HTTPS请求 | Authentication failed（TLS） | 不绕过证书验证；网页工具结果不能证明本机采集网络已通 |

公开API的具体成功和失败请求另见[接口记录](task2_research_analysis/api_inventory.md)。没有登录、申请权限、上传数据、发邮件或运行大规模爬取。

## 需要人工提供的最小资料

| 优先级 | 资料/访问 | 用途与验收 |
| --- | --- | --- |
| P0 | OpenLB维护方确认的公网/内网入口、网络要求及只读访问方式 | 区分网络问题、动态前端和访问权限；不要提供明文密码 |
| P0 | cncbbank正式数据字典、版本、3条非空/1条空/1条多关联原始JSON或CSV | 确认类型、空值语义、数据库标识、资源登录号与关系类型 |
| P0 | 文献筛选与批量导出说明或授权样本 | 含PMID/DOI、发表年、作者、机构、基金、关联资源；确认分页、总量和许可 |
| P0 | 中心数据库标准ID和别名映射、成果统计纳入规则 | 区分介绍、提交、使用、引用等关系，确定统计总体 |
| P1 | BIT公开API/登录/运行限制说明及工具维护人 | 若只做跳转也需确认稳定入口；若自动调用须有正式契约 |
| P1 | 当前资源导航导出、GSA-Human/旧浏览器正确入口、超大手册相关章节 | 补齐全库逐项调研和失效链接，避免手写URL猜测 |
| P1 | GWH/ICG等接口的限流、分页、更新、字段定义与非空基金样本 | 验证是否可以扩大采集，明确提交机构与论文机构的区别 |
| P2 | 作者机构标准化方案、基金项目号规则、主题分类口径 | 决定能否发布排名与人员机构趋势 |

## 附件使用说明

已只读提取根目录《试用期考核标准-详细说明附件.docx》的任务文字。附件明确给出四类数据、BIT/各库工具调研、OpenLB cncbbank非空筛选和文献统计方向。文中Taxonomy、BLAST等仅是示例方向；本轮分别寻找官方页面佐证。附件中的开发步骤不作为本阶段开发授权，依照本次请求仅交付调研文档。

## 本轮局限与交付边界

八份文档已经形成可评审的调研基线，但不是全中心所有数据库API和所有工具的运行验收。其他导航资源逐库细查、OpenLB批量数据、cncbbank定义及作者机构基金覆盖率仍明确待补。所有“未知”保留，不以常识补齐。后续开发应以数据契约和验收结果为前置条件。


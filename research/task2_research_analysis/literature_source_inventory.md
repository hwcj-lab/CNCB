# 文献来源与cncbbank核实记录

调研日期：2026-11-06。以下每项访问日期相同。附件是业务需求来源，**其中“筛选cncbbank非空”是待实现需求，不是字段存在、格式或可公开查询的证据**。

## OpenLB访问与能力

| 检查项 | 实证/官方来源 | 结论与状态 |
| --- | --- | --- |
| 官方入口 | [OpenLB](https://ngdc.cncb.ac.cn/openlb) → /openlb/home | 重试后首页框架已验证；初次/home、尾斜杠入口超时 |
| 数据来源 | [首页](https://ngdc.cncb.ac.cn/openlb/home)列PubMed、bioRxiv、medRxiv | 官方文档确认；不是同名openlb.net流体力学软件 |
| 一般检索 | 官网cancer示例跳转[publication?term=cancer](https://ngdc.cncb.ac.cn/openlb/publication?term=cancer) | 已验证URL和结果框架；未取得稳定真实结果集合 |
| 高级检索 | [Advanced Search](https://ngdc.cncb.ac.cn/openlb/publication/search) | 官方索引有AND/OR/NOT查询构建器；实际访问445，字段选项未确认 |
| PMID | [官方详情示例](https://ngdc.cncb.ac.cn/openlb/publication/OLB-PM-31373609)及[BIT文献链接](https://ngdc.cncb.ac.cn/bit/) | 确认OLB-PM-数字详情路由和PMID展示设计；PMID检索参数/语法未验证；部分详情仅模板 |
| 中心资源关联 | [PMID 32460027详情索引](https://ngdc.cncb.ac.cn/openlb/publication/OLB-PM-32460027)列GSA CRA002344 | 官方索引确认页面级关联；实际详情445；不等同cncbbank字段定义 |
| 作者/机构 | [PMID 31373609官方详情索引](https://ngdc.cncb.ac.cn/openlb/publication/OLB-PM-31373609)含Author Information | 官方索引确认作者与机构文本；本次直接详情模板化，结构化字段未知 |
| 基金 | [PMID 32460027官方详情索引](https://ngdc.cncb.ac.cn/openlb/publication/OLB-PM-32460027)含Grants及R01 DK112943/NIDDK NIH HHS | 官方索引确认样例；不能据此确定全部基金字段或覆盖率 |
| 公开API | 检索site:ngdc.cncb.ac.cn openlb API及精确cncbbank | 未找到可确认的公开API文档；不能把/publication网页路由称为API |
| 批量导出 | 首页、一般/高级检索、官方资料索引 | 未找到可验证批量导出方法；分页上限/导出格式/字段未知 |
| 访问与使用限制 | [官方首页索引](https://ngdc.cncb.ac.cn/openlb)的学术用途说明及上游分发条款声明 | 官方资料确认学术使用/商业许可联系要求；未确认接口配额。公开摘要不等于任意全文可再分发 |

OpenLB能读到介绍和部分官方索引样例，**未能在本环境取得可用于本项目的批量文献数据或cncbbank筛选集**。不能用展示的总文献数推断可下载数量、字段完整率或数据新鲜度。

## cncbbank六项核实结论

| 核实问题 | 证据与状态 | 后续需要 |
| --- | --- | --- |
| 实际含义 | 未找到官方字段定义或JSON原始字段；待人工核实 | 维护方数据字典、所属集合/索引、版本 |
| 格式 | 未知：不得假定字符串、数组、对象或库名列表 | 至少3条非空、1条空值、1条多资源关联样例 |
| 非空筛选方式 | 未确认；不编造SQL、Elasticsearch exists或URL查询语法 | 可公开的筛选说明、参数、分页和空值语义 |
| 存储内容 | 页面Links to CNCB-NGDC Resources包含库名+登录号，但不能证明该区块来自cncbbank | 字段值与前端关联区块的映射说明 |
| 能否建立论文-数据库关联 | 官方页面级关联存在；以cncbbank建立自动关联仅为条件性方案 | 数据库标识注册表、资源登录号、关系类型及依据 |
| 有无真实非空记录清单 | 未取得；不创建伪造PMID清单 | 经授权的CSV/JSON导出或只读API |

以下仅是**推测性候选解释**：cncbbank可能与中心资源关联有关，依据是附件描述及OpenLB关联展示。没有官方字段/原始样本佐证，不作为映射规则或数据库字段设计事实。

## 文献数据替代来源

| 来源 | 可获得内容 | 程序化能力 | 限制/状态 |
| --- | --- | --- | --- |
| GWH经项目查询 | 项目关联PMID/DOI/年份/部分作者，提交机构 | [BioProject API](https://ngdc.cncb.ac.cn/gwh/api/public/bioProject/PRJCA000437) | 已验证JSON；不是PMID反查、没有全库列表或完整作者机构 |
| ICG | 文献PMID、标题、作者串、期刊、年份、摘要 | [PMID API](https://ngdc.cncb.ac.cn/icg/api/json/publication/24810581) | 已验证JSON；基金、作者机构及文献→研究关系未确认 |
| EWAS Atlas | 文献及相关研究 | [官方API文档](https://ngdc.cncb.ac.cn/ewas/api) | 文档确认pmid查询；实际接口不可访问 |
| MethBank | 项目PMID、Submitter及元数据下载入口 | [Projects](https://ngdc.cncb.ac.cn/methbank/projects) | 官方索引确认字段；数据行/导出结果未取得 |
| GEN | 项目/数据集文献标识 | [官方文档](https://ngdc.cncb.ac.cn/gen/documentation) | PMID列可能含PMID或DOI，必须按格式区分；API未找到 |
| LncExpDB | 项目与PMID对应表 | [Help](https://ngdc.cncb.ac.cn/lncexpdb/help) | 官方表有PRJNA413568→29507413等样例；有缺失“-”；不是全量文献接口 |
| BrainBase | 疾病基因证据PMID/Source及多组学参考文献 | [FAQ](https://ngdc.cncb.ac.cn/brainbase/faq) | 官方文档确认；未找到公开文献API |
| Database Commons / BioCode | 数据库/工具介绍文献 | [GenBase详情](https://ngdc.cncb.ac.cn/databasecommons/database/id/8197)、[BioCode](https://ngdc.cncb.ac.cn/biocode/) | 介绍论文、建设机构、库建设基金不能混作数据使用成果或论文资助 |

## 已见标识样例（不是cncbbank筛选结果）

| 来源资源 | PMID | DOI或资源号 | 年份 | 验证状态 | 官方来源 |
| --- | --- | --- | --- | --- | --- |
| GWH→BioProject PRJCA000437 | 34939160 | 10.1007/s11427-021-2033-2 | 2022 | 已验证JSON；authorList=null | [API](https://ngdc.cncb.ac.cn/gwh/api/public/bioProject/PRJCA000437) |
| GWH→BioProject PRJCA000437 | 32070270 | 10.1186/s12864-020-6579-z | 2020 | 已验证JSON；主题与项目差异需核验关联 | [API](https://ngdc.cncb.ac.cn/gwh/api/public/bioProject/PRJCA000437) |
| ICG | 24810581 | 本轮不猜测未摘录的DOI | 2014 | 已验证JSON，含author字符串 | [API](https://ngdc.cncb.ac.cn/icg/api/json/publication/24810581) |
| OpenLB→GSA | 32460027 | CRA002344 | 待核 | 仅官方搜索索引；直接读取445 | [详情](https://ngdc.cncb.ac.cn/openlb/publication/OLB-PM-32460027) |

## 外部补全候选（不替代中心关联证据）

[NCBI E-utilities官方说明](https://www.ncbi.nlm.nih.gov/books/NBK25497/)确认可通过ESearch查UID、EFetch取记录，PubMed UID即PMID，并支持批量与History。限流为无API key每IP不超过3请求/秒，带key默认10请求/秒；引用来源与摘要版权须遵循官方要求。本轮NBK25499/NBK25501直接访问遇到浏览器校验，仅通过官方索引核对上述通用能力，**未实测PubMed文献补全，不填写未经核实的EFetch调用参数及XML字段路径**。

需要下一轮核验PubMed作者-机构、基金与主题字段及完整率。它不能提供中心cncbbank字段，不能用“PubMed存在这篇论文”证明该论文使用了中心数据。专利、科技成果尚无已确认中心来源，本阶段不把其统计纳入可交付能力。


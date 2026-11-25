# 数据库 数据类型与工具映射

调研日期：2026-11-06；表中各行访问日期相同。以下为第一个业务页面的**候选内容表**，不含未经验证的运行承诺。原生关系来自数据库导航；“跨库推荐建议”是本项目的数据兼容性分析，不是官方已建立的数据关联。

| 数据库名称 | 数据类型 | 工具名称 | 工具功能 | 工具入口 | 所属平台 | 关联依据/限制 | 调研状态 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| GenBase / GWH | DNA序列 | Needle | 两条核酸序列全局比对 | [入口](https://ngdc.cncb.ac.cn/bit/needle) | BIT | 跨库推荐建议；不是已证实原生数据联动 | 配置页已验证；运行/数据传递待验证 |
| GenBase / GWH | 蛋白质序列 | Needle | 两条蛋白序列全局比对 | [入口](https://ngdc.cncb.ac.cn/bit/needle) | BIT | 跨库推荐建议；先确认可取得蛋白FASTA | 配置页已验证；运行/数据传递待验证 |
| GenBase / GWH | DNA序列、蛋白质序列 | BLAST | 序列相似性搜索 | [入口](https://ngdc.cncb.ac.cn/blast/home) | 中心BLAST | 官网推荐；可搜索库覆盖须核验 | 官方说明确认；入口空壳，暂不标可用 |
| GenBase / GWH | DNA序列、蛋白质序列 | Dotmatcher | 序列相似性点阵图 | [入口](https://ngdc.cncb.ac.cn/bit/dotmatcher) | BIT | 跨库推荐建议 | 已验证输入页；运行待验证 |
| GWH | DNA序列/基因组注释 | Genome Browser | 组装区间与轨道浏览 | [入口](https://ngdc.cncb.ac.cn/gwh/browser/index/Zea_mays/GWHAAJX00000000/GWHAAJX00000002/1/200) | GWH | 原生工具；URL是官网示例 | 入口已验证；任意assembly适配待验证 |
| Taxonomy | DNA序列、蛋白质序列、RNA | Clustal Omega | 多序列比对与系统进化入口 | [入口](https://ngdc.cncb.ac.cn/taxonomy/clustalOmega) | Taxonomy | 原生工具 | 输入页已验证；运行待验证 |
| Taxonomy | DNA序列/物种 | Biological Barcode Identification | DNA条形码鉴定 | [入口](https://ngdc.cncb.ac.cn/taxonomy/barCode) | Taxonomy | 原生工具；仅DNA输入 | 已验证说明；运行待验证 |
| Taxonomy | 物种 | 分类信息详情（画像候选） | 查看分类信息/谱系 | [入口](https://ngdc.cncb.ac.cn/taxonomy/detail?id=9605) | Taxonomy | 原生服务；9605为官网示例Homo属 | 页面已验证；完整物种画像待人工核实 |
| RCoV19 | 病毒核酸序列 | Evolutionary tree | 病毒系统进化构建 | [入口](https://ngdc.cncb.ac.cn/ncov/online/tool/tree) | RCoV19 / BIT | 原生工具，病毒范围 | 页面已验证；输入约束/运行待验证 |
| IC4R | 水稻序列 | BLAST（IC4R） | 水稻数据序列比对 | [入口](https://ngdc.cncb.ac.cn/ic4r/blast) | IC4R | 原生入口；DNA/蛋白选项待核 | 页面已验证；运行待验证 |
| LncBook | lncRNA/核酸序列 | lncbook-BLAST | lncRNA序列比对 | [入口](https://ngdc.cncb.ac.cn/lncbook/tools/blast) | LncBook / BIT | 原生工具；不可扩展为蛋白比对 | 官方导航确认；445，暂不上线 |
| GEN | RNA表达矩阵 | Online Analysis | 表达数据在线分析入口 | [入口](https://ngdc.cncb.ac.cn/gen/analysis) | GEN | 原生入口，非DNA序列工具 | 页面已验证；子工具待核 |
| MethBank | DNA甲基化 | Age Predictor / IDMP / DMR Toolkit | 年龄预测及差异甲基化分析 | [入口](https://ngdc.cncb.ac.cn/methbank/) | MethBank / BIT | 原生工具；具体链接见工具清单 | 页面已验证；不是原始DNA序列推荐 |
| LncExpDB | RNA表达/基因ID | LncNet | 共表达网络可视化 | [入口](https://ngdc.cncb.ac.cn/lncexpdb/lncnet) | LncExpDB | 原生工具 | 官方索引确认；交互待验证 |
| OpenLB | 文献 | 文献检索/关联导航（服务） | 检索文献并导航中心资源 | [入口](https://ngdc.cncb.ac.cn/openlb/home) | OpenLB | 文献服务，不是BIT分析工具 | 官方资料确认；本环境访问失败 |
| ICG | 文献/PMID | PMID查询API（数据服务） | 获取内参基因相关文献元数据 | [入口](https://ngdc.cncb.ac.cn/icg/api/json/publication/24810581) | ICG | 原生接口；URL示例PMID来自文档 | 已验证JSON；不是文献分析算法 |

## 四类数据展示规则（设计建议）

1. DNA：优先GenBase、GWH；GSA是原始reads，需单独标明，不能直接送入只接受组装序列的工具。GVM变异、MethBank甲基化是相关数据，不能凭DNA标签自动推荐序列工具。
2. 蛋白质：已确认GenBase与GWH蛋白序列/文件。OMIX蛋白组、BrainBase蛋白知识不等于已确认有可用蛋白FASTA。
3. 文献：OpenLB是主入口；ICG有实际PMID查询，GWH有项目到文献的JSON。当前未确认BIT文献计量工具，此模块可展示检索/导航与来源，不虚填算法工具。
4. 物种：Taxonomy优先；BioSample/BioProject/GWH/GEN/MethBank有物种或分类信息，但不是所有资源都具有物种鉴定工具。

## 页面实现前的验收条件

- 只提供链接推荐的首版可基于以上候选表；上线前逐个浏览器点击、确认登录要求，并选官方示例验证计算。
- 失败或未知链接保持禁用/待验证标签，展示最后核验日期。帮助页、工具介绍页、数据查询接口应标明服务类型。
- 任意序列粘贴、文件上传、自动预填URL、跨域调用均未确认，不能承诺一键分析。
- 多输入类型拆成多条推荐关系；同一工具用实际工具ID去重。统一BLAST和数据库内BLAST保留不同部署实例，不能仅按名称合并。
- 来源：[GenBase文档](https://ngdc.cncb.ac.cn/genbase/document)、[GWH文档](https://ngdc.cncb.ac.cn/gwh/api_documents)、[BIT](https://ngdc.cncb.ac.cn/bit/)、[Taxonomy](https://ngdc.cncb.ac.cn/taxonomy/index_new)、[MethBank](https://ngdc.cncb.ac.cn/methbank/)、[IC4R](https://ngdc.cncb.ac.cn/ic4r/)、[LncBook](https://ngdc.cncb.ac.cn/lncbook/)。详细输入证据见[工具清单](tool_inventory.md)。


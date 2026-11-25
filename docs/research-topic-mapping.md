# 研究主题标签与统计口径

## 总览图的口径

总览的“研究主题分布”按论文 `cncb_banks_json` 中的 `db` 字段识别关联数据库，再读取 `research_database_topic` 中已确认的数据库—主题映射，最终按 `research_topic_category` 的主题名称聚合。一个论文关联多个已标记数据库时，会进入多个主题；同一个主题内按文献唯一值去重。未知数据库不猜标签，也不进入该图。

`research_topic_category` 保存可维护的主题字典；`research_database_topic` 单独保存数据库与主题的多对多映射、证据和确认状态。以后新增主题只加字典记录，再给适用数据库增加映射，不需要改 Java 枚举。

分数据库详情中的主题仍保留关键词和 PubMed MeSH 主题词。MeSH 描述符如 `Databases, Genetic` 中的逗号属于术语本身；关键词分隔不再把逗号直接当分隔符，避免把一个短语拆开。它们不再作为总览主题标签。

## 当前目录前 13 条核心资源的标签

这里的“13 条”按本地 `database_resource.catalog_id` 1–13 核对。标签依据实际主要内容；平台/目录类放入“其他”或“数据库”，项目、样本等元数据不当作其关联归档内容。

| catalog_id | 数据库 | 标签 | 判断依据 |
|---:|---|---|---|
| 1 | 3CDB | 组学数据集 | 收录染色质构象捕获相互作用数据。 |
| 2 | GSA | 序列 | 主要归档原始测序 reads；项目、样本记录用于组织数据。 |
| 3 | Database Commons | 数据库 | 全球生物数据库目录。 |
| 4 | BioCode | 工具 | 归档和发布生物信息学工具代码。 |
| 5 | BioProject | 项目 | 研究项目描述元数据。 |
| 6 | BioSample | 样本 | 生物来源材料及其属性描述。 |
| 7 | GSA-Human | 序列 | 人类遗传资源相关的组学原始测序数据。 |
| 8 | OMIX | 组学数据集 | 转录组、蛋白质组、表观组、脂质组、代谢组及多模态数据。 |
| 9 | GenBase | 序列 | 带注释的核苷酸和蛋白质序列。 |
| 10 | SDAS | 其他 | 汇交计划、进度监督和证明服务；官方说明不存储科学数据。 |
| 11 | HGRIP | 其他 | 人类遗传资源管理与备份平台；底层数据由多个资源库承载。 |
| 12 | GVM | 变异 | 主要为 SNP 和小片段插入缺失变异。 |
| 13 | GWAS Atlas | 变异 | 基因组变异与性状关联（G2P）数据。 |

此次数据映射还包括 API 中可能出现的别名，例如 `HRA`、`GSA-Human`、`GenBase`、`nucbank_nuc2`、`nucbank_pro2`。这些别名指向同一数据库的同一标签，不代表重复资源。

## 作者机构口径

作者优先使用自己 `authors_json` 节点中的机构。作者机构为空时，按已确认的口径用同一篇文章中出现次数最多的作者机构补齐；同名作者仍按“姓名 + 机构”分开。机构显示会去掉邮箱等联系信息；如果署名第二段像机构（常见格式如“科室/学院, 机构, 城市, 国家”），优先取第二段，否则从地址片段中提取机构名。原始署名完整保留在 `authors_json` 以便追查。该提取是规则化清洗，不等于 ROR 等权威机构实体消歧；含多个独立单位的复杂署名需要后续人工复核。

所有作者都会参与计数，但快照只物化作者、机构、基金和主题关联文献数最高的前 50 项；页面可以在这些结果中选择展示前 10、20、30、40 或 50 项。

## 依据链接

- [3CDB](https://ngdc.cncb.ac.cn/3cdb/)
- [GSA 教程](https://ngdc.cncb.ac.cn/education/tutorials/gsa/)
- [Database Commons 帮助](https://ngdc.cncb.ac.cn/databasecommons/help)
- [BioCode 帮助](https://ngdc.cncb.ac.cn/biocode/help)
- [BioSample 提交指南](https://ngdc.cncb.ac.cn/gsub/document/BioSample-BioSample_Submission_Guide_2.1.cn.pdf)
- [OMIX](https://ngdc.cncb.ac.cn/omix/)
- [GenBase 文档](https://ngdc.cncb.ac.cn/genbase/document)
- [SDAS 帮助](https://ngdc.cncb.ac.cn/sdas/help)
- [HGRIP 平台介绍](https://ngdc.cncb.ac.cn/news/146)
- [GVM FAQ](https://ngdc.cncb.ac.cn/gvm/faq)
- [GWAS Atlas 文档](https://ngdc.cncb.ac.cn/gwas/documentation)

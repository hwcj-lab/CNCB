# 中心数据库资源清单

调研日期：2026-11-06（Asia/Shanghai）。本表23条为优先深入调研资源，不代表中心官网全部数据库。所有行访问日期均为本日。中文名标注“描述性”的不是已确认官方命名。

“是”表示有页面/文档证据；“关联”表示元数据或相关数据，不能作为可直接输入序列工具的序列证据；“待核”表示没有足够证据，不表示不存在。数据库介绍论文与数据集对应论文分开处理。

| ID | 数据库名称 | 中文名称 | 英文名称 | 官方网站 | 所属领域 | 收录的数据类型 | DNA序列 | 蛋白质序列 | 文献 | 物种 | 机构/基金/作者 | 信息来源 | 调研状态 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| D01 | BioProject | 生物项目信息库 | Biological Project Library | [入口](https://ngdc.cncb.ac.cn/bioproject) | 项目元数据 | 项目、物种、递交者、基金、文献 | 关联 | 关联 | 是 | 是 | 递交者机构、基金；论文作者须核验 | [官方来源](https://ngdc.cncb.ac.cn/databasecommons/database/id/6010) | 官方文档确认；首页445 |
| D02 | BioSample | 生物样本信息库 | BioSample（未确认独立英文全称） | [入口](https://ngdc.cncb.ac.cn/biosample/) | 样本元数据 | 样本、来源物种、属性、项目链接 | 关联 | 待核 | 待核 | 是 | 待核；GWH代理样本接口有文档 | [官方来源](https://ngdc.cncb.ac.cn/gsub/) | 官方文档确认；首页445 |
| D03 | GenBase | 基因序列库 | Genetic Sequence Database | [入口](https://ngdc.cncb.ac.cn/genbase/) | 序列 | 核酸、蛋白序列与注释 | 是 | 是 | 是 | 是 | 作者、提交者、出版资料；基金待核 | [官方来源](https://ngdc.cncb.ac.cn/genbase/document) | 已验证首页；API文件未验证 |
| D04 | GSA | 组学原始数据归档库 | Genome Sequence Archive | [入口](https://ngdc.cncb.ac.cn/gsa/) | 原始组学 | 原始测序reads及元数据 | 是（含DNA测序；并非所有记录） | 待核（原始reads不等于蛋白序列） | 关联 | 是 | 经BioProject关联；独立字段待核 | [官方来源](https://ngdc.cncb.ac.cn/gsub/) | 官方文档确认；首页445 |
| D05 | GSA-Human | 人类组学原始数据归档库 | GSA for Human | [入口](https://ngdc.cncb.ac.cn/gsa-human/) | 人类组学 | 人类遗传相关组学数据 | 是（记录级待核） | 待核 | 关联 | 是（人类） | 待核 | [官方来源](https://ngdc.cncb.ac.cn/gsub/) | 官方文档确认；该入口404 |
| D06 | OMIX | 多元数据归档库 | Open Archive for Miscellaneous Data | [入口](https://ngdc.cncb.ac.cn/omix/) | 多元组学 | 转录组、蛋白组、表观组、代谢组等 | 待核（不将组学等同DNA序列） | 待核（蛋白组不等于序列） | 是（文章关联） | 待核 | 待核 | [官方来源](https://ngdc.cncb.ac.cn/omix/methods) | 官方资料确认；首页超时 |
| D07 | GWH | 基因组序列库 | Genome Warehouse | [入口](https://ngdc.cncb.ac.cn/gwh) | 基因组与注释 | 组装、DNA/CDS/RNA/蛋白文件、文献、项目 | 是 | 是 | 是 | 是 | 提交机构、contacts；项目基金字段可为空 | [官方来源](https://ngdc.cncb.ac.cn/gwh/api_documents) | 已验证API；首页超时 |
| D08 | GVM | 基因组变异库 | Genome Variation Map | [入口](https://ngdc.cncb.ac.cn/gvm/) | 变异 | SNP、InDel及物种项目样本 | 关联（变异≠完整序列） | 待核 | 有库引用；项目文献待逐条核实 | 是 | 待核 | [官方来源](https://ngdc.cncb.ac.cn/gvm/) | 已验证首页 |
| D09 | GEN | 基因表达数据库 | Gene Expression Nebulas | [入口](https://ngdc.cncb.ac.cn/gen/) | 转录组 | bulk、单细胞、空间RNA-seq表达谱、项目、文献 | 待核（表达矩阵不等于序列） | 待核 | 是 | 是 | 待核 | [官方来源](https://ngdc.cncb.ac.cn/gen/documentation) | 已验证首页；文档确认PMID/DOI |
| D10 | MethBank | 甲基化数据库 | Methylation Bank | [入口](https://ngdc.cncb.ac.cn/methbank/) | 表观组 | DNA甲基化、项目样本、DMR、文献 | 关联（甲基化不是原始DNA序列） | 待核 | 是（PMID列） | 是 | Submitter可见；作者机构基金待核 | [官方来源](https://ngdc.cncb.ac.cn/methbank/projects) | 已验证首页；官方索引确认字段 |
| D11 | BioCode | 生物工具软件库 | BioCode | [入口](https://ngdc.cncb.ac.cn/biocode/) | 工具代码与知识 | 工具元信息和代码归档 | 待核 | 待核 | 有工具文献；完整字段待核 | 待核 | 待核；工具贡献者≠论文作者 | [官方来源](https://ngdc.cncb.ac.cn/biocode/) | 已验证首页 |
| D12 | Database Commons | 生物数据库目录 | A Catalog of Worldwide Biological Databases | [入口](https://ngdc.cncb.ac.cn/databasecommons/) | 数据库元数据 | 数据库、分类、机构、基金、资源介绍文献 | 元数据标签 | 元数据标签 | 是 | 元数据分类 | 数据库建设机构、联系人、基金；不是论文机构 | [官方来源](https://ngdc.cncb.ac.cn/databasecommons/database/id/8197) | 已验证详情；首页超时 |
| D13 | OpenLB | 生命科学文献库 | Open Library of Bioscience | [入口](https://ngdc.cncb.ac.cn/openlb/home) | 文献 | 标题、摘要、作者、期刊、参考文献及中心资源链接 | 关联 | 关联 | 是 | 待核（取决于文献） | 官方索引见作者机构、Grants；批量未验证 | [官方来源](https://ngdc.cncb.ac.cn/openlb/publication/OLB-PM-32460027) | 首页框架重试可读；高级检索/指定详情超时或445，数据未获取 |
| D14 | Taxonomy | 物种分类资源（描述性中文名） | Taxonomy | [入口](https://ngdc.cncb.ac.cn/taxonomy/index_new) | 物种与分类 | 分类ID、名称、谱系、分类树及分析入口 | 工具支持 | 工具支持 | 待核 | 是 | 待核 | [官方来源](https://ngdc.cncb.ac.cn/taxonomy/index_new) | 已验证页面；动态统计未加载 |
| D15 | Cell Taxonomy | 细胞分类资源（描述性中文名） | Cell Taxonomy | [入口](https://ngdc.cncb.ac.cn/celltaxonomy/) | 单细胞与细胞类型 | 物种、组织、细胞类型、marker、文献计数 | 待核 | 待核（marker≠蛋白序列） | 是（目录字段） | 是 | 待核 | [官方来源](https://ngdc.cncb.ac.cn/celltaxonomy/species) | 官方索引确认；首页超时 |
| D16 | LncBook | 长非编码RNA知识库 | LncBook | [入口](https://ngdc.cncb.ac.cn/lncbook/) | 非编码RNA | 人类lncRNA、多组学注释及small protein模块 | 关联 | 待核（模块不等于可下载序列） | 待核（内容文献需逐条检查） | 是（人类） | 待核 | [官方来源](https://ngdc.cncb.ac.cn/lncbook/) | 已验证首页 |
| D17 | EWAS Atlas | 表观组关联研究知识库 | Epigenome-Wide Association Study Atlas | [入口](https://ngdc.cncb.ac.cn/ewas/atlas) | 表观组关联 | 文献、研究、探针、性状关联 | 关联（DNA甲基化） | 待核 | 是 | 是（人类） | 数据库机构基金可见；论文级待核 | [官方来源](https://ngdc.cncb.ac.cn/ewas/api) | 官方API文档确认；接口访问失败 |
| D18 | GWAS Atlas | 基因型与表型关联知识库 | Genome-Wide Association Studies Atlas | [入口](https://ngdc.cncb.ac.cn/gwas/) | 遗传关联 | 跨物种变异-性状关联、研究与文献 | 关联 | 待核 | 是 | 是 | 待核 | [官方来源](https://ngdc.cncb.ac.cn/gwas/documentation) | 官方培训资料确认；首页超时 |
| D19 | BrainBase | 脑疾病知识库 | BrainBase | [入口](https://ngdc.cncb.ac.cn/brainbase/index) | 脑疾病 | 疾病-基因、药物、文献、多组学资料 | 关联 | 待核（蛋白模块不是序列确认） | 是 | 待核 | 待核 | [官方来源](https://ngdc.cncb.ac.cn/brainbase/faq) | 已验证首页；FAQ确认文献来源 |
| D20 | LncExpDB | 人类长非编码RNA表达数据库 | Expression Database of Human Long non-coding RNAs | [入口](https://ngdc.cncb.ac.cn/lncexpdb/) | 非编码RNA表达 | 表达谱、共表达网络、项目和PMID | 待核（RNA表达非DNA序列） | 待核 | 是 | 是（人类） | 待核 | [官方来源](https://ngdc.cncb.ac.cn/lncexpdb/help) | 已验证首页；官方帮助索引确认 |
| D21 | RCoV19 | 2019新型冠状病毒信息库 | Resource for Coronavirus 2019 | [入口](https://ngdc.cncb.ac.cn/ncov/) | 病毒基因组 | 病毒序列、变异、宿主、文献情报 | 是 | 待核（S蛋白突变≠蛋白序列） | 是（情报模块） | 是（病毒及宿主） | 待核 | [官方来源](https://ngdc.cncb.ac.cn/ncov/genome/search) | 已验证首页；API有官方文档 |
| D22 | ICG | 内参基因数据库（描述性中文名） | Internal Control Genes Database | [入口](https://ngdc.cncb.ac.cn/icg/) | 基因表达标准化 | 物种、内参基因、研究、文献 | 待核（基因记录非DNA序列） | 待核 | 是 | 是 | 作者字符串已验证；机构基金未确认 | [官方来源](https://ngdc.cncb.ac.cn/icg/api) | 已验证PMID接口 |
| D23 | IC4R | 水稻信息共享 | Information Commons for Rice | [入口](https://ngdc.cncb.ac.cn/ic4r/) | 水稻基因组 | 编码基因、lncRNA、circRNA、注释 | 关联；BLAST/浏览器入口 | 待核（编码基因非蛋白序列） | 待核（库引用不算全量文献） | 是（水稻） | 待核 | [官方来源](https://ngdc.cncb.ac.cn/ic4r/api) | 已验证首页和gene API |

## 范围和归属限制

从[中心主页](https://www.cncb.ac.cn/)及[NGDC导航](https://ngdc.cncb.ac.cn/databases)出发。导航搜索索引显示119条“资源”，但直接打开只看到模板占位符；资源包含工具等，不能称为119个数据库。主页统一检索还包含DMS、NCBI、EBI和合作库，不能全部归为中心自建库。Database Commons收录的全球数据库也不能这样归属。

## 其他已发现导航线索

[官方中文主页索引](https://ngdc.cncb.ac.cn/?lang=zh)还列出如下资源。仅确认导航收录，尚未逐库读取主页、解析实际链接和检查API；不纳入23条深入清单或可用数据库计数。为避免猜测URL，入口暂指向官方导航，下一轮须从导航取真实链接。以下分类沿用官网，不代表已核实数据字段。

| 官网分类 | 导航名称（逐库资料和接口待人工核实） | 来源/日期/状态 |
| --- | --- | --- |
| 基因组和变异 | 3CDB、CGB、CGIR、CompoDynamics、GTDB、HMC、P10K、PGG.Han、PGG.SNV、PGG.SV、Propan、RePoS、RVD、SR4R、TargetGene | [官方导航](https://ngdc.cncb.ac.cn/?lang=zh)；2026-11-06；官方索引确认 |
| 基因表达 | CancerSCEM、ChickenGTEx、Dynamic-BM、MTD、RED、SMDB、SmProt | 同上；仅目录 |
| 非编码RNA | dbDEMC、LncRNAWiki、LncVar、NONCODE、NPInter、piRBase、TransCirc | 同上；仅目录 |
| 表观基因组/单细胞 | EpiCancer、EWAS Data Hub、EWAS Open Platform、NucMap、scMethBank、CROST、DSMNC、scMultiModalMap、TE-SCALE | 同上；仅目录 |
| 生物多样性和生物合成 | ChickenSD、DDS Atlas、Dog10K、eLMSG、HGD、iBumblebee、iCellomics、iDOG、iPangolin、iSheep、LSD、MPA、NHP Atlas、NTM-DB、OPIA、PADS Arsenal、PED、PlantPan、PPGR、RDBSB、RiceWiki、ScienceWikis、SCOD、SesbaniaOmics、SorGSD、SoyOmics、TCOD | 同上；仅目录 |
| 健康和疾病 | Aging Atlas、ASCancer Atlas、BioKA、Brain Catalog、CAVDdb、CeDR、ClinMAVE、CNS-GAP、CVD Atlas、EDK、HALL、HemAtlas、HervD Atlas、iCTCF、KGCoV、leukemia、MACdb、MPoxVR、NAFLDkb、PWAS Atlas、Regeneration Roadmap、ResMicroDb、scTWAS Atlas、TWAS Atlas | 同上；仅目录 |
| 归档/服务类（不一定是数据库） | HGRIP、NODE、OBIA、SDAS、π-OmicsKit | 同上；仅目录 |

本轮未完成所有导航资源的逐库API盘点。主页动态加载、部分网页访问失败是明确限制；上述线索保留为后续清单，不填充未经核实的全称、字段、工具或API。


# 任务一：数据库数据类型与在线工具关系梳理

更新日期：2026-11-11。来源为CNCB数据库目录与工具目录公开接口。本文件是基于接口标题、描述和URL的第一轮保守标注，不等同于逐站运行验证。

## 判断原则

- 只按数据库主要实际存储内容标注；仅在描述中提及、通过外链关联或只保存元数据时不标。
- SNP/INDEL、甲基化值、表达量、项目元数据不自动等同DNA序列。
- 蛋白名称、蛋白表达量或互作关系不自动等同蛋白质序列。
- 参考论文或文献审编来源不自动等同文献数据库。
- 普通species字段不自动等同物种分类数据库。
- NATIVE仅在工具URL落入数据库官方URL路径时建立；COMPATIBLE只用于功能和输入均明确匹配的通用工具。

## 汇总

| 项目 | 数量 |
| --- | ---: |
| 官方数据库目录 | 121 |
| 数据类型关系 | 26 |
| DNA 标签数据库 | 19 |
| PROTEIN 标签数据库 | 4 |
| LITERATURE 标签数据库 | 2 |
| SPECIES 标签数据库 | 1 |
| 原生工具关系 | 108 |
| 兼容推荐关系 | 66 |

## 全量数据库标注结果

| 官方ID | 数据库 | 官方分类 | 本系统标签 | 状态/依据摘要 |
| ---: | --- | --- | --- | --- |
| 1 | [3CDB](https://ngdc.cncb.ac.cn/3cdb/) | 基因组和变异 | 未标注 | 描述不足或主要内容不属于当前四类：基于人工审编的染色体构象捕获数据的数据库（3CDB） |
| 2 | [GSA](https://ngdc.cncb.ac.cn/gsa) | 原始数据 | DNA | 描述明确：The Genome Sequence Archive (GSA) is a data repository for archiving raw sequence reads. It accepts data submissions fro… |
| 3 | [Database Commons](https://ngdc.cncb.ac.cn/databasecommons) | 原始数据 | 未标注 | 描述不足或主要内容不属于当前四类：Database Commons provides users with easy access to a comprehensive collection of publicly available biological database… |
| 4 | [BioCode](https://ngdc.cncb.ac.cn/biocode) | 原始数据 | 未标注 | 描述不足或主要内容不属于当前四类：生物信息学工具代码归档数据库 |
| 5 | [BioProject](https://ngdc.cncb.ac.cn/bioproject/) | 原始数据 | 未标注 | 描述不足或主要内容不属于当前四类： |
| 6 | [BioSample](http://ngdc.cncb.ac.cn/biosample/) | 原始数据 | 未标注 | 描述不足或主要内容不属于当前四类： |
| 7 | [GSA-Human](https://ngdc.cncb.ac.cn/gsa-human/) | 原始数据 | DNA | 描述明确：a part of GSA in the National Genomics Data Center, is a data repository specialized for human genetic related data deri… |
| 8 | [OMIX](https://ngdc.cncb.ac.cn/omix) | 原始数据 | 未标注 | 描述不足或主要内容不属于当前四类：Omix collects the genomics data from transcriptome, epigenome, microarray,the functional genomics data such as lipidome,… |
| 9 | [GenBase](https://ngdc.cncb.ac.cn/genbase/) | 原始数据 | DNA | 描述明确：GenBase is a genetic sequence database that accepts user submissions and integrates data from INSDC. |
| 10 | [SDAS](https://ngdc.cncb.ac.cn/sdas/) | 原始数据 | 未标注 | 描述不足或主要内容不属于当前四类：Scientific Data Archive System provides services such as plan submission, real-time query of data archiving and certific… |
| 11 | [HGRIP](https://ngdc.cncb.ac.cn/hgrip/) | 原始数据 | 未标注 | 描述不足或主要内容不属于当前四类：Human Genetic Resource Information Management Backup Platform |
| 12 | [GVM](https://ngdc.cncb.ac.cn/gvm) | 基因组和变异 | 未标注 | 描述不足或主要内容不属于当前四类：The Genome Variation Map (GVM) is a public data repository of genome variations, including single nucleotide polymorphis… |
| 13 | [GWAS Atlas](https://ngdc.cncb.ac.cn/gwas/) | 基因组和变异 | 未标注 | 描述不足或主要内容不属于当前四类：GWAS Atlas is a public and manually curated database of published genome-wide SNP-trait associations for plants and anim… |
| 14 | [GWH](https://ngdc.cncb.ac.cn/gwh/) | 基因组和变异 | DNA | 描述明确：The Genome Warehouse (GWH) is a public repository housing genome-scale data for a wide range of species and delivering a… |
| 16 | [SR4R](https://ngdc.cncb.ac.cn/sr4r) | 基因组和变异 | 未标注 | 描述不足或主要内容不属于当前四类：The SnpReady for Rice (SR4R) database presents four reference panels of SNPs and tools to satisfy different aspects of p… |
| 17 | [CompoDynamics](https://ngdc.cncb.ac.cn/compodynamics/) | 基因组和变异 | DNA | 描述明确：CompoDynamics is a comprehensive database for characterizing molecular characteristics of coding sequences (CDS). Taking… |
| 18 | [CGB](https://ngdc.cncb.ac.cn/cgb/) | 基因组和变异 | DNA | 描述明确：冠状病毒GenBrowser（CGB）基于高效的分析框架和节点拾取渲染策略，能够利用传播相关的元数据轻松分析和可视化SARS-CoV-2基因组序列。 |
| 19 | [MTD](https://ngdc.cncb.ac.cn/mtd/) | 基因表达 | 未标注 | 描述不足或主要内容不属于当前四类：MTD is a mammalian transcriptomic database that is focused on mammalian transcriptomes, and the current version contains… |
| 20 | [ICG](https://ngdc.cncb.ac.cn/icg) | 基因表达 | 未标注 | 描述不足或主要内容不属于当前四类：ICG is a wiki-based knowledgebase of internal control genes (or reference genes) for RT-qPCR normalization in a variety … |
| 21 | [RED](https://ngdc.cncb.ac.cn/red) | 基因表达 | 未标注 | 描述不足或主要内容不属于当前四类：Rice Expression Database (RED) is a repository of gene expression profiles derived entirely from RNA-Seq data analysis o… |
| 22 | [GEN](https://ngdc.cncb.ac.cn/gen/) | 基因表达 | 未标注 | 描述不足或主要内容不属于当前四类：Gene Expression Nebulas（GEN）是各种条件下的基因表达谱的数据门户，完全来自多个物种的RNA-seq数据分析。 |
| 23 | [CancerSCEM](https://ngdc.cncb.ac.cn/cancerscem) | 基因表达 | 未标注 | 描述不足或主要内容不属于当前四类：致力于收集、分析和可视化人类癌症单细胞RNA-seq数据的公共数据库。对不同类型的人类肿瘤微环境进行了多层次分析，并在数据库中配备了全面的在线分析平台。 |
| 24 | [LncRNAWiki](https://ngdc.cncb.ac.cn/lncrnawiki) | 非编码RNA | 未标注 | 描述不足或主要内容不属于当前四类：LncRNAWiki, a knowledgebase of human lncRNAs, has been rapidly expanded by incorporating more experimentally validated l… |
| 25 | [LncBook](http://ngdc.cncb.ac.cn/lncbook) | 非编码RNA | 未标注 | 描述不足或主要内容不属于当前四类：LncBook is devoted to the integration and multi-omics annotation of human lncRNAs. It provides a comprehensive and high-… |
| 26 | [LncExpDB](https://ngdc.cncb.ac.cn/lncexpdb/) | 非编码RNA | 未标注 | 描述不足或主要内容不属于当前四类：LncExpDB is a comprehensive database for lncRNA expression. It houses expression profiles of 101,293 high-quality human … |
| 27 | [MethBank](https://ngdc.cncb.ac.cn/methbank) | 表观基因组学、生物多样性和生物合成 | 未标注 | 描述不足或主要内容不属于当前四类：MethBank is a database that integrates high-quality DNA methylomes across a variety of species and provides an interacti… |
| 28 | [NucMap](http://ngdc.cncb.ac.cn/nucmap) | 表观基因组学 | 未标注 | 描述不足或主要内容不属于当前四类：NucMap is a database of genome-wide nucleosome positioning map across species. It is dedicated to collecting, analyzing … |
| 29 | [EWAS Atlas](http://ngdc.cncb.ac.cn/ewas) | 表观基因组学 | 未标注 | 描述不足或主要内容不属于当前四类：A knowledgebase of epigenome-wide association studies |
| 30 | [EWAS Data Hub](https://ngdc.cncb.ac.cn/ewas/datahub) | 表观基因组学 | 未标注 | 描述不足或主要内容不属于当前四类：The current release of EWAS Data Hub integrates a comprehensive collection of DNA methylation array data from 75 344 sam… |
| 31 | [EpiCancer](https://ngdc.cncb.ac.cn/epicancer/) | 表观基因组学 | 未标注 | 描述不足或主要内容不属于当前四类：Epitranscriptomics in Cancers (EpiCancer) is a curated knowledgebase of functional regulation factors, targeted drugs, a… |
| 32 | [EWAS Open Platform](https://ngdc.cncb.ac.cn/ewas/) | 表观基因组学 | 未标注 | 描述不足或主要内容不属于当前四类：EWAS Open Platform is a comprehensive analysis suite that integrates EWAS Atlas, EWAS Data Hub and EWAS Toolkit for epig… |
| 33 | [DSMNC](https://dsmnc.ngdc.cncb.ac.cn/) | 单细胞组学 | 未标注 | 描述不足或主要内容不属于当前四类：DSMNC is a database which provides most comprehensive catalogue of somatic SNVs occurring in single cells from various h… |
| 34 | [Cell Taxonomy](https://ngdc.cncb.ac.cn/celltaxonomy/) | 单细胞组学 | 未标注 | 描述不足或主要内容不属于当前四类：一种人工审编的细胞类型和相关标记的知识库，涵盖了广泛的物种、组织和条件。细胞分类学为精选的细胞类型提供了一个结构良好的分类学，并结合了数百万个单细胞的scRNA-seq数据集，以便进一步探索细胞类型和标记。提供了多方面的表征，包括细胞类型/… |
| 35 | [LSD](https://ngdc.cncb.ac.cn/lsd) | 生物多样性和生物合成 | 未标注 | 描述不足或主要内容不属于当前四类：The leaf senescence database is a comprehensive resource of senescence-associated genes and their corresponding mutants. |
| 36 | [RiceWiki](https://ngdc.cncb.ac.cn/ricewiki/index.php/Main_Page) | 生物多样性和生物合成 | 未标注 | 描述不足或主要内容不属于当前四类： |
| 37 | [IC4R](http://ngdc.cncb.ac.cn/ic4r) | 生物多样性和生物合成 | 未标注 | 描述不足或主要内容不属于当前四类：水稻信息共享平台（IC4R）是一个基于可扩展和可持续架构的水稻知识库，通过社区贡献的模块实现数据集成。IC4R专为可扩展性和可持续性而设计，通过Web API集成来自远程资源的数据，并具有协作集成来自多个已提交模块的RICE数据以及降低数据… |
| 38 | [SorGSD](https://ngdc.cncb.ac.cn/sorgsd) | 生物多样性和生物合成 | 未标注 | 描述不足或主要内容不属于当前四类：SorGSD is a comprehensive database of sorghum genomic variations (SNPs and INDELs) of 289 accessions, phenotypes, practi… |
| 39 | [ScienceWikis](https://ngdc.cncb.ac.cn/sciencewikis) | 生物多样性和生物合成 | 未标注 | 描述不足或主要内容不属于当前四类：ScienceWikis is a catalog of biological knowledge wikis and wiki extensions, aiming to exploit the full potential of wik… |
| 40 | [iDOG](https://ngdc.cncb.ac.cn/idog) | 生物多样性和生物合成 | DNA | 描述明确：the first integrated resource dedicated to domestic dogs and wild canids. It incorporates a variety of omics data, inclu… |
| 41 | [PED](http://ngdc.cncb.ac.cn/ped) | 生物多样性和生物合成 | 未标注 | 描述不足或主要内容不属于当前四类：a curated database of RNA editosome in plants that is dedicated to the curation, integration and standardization of plan… |
| 42 | [PADS Arsenal](https://ngdc.cncb.ac.cn/padsarsenal) | 生物多样性和生物合成 | 未标注 | 描述不足或主要内容不属于当前四类：PADS (Prokaryotic Antiviral Defense System) Arsenal is a database of prokaryotic defense systems related genes, which ar… |
| 43 | [iSheep](https://ngdc.cncb.ac.cn/isheep/) | 生物多样性和生物合成 | 未标注 | 描述不足或主要内容不属于当前四类：iSheep is a dedicated database for Ovis aries that provides open access to a complete collection of sheep genes, variant… |
| 44 | [HGD](https://ngdc.cncb.ac.cn/hgd) | 生物多样性和生物合成 | 未标注 | 描述不足或主要内容不属于当前四类：A data portal of homologous genes across multiple species |
| 45 | [TCOD](https://ngdc.cncb.ac.cn/tcod) | 生物多样性和生物合成 | DNA | 描述明确：TCOD, a multi-omics data platform for tropical crops, provides data service for selective breeding and trait improvement… |
| 46 | [EDK](https://ngdc.cncb.ac.cn/edk) | 健康和疾病 | 未标注 | 描述不足或主要内容不属于当前四类：an integrated knowledgebase of RNA editome-disease associations manually curated from published literatures. |
| 47 | [RCoV19](https://ngdc.cncb.ac.cn/ncov) | 健康和疾病 | DNA、PROTEIN | 描述明确：RCoV19  (Previously known as 2019nCoVR) features comprehensive integration of genomic and proteomic sequences as well as… |
| 48 | [Aging Atlas](https://ngdc.cncb.ac.cn/aging/index) | 健康和疾病、基因表达 | 未标注 | 描述不足或主要内容不属于当前四类：Aging Atlas旨在汇编由一系列高通量组学技术创建的大型基因表达和调控数据集，并作为一系列生命科学研究人员的宝贵资源。 |
| 49 | [CeDR](https://ngdc.cncb.ac.cn/cedr) | 健康和疾病 | 未标注 | 描述不足或主要内容不属于当前四类：CEDR代表了对人类、小鼠和细胞系的组织细胞类型特异性药物反应的综合系统探索。 |
| 50 | [ASCancer Atlas](https://ngdc.cncb.ac.cn/ascancer) | 健康和疾病 | 未标注 | 描述不足或主要内容不属于当前四类：人类癌症中可变剪接的综合知识库。 |
| 51 | [BrainBase](https://ngdc.cncb.ac.cn/brainbase) | 健康和疾病 | 未标注 | 描述不足或主要内容不属于当前四类：通过多组学数据分析，BrainBase不仅整合了来自已发表文献的有价值的知识关联，还整合了全面的分子图谱，特别是神经胶质瘤。因此，它很有希望成为大脑研究的一个有价值的知识库。 |
| 52 | [Regeneration Roadmap](https://ngdc.cncb.ac.cn/regeneration/index) | 健康和疾病 | 未标注 | 描述不足或主要内容不属于当前四类：Regeneration Roadmap aims to compile large datasets for gene expression and regulation created by a broad spectrum of hi… |
| 53 | [Brain Catalog](https://ngdc.cncb.ac.cn/braincatalog) | 健康和疾病 | 未标注 | 描述不足或主要内容不属于当前四类：大脑疾病和相关表型的遗传资源数据库。 |
| 54 | [TWAS Atlas](https://ngdc.cncb.ac.cn/twas/) | 健康和疾病 | 未标注 | 描述不足或主要内容不属于当前四类：Transcriptome-Wide Association Studies (TWAS) Atlas is a curated knowledgebase of transcriptome-wide association studies… |
| 55 | [MACdb](https://ngdc.cncb.ac.cn/macdb/home) | 健康和疾病 | 未标注 | 描述不足或主要内容不属于当前四类：MACdb is a comprehensive knowledgebase based totally on manual curation of cancer metabolic literature, providing high q… |
| 57 | [NONCODE](http://www.noncode.org/) | 非编码RNA | 未标注 | 描述不足或主要内容不属于当前四类：NONCODE is a comprehensive database of collection and annotation of ncRNAs (excluding tRNAs and rRNAs), especially lncRN… |
| 58 | [LncVar](http://bioinfo.ibp.ac.cn/LncVar) | 非编码RNA | 未标注 | 描述不足或主要内容不属于当前四类：LncVar is a database of genetic variation associated with lncRNA genes in six species, and lncRNA genes are collected fr… |
| 59 | [Dynamic-BM](http://bioinfo.ibp.ac.cn/Dynamic-BM) | 基因表达 | 未标注 | 描述不足或主要内容不属于当前四类：Dynamic-BM (Dynamic BodyMap) is a database for multiple species from time-series RNA-seq datasets. It specifically curat… |
| 61 | [SmProt](http://bigdata.ibp.ac.cn/SmProt/) | 基因表达 | PROTEIN | 描述明确： |
| 62 | [eLMSG](https://www.biosino.org/elmsg/) | 生物多样性和生物合成 | SPECIES | 描述明确：a web database, which will integrate microbial systematics, genomics, and phenomics (polyphasic taxonomy related phenoty… |
| 63 | [GTDB](https://www.biosino.org/gtdb/) | 基因组和变异 | DNA、PROTEIN | 描述明确：GTDB is an integrated repository of glycosyltransferases, which collects comprehensive information, including amino acid… |
| 64 | [NPInter](http://bigdata.ibp.ac.cn/npinter5/) | 非编码RNA | 未标注 | 描述不足或主要内容不属于当前四类：NPInter documents functional interactions between ncRNAs (except tRNAs and rRNAs) and biomolecules (proteins, RNAs and D… |
| 65 | [TransCirc](https://www.biosino.org/transcirc) | 非编码RNA | 未标注 | 描述不足或主要内容不属于当前四类：TransCirc is a specialized database that provide comprehensive evidences supporting the translation potential of circula… |
| 66 | [piRBase](http://bigdata.ibp.ac.cn/piRBase) | 非编码RNA | 未标注 | 描述不足或主要内容不属于当前四类：piRBase is a manually curated resource of piRNAs, which focused on piRNA function analyses as well as piRNA annotation. |
| 67 | [KGCoV](https://www.biosino.org/kgcov) | 健康和疾病 | 未标注 | 描述不足或主要内容不属于当前四类：KGCoV is a knowledge graph of COVID-19 that integrates epidemiological data and virus genomic data to find the evolution… |
| 68 | [PGG.Han](https://www.biosino.org/pgghan/index) | 基因组和变异 | 未标注 | 描述不足或主要内容不属于当前四类： |
| 69 | [PGG.SNV](https://pog.fudan.edu.cn/pggsnv/) | 基因组和变异 | 未标注 | 描述不足或主要内容不属于当前四类：A database for understanding evolutionary and medical implications of human single nucleotide variations in diverse popu… |
| 70 | [OpenLB](https://ngdc.cncb.ac.cn/openlb) | 文献和教育 | LITERATURE | 描述明确：Open Library of Biosciences |
| 71 | [Education](https://ngdc.cncb.ac.cn/education) | 文献和教育 | 未标注 | 描述不足或主要内容不属于当前四类：a series of educational materials including courses, tutorials, and training documentation |
| 72 | [CROST](https://ngdc.cncb.ac.cn/crost) | 单细胞组学 | 未标注 | 描述不足或主要内容不属于当前四类：CROST是一个综合性的空间转录组数据资源库，提供多物种，多疾病和多技术平台的空间转录组数据的整合分析，并提供一站式数据分析平台。 |
| 73 | [OBIA](https://ngdc.cncb.ac.cn/obia) | 原始数据 | 未标注 | 描述不足或主要内容不属于当前四类：开放生物医学成像档案是用于存档生物医学成像数据和相关临床数据的存储库。OBIA 为生物医学成像数据管理提供可靠的平台，以支持世界各地的研究活动。 |
| 74 | [OPIA](https://ngdc.cncb.ac.cn/opia/) | 原始数据、生物多样性和生物合成 | 未标注 | 描述不足或主要内容不属于当前四类：OPIA是一个开放的植物图像数据以及相应表型性状数据的存储库。为基于图像的植物表型分析及应用提供更多数据资源和图像预处理服务。 |
| 75 | [BioKA](https://ngdc.cncb.ac.cn/bioka/) | 健康和疾病 | 未标注 | 描述不足或主要内容不属于当前四类：BIOKA是一个精选和整合的生物标志物知识库，包括多种动物物种、疾病/性状和注释资源。不仅为人类疾病研究，而且为动物疾病和家畜育种提供综合知识。 |
| 79 | [HALL](https://ngdc.cncb.ac.cn/hall/) | 健康和疾病 | 未标注 | 描述不足或主要内容不属于当前四类：HALL是关于人类衰老和长寿的综合性数据库，旨在收集涵盖从婴幼儿、年轻人、老年人到百岁老人的各个年龄段的不同人群的多维组学知识和数据集。 |
| 80 | [TargetGene](https://ngdc.cncb.ac.cn/targetgene/) | 基因组和变异 | 未标注 | 描述不足或主要内容不属于当前四类：TargetGene是关于人类复杂性状相关SNP的靶基因数据库，通过在不同分子水平上链接SNP和靶基因，系统地鉴定出细胞类型特异性的SNP靶基因。 |
| 81 | [PlantPan](https://ngdc.cncb.ac.cn/plantpan/) | 生物多样性和生物合成 | 未标注 | 描述不足或主要内容不属于当前四类：PlantPan是一个综合数据库，包含11个植物物种，195个基因组的泛基因组分析结果。它为每个物种提供了基于基因和基于图谱的泛基因组，以及详细的基因功能、13种类型的基因组变异和基因组同源性等信息。 |
| 82 | [HervD Atlas](https://ngdc.cncb.ac.cn/hervd/) | 健康和疾病 | 未标注 | 描述不足或主要内容不属于当前四类：人类内源性逆转录病毒疾病图谱是HERV疾病关联的人工审编知识库 |
| 88 | [SMDB](https://www.biosino.org/smdb/) | 基因表达 | 未标注 | 描述不足或主要内容不属于当前四类：an interactive tool that enables researchers to interpret tissue organization and gene expression in situ |
| 89 | [PGG.SV](https://www.biosino.org/pggsv/#/home) | 基因组和变异 | 未标注 | 描述不足或主要内容不属于当前四类：A structural variation (SV) database containing 649,652 SVs from 6,273 whole-genome sequencing samples worldwide. |
| 90 | [NAFLDkb](https://www.biosino.org/nafldkb) | 健康和疾病 | 未标注 | 描述不足或主要内容不属于当前四类：a knowledge base and platform for drug development against non-alcoholic fatty liver disease |
| 91 | [RePoS](http://bigdata.ibp.ac.cn/RePoS/) | 基因组和变异 | 未标注 | 描述不足或主要内容不属于当前四类：a database for integration and exhibition with recent positive selection signals data (SDS & iHS). |
| 92 | [MPA](https://www.biosino.org/mpa/) | 生物多样性和生物合成 | 未标注 | 描述不足或主要内容不属于当前四类：a Mycobacteriaceae phenome database, which integrates the phenomic data of Mycobacteriaceae strains by literature mining… |
| 93 | [PPGR](https://ngdc.cncb.ac.cn/ppgr/) | 生物多样性和生物合成 | 未标注 | 描述不足或主要内容不属于当前四类：a comprehensive, high-quality resource of gene expression patterns, regulatory genes, metabolic pathways, protein-protei… |
| 94 | [NTM-DB](https://ngdc.cncb.ac.cn/ntmdb/) | 生物多样性和生物合成 | 未标注 | 描述不足或主要内容不属于当前四类：a user-friendly genomic database that consolidates extensive genomic data, MLST genotyping, clinical phenotype informati… |
| 95 | [HemAtlas](https://ngdc.cncb.ac.cn/hematlas/) | 健康和疾病 | 未标注 | 描述不足或主要内容不属于当前四类：an open and user-friendly database resource for hematopoiesis. |
| 96 | [MPoxVR](https://ngdc.cncb.ac.cn/gwh/poxvirus/) | 健康和疾病 | DNA、PROTEIN | 描述明确：Monkeypox Virus Resource features comprehensive integration and curation of monkeypox virus genome sequences, protein se… |
| 97 | [Propan](https://ngdc.cncb.ac.cn/propan/) | 基因组和变异 | DNA | 描述明确：ProPan是一个全面的原核泛基因组数据库 |
| 98 | [SoyOmics](https://ngdc.cncb.ac.cn/soyomics/) | 生物多样性和生物合成 | DNA | 描述明确：SoyOmics提供高质量的大豆基因组、泛基因组、变异组、转录组、表型组等数据检索和在线分析平台。 |
| 100 | [Dog10K](https://ngdc.cncb.ac.cn/dog10k) | 生物多样性和生物合成 | 未标注 | 描述不足或主要内容不属于当前四类：The aim of the Dog10k is to coordinate the global effort on genome sequencing in dogs and build a comprehensive resource… |
| 101 | [ChickenGTEx](http://ngdc.cncb.ac.cn/chickengtex) | 基因表达、生物多样性和生物合成 | 未标注 | 描述不足或主要内容不属于当前四类：a comprehensive public catalogue of genetic regulatory variants associated with multiple types of transcriptomic phenoty… |
| 102 | [CNS-GAP](https://ngdc.cncb.ac.cn/cnsgap/index) | 健康和疾病 | 未标注 | 描述不足或主要内容不属于当前四类：a landmark central nervous system (CNS) disease genomics program, molecularly characterized over 4028 primary diseases a… |
| 103 | [NHP Atlas](https://ngdc.cncb.ac.cn/nhp/) | 生物多样性和生物合成 | 未标注 | 描述不足或主要内容不属于当前四类：a comprehensive platform, seamlessly integrating vast, varied, and ever-expanding biological resources from non-human pr… |
| 104 | [scMethBank](https://ngdc.cncb.ac.cn/methbank/scm/) | 表观基因组学、单细胞组学 | 未标注 | 描述不足或主要内容不属于当前四类：多物种单细胞甲基化数据库 |
| 105 | [iCTCF](https://ngdc.cncb.ac.cn/ictcf/) | 健康和疾病 | 未标注 | 描述不足或主要内容不属于当前四类：Since December, 2019, the outbreak of an unknown viral pneumonia has severely affected Wuhan, China. This virus was quic… |
| 106 | [HMC](https://ngdc.cncb.ac.cn/hmc) | 基因组和变异 | 未标注 | 描述不足或主要内容不属于当前四类：a curated knowledgebase of human morphology |
| 107 | [CGIR](https://ngdc.cncb.ac.cn/cgir/) | 基因组和变异 | DNA | 描述明确：a curated resource of chloroplast genome information, dedicating to the integration, annotation and standardization of c… |
| 108 | [dbDEMC](https://www.biosino.org/dbDEMC/index) | 非编码RNA | 未标注 | 描述不足或主要内容不属于当前四类：dbDEMC (database of Differentially Expressed MiRNAs in human Cancers) is an integrated database that designed to store a… |
| 109 | [RDBSB](https://www.biosino.org/rdbsb/) | 生物多样性和生物合成 | 未标注 | 描述不足或主要内容不属于当前四类：Registry and databases of bioparts for synthetic biology |
| 110 | [ChickenSD](https://ngdc.cncb.ac.cn/chickensd/) | 生物多样性和生物合成 | 未标注 | 描述不足或主要内容不属于当前四类：A data container for the variation information of chicken (Gallus gallus) genome. The aim of this database is to constru… |
| 111 | [P10K](https://ngdc.cncb.ac.cn/p10k) | 基因组和变异、生物多样性和生物合成 | DNA | 描述明确：万种原生生物基因组（P10K）项目是第一个关注原生生物物种的大型国际基因组测序计划。 |
| 112 | [scTWAS Atlas](https://ngdc.cncb.ac.cn/sctwas/) | 健康和疾病 | 未标注 | 描述不足或主要内容不属于当前四类：scTWAS Atlas 通过文献整理和数据分析，收录并整合了细胞水平上的全转录组关联信息，同时提供丰富的可视化和分析工具，以推动单细胞分析中对TWAS关联的进一步研究。 |
| 113 | [CVD Atlas](https://ngdc.cncb.ac.cn/cvd/) | 健康和疾病 | 未标注 | 描述不足或主要内容不属于当前四类：CVD Atlas是一个综合性的心脑血管疾病数据库，整合了人工审编的基因-疾病关联、多组学数据分析结果，以及来自现有资源的先验知识。 |
| 114 | [Leukemia](https://ngdc.cncb.ac.cn/leukemia/) | 健康和疾病 | 未标注 | 描述不足或主要内容不属于当前四类：白血病图谱库是一个集成的综合数据库，致力于血液系统恶性肿瘤的拷贝数变异（CNV）分析与研究，目前主要关注急性髓系白血病（AML）。 |
| 127 | [scMultiModalMap](https://ngdc.cncb.ac.cn/scmultimodalmap/) | 单细胞组学 | 未标注 | 描述不足或主要内容不属于当前四类：SCMultimodalMap是一个创新的数据库，旨在为研究人员提供探索、分析和可视化单细胞多模式数据集的工具。通过利用来自Cite-Seq和10x Genomics Multiome等先进技术的高分辨率数据，SCMultimodalMap… |
| 128 | [TE-SCALE](https://ngdc.cncb.ac.cn/te-scale/) | 基因表达、单细胞组学、健康和疾病 | 未标注 | 描述不足或主要内容不属于当前四类：单细胞分辨率的转座子表达数据库，基于大量scRNA-seq数据集，涵盖20种癌症类型的 1317803个细胞。进行多层分析来表征肿瘤微环境中的转座子活性，识别肿瘤特异性转座子，并实现深入的功能探索。 |
| 129 | [ResMicroDb](https://resmicrodb.cncb.ac.cn/) | 健康和疾病 | 未标注 | 描述不足或主要内容不属于当前四类：RESmicroDB，人类呼吸微生物组的综合数据库和分析平台。ResMicroDB整合了来自489个出版物和514个项目的106，464个样本，涵盖10个样本点和146个表型。 |
| 130 | [DDS Atlas](https://ngdc.cncb.ac.cn/ddsatlas/) | 原始数据 | 未标注 | 描述不足或主要内容不属于当前四类：基于人工审编的DNA数据存储技术数据库 |
| 131 | [ClinMAVE](https://ngdc.cncb.ac.cn/clinmave) | 基因组和变异 | 未标注 | 描述不足或主要内容不属于当前四类：变异效应多重分析（MAVE）研究的精选数据库，提供变异水平的功能注释，以支持遗传变异解释、诊断决策和探索疾病中的基因型-表型关系。 |
| 132 | [CAVDdb](https://ngdc.cncb.ac.cn/cavd/) | 健康和疾病、生物多样性和生物合成 | 未标注 | 描述不足或主要内容不属于当前四类：钙化性主动脉瓣疾病数据库（CAVDDB）是为钙化性主动脉瓣疾病研究提供公共数据服务和分析工具的综合资源。该数据库目前提供了基因、蛋白质、代谢物和甲基化数据的全面概况，使多组学分析成为可能。它有一个基因组浏览器，用于可视化不同的组学数据，以及… |
| 133 | [SCOD](https://ngdc.cncb.ac.cn/scod/) | 生物多样性和生物合成 | 未标注 | 描述不足或主要内容不属于当前四类：甘蔗及其近缘物种的综合多组学数据平台。 |
| 134 | [GlobalMGD](https://mdc.im.ac.cn/microxiandao/genome) | 基因组和变异 | DNA | 描述明确：全球微生物基因组数据库系统收集了来自全球的微生物基因组数据，经过统一的质量控制和标准化整合，目前已包含超过51万个高质量的基因组组装序列。平台支持高级检索（如关键字、NMDC编号、提交人姓名等）以及基因组原始序列的批量下载。 |
| 135 | [RDFdb](https://mdc.im.ac.cn/microxiandao/article) | 原始数据 | LITERATURE | 描述明确：生物资源RDF库是一个基于RDF（资源描述框架）技术构建的生物资源文献元数据库。该库整合了与微生物等生物资源相关的学术文献信息，支持通过PMID、PMC、标题、发表年份、期刊、物种、菌株等字段进行精确查询与高级检索。目前页面展示查询和高级检… |
| 137 | [DNAbcQS](https://mdc.im.ac.cn/microxiandao/databaselist) | 原始数据 | DNA | 描述明确：重要检疫物种及其近缘种DNA条形码核心数据库是一个专门针对进出境检疫、入侵生物监测等领域的专业参考数据库。该库重点收录了重要检疫性微生物及其近缘种的DNA条形码序列，每条记录关联了样本ID、物种、科、纲等层级明确的分类等信息，为物种精准鉴定… |
| 138 | [GMCdb](https://mdc.im.ac.cn/microxiandao/microbialCultureCollectionbase) | 原始数据 | 未标注 | 描述不足或主要内容不属于当前四类：全球微生物菌种目录数据库是一个收录全球微生物保藏机构及其菌种资源的目录型数据库。每条记录关联了注册编号、机构缩写、机构全称、菌种类型及菌种总数等信息，涵盖细菌、真菌、酵母、病毒等多种微生物类型，支持对全球菌种保藏资源的查询与检索。 |
| 139 | [MCdb](https://mdc.im.ac.cn/microxiandao/microbialCultureResourceDatabase) | 原始数据 | 未标注 | 描述不足或主要内容不属于当前四类：中国微生物菌种资源数据库是一个收录微生物菌种资源信息的参考数据库。每条记录关联了菌种编号（如CGMCC编号）、菌种名称、分离来源、最适生长温度、状态及应用等关键信息。目前数据库包含超过2万条记录，支持菌种资源的查询与检索。 |
| 140 | [DNAbcIMD](https://mdc.im.ac.cn/microxiandao/dnabarCop) | 原始数据 | DNA | 描述明确：DNA条形码国际镜像数据库是国际DNA条形码数据在中国的镜像站点。该库通过同步国际权威数据库的条形码数据，为国内用户提供稳定、快速的本地化访问服务。目前数据库包含超过285万条记录，每条数据关联了样本ID、物种、科、纲等标准化的分类信息，支… |
| 141 | [PathAMR](http://nmdc.cn/gcpathogen/drugresistance/) | 原始数据 | DNA | 描述明确：病原菌耐药基因数据参考库聚焦病原细菌耐药基因，通过标准化注释流程对NCBI基因组数据进行耐药基因注释（CARD、Resfams、AMRfinder）。收录超39万条序列，涵盖基因名称、药物类型、耐药机制等注释，支持多条件检索及数据下载 |
| 142 | [iPangolin](https://ngdc.cncb.ac.cn/ipangolin/) | 基因组和变异、生物多样性和生物合成 | 未标注 | 描述不足或主要内容不属于当前四类：iPangolin是首个专门针对穿山甲的综合型数据库。当前版本整合了全部9种穿山甲的参考基因组与全基因组变异数据集，甲片图像、头骨和颅内模型等表型信息，以及物种鉴定模型和地理溯源工具。 |
| 143 | [PWAS Atlas](https://ngdc.cncb.ac.cn/pwas/) | 健康和疾病、基因组和变异 | 未标注 | 描述不足或主要内容不属于当前四类：PWAS Atlas是整合文献整理与标准化PWAS分析结果的综合知识库，收录蛋白质—性状关联数据，并提供知识图谱、检索、比较分析、可视化及数据下载功能。 |
| 144 | [π-OmicsKit](https://ngdc.cncb.ac.cn/piomicskit/home) | 原始数据 | 未标注 | 描述不足或主要内容不属于当前四类：π-组学标准处理套件库（π-OmicsKit，https://ngdc.cncb.ac.cn/piomicskit/home）是一个由领域专家构建和审核的组学数据标准化处理资源，旨在实现可重复的原始数据处理。目前，该资源提供80个开箱即用的… |
| 145 | [SesbaniaOmics](https://ngdc.cncb.ac.cn/sesbaniaomics) | 生物多样性和生物合成 | 未标注 | 描述不足或主要内容不属于当前四类：SesbaniaOmics是一个专门针对田菁属植物的多组学综合数据库，整合了基因组、转录组、蛋白质组、表观基因组和表型数据。该资源支持耐盐机制研究，有助于耐逆作物的遗传改良和分子育种。通过提供全面的生物学数据，SesbaniaOmics 推… |
| 146 | [iCellomics](https://ngdc.cncb.ac.cn/icellomics/) | 原始数据、单细胞组学 | 未标注 | 描述不足或主要内容不属于当前四类：iCellOmics 是一个提供工业相关的多组学数据资源，包括基因组注释、变异、转录组学、染色质可及性及基因-表型关系，并配备在线分析工具。它整合了标准化分析结果与文献知识，旨在为工业细胞系研究提供用户友好的探索平台。 |

## 数据库—工具关系

| 数据库 | 工具 | 关系 | 依据 |
| --- | --- | --- | --- |
| [GSA](https://ngdc.cncb.ac.cn/gsa) | [BLAST](https://ngdc.cncb.ac.cn/blast/home) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |
| [GSA](https://ngdc.cncb.ac.cn/gsa) | [Needle](https://ngdc.cncb.ac.cn/bit/needle) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |
| [GSA](https://ngdc.cncb.ac.cn/gsa) | [Water](https://ngdc.cncb.ac.cn/bit/water) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |
| [BioCode](https://ngdc.cncb.ac.cn/biocode) | [KaKs_Calculator](https://ngdc.cncb.ac.cn/biocode/tool/BT000001) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/biocode/tool/BT000001 |
| [BioCode](https://ngdc.cncb.ac.cn/biocode) | [CAT](https://ngdc.cncb.ac.cn/biocode/tool/BT000002) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/biocode/tool/BT000002 |
| [BioCode](https://ngdc.cncb.ac.cn/biocode) | [ParaAT](https://ngdc.cncb.ac.cn/biocode/tool/BT000003) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/biocode/tool/BT000003 |
| [BioCode](https://ngdc.cncb.ac.cn/biocode) | [WBSA](https://ngdc.cncb.ac.cn/biocode/tool/BT000010) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/biocode/tool/BT000010 |
| [BioCode](https://ngdc.cncb.ac.cn/biocode) | [ISVASE](https://ngdc.cncb.ac.cn/biocode/tool/BT004208) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/biocode/tool/BT004208 |
| [BioCode](https://ngdc.cncb.ac.cn/biocode) | [BS-RNA](https://ngdc.cncb.ac.cn/biocode/tool/BT000009) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/biocode/tool/BT000009 |
| [BioCode](https://ngdc.cncb.ac.cn/biocode) | [PGAP](https://ngdc.cncb.ac.cn/biocode/tool/BT000005) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/biocode/tool/BT000005 |
| [BioCode](https://ngdc.cncb.ac.cn/biocode) | [STRsensor](https://ngdc.cncb.ac.cn/biocode/tool/BT007090) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/biocode/tool/BT007090 |
| [BioCode](https://ngdc.cncb.ac.cn/biocode) | [scCapsNet](https://ngdc.cncb.ac.cn/biocode/tool/BT007156) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/biocode/tool/BT007156 |
| [BioCode](https://ngdc.cncb.ac.cn/biocode) | [MultiCapsNet](https://ngdc.cncb.ac.cn/biocode/tool/BT007158) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/biocode/tool/BT007158 |
| [BioCode](https://ngdc.cncb.ac.cn/biocode) | [Phylogenetic tree](https://ngdc.cncb.ac.cn/biocode/tool/BT007276) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/biocode/tool/BT007276 |
| [BioCode](https://ngdc.cncb.ac.cn/biocode) | [TSomVar](https://ngdc.cncb.ac.cn/biocode/tool/BT007299) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/biocode/tool/BT007299 |
| [BioCode](https://ngdc.cncb.ac.cn/biocode) | [McAN](https://ngdc.cncb.ac.cn/biocode/tool/BT007301) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/biocode/tool/BT007301 |
| [BioCode](https://ngdc.cncb.ac.cn/biocode) | [fastQTLmapping](https://ngdc.cncb.ac.cn/biocode/tool/BT007346) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/biocode/tool/BT007346 |
| [BioCode](https://ngdc.cncb.ac.cn/biocode) | [MOWAS](https://ngdc.cncb.ac.cn/biocode/tool/BT007349) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/biocode/tool/BT007349 |
| [BioCode](https://ngdc.cncb.ac.cn/biocode) | [GSA_STAT_reads_classification](https://ngdc.cncb.ac.cn/biocode/tool/BT007358) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/biocode/tool/BT007358 |
| [BioCode](https://ngdc.cncb.ac.cn/biocode) | [GMQN](https://ngdc.cncb.ac.cn/biocode/tool/BT007369) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/biocode/tool/BT007369 |
| [BioCode](https://ngdc.cncb.ac.cn/biocode) | [scCopyATAC](https://ngdc.cncb.ac.cn/biocode/tool/BT007370) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/biocode/tool/BT007370 |
| [BioCode](https://ngdc.cncb.ac.cn/biocode) | [HiRisk-Detector](https://ngdc.cncb.ac.cn/biocode/tool/BT007386) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/biocode/tool/BT007386 |
| [BioCode](https://ngdc.cncb.ac.cn/biocode) | [piplineForQC](https://ngdc.cncb.ac.cn/biocode/tool/BT007415) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/biocode/tool/BT007415 |
| [BioCode](https://ngdc.cncb.ac.cn/biocode) | [inClust](https://ngdc.cncb.ac.cn/biocode/tool/BT007454) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/biocode/tool/BT007454 |
| [BioCode](https://ngdc.cncb.ac.cn/biocode) | [T2T-YAO WES](https://ngdc.cncb.ac.cn/biocode/tool/BT007544) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/biocode/tool/BT007544 |
| [BioCode](https://ngdc.cncb.ac.cn/biocode) | [CIRI3](https://ngdc.cncb.ac.cn/biocode/tool/BT007594) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/biocode/tool/BT007594 |
| [BioCode](https://ngdc.cncb.ac.cn/biocode) | [SCSES](https://ngdc.cncb.ac.cn/biocode/tool/BT007615) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/biocode/tool/BT007615 |
| [BioCode](https://ngdc.cncb.ac.cn/biocode) | [DET](https://ngdc.cncb.ac.cn/biocode/tool/BT007623) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/biocode/tool/BT007623 |
| [BioCode](https://ngdc.cncb.ac.cn/biocode) | [HaploNet-PathoLin](https://ngdc.cncb.ac.cn/biocode/tool/BT007695) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/biocode/tool/BT007695 |
| [BioCode](https://ngdc.cncb.ac.cn/biocode) | [CTAP](https://ngdc.cncb.ac.cn/biocode/tool/BT007697) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/biocode/tool/BT007697 |
| [BioCode](https://ngdc.cncb.ac.cn/biocode) | [HBMRS](https://ngdc.cncb.ac.cn/biocode/tool/BT007702) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/biocode/tool/BT007702 |
| [BioCode](https://ngdc.cncb.ac.cn/biocode) | [PGAP2](https://ngdc.cncb.ac.cn/biocode/tool/BT007758) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/biocode/tool/BT007758 |
| [BioCode](https://ngdc.cncb.ac.cn/biocode) | [RefRGim](https://ngdc.cncb.ac.cn/biocode/tool/BT007766) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/biocode/tool/BT007766 |
| [BioCode](https://ngdc.cncb.ac.cn/biocode) | [chip-seq_pipeline](https://ngdc.cncb.ac.cn/biocode/tool/BT007776) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/biocode/tool/BT007776 |
| [BioCode](https://ngdc.cncb.ac.cn/biocode) | [scm6A-processing](https://ngdc.cncb.ac.cn/biocode/tool/BT007790) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/biocode/tool/BT007790 |
| [BioCode](https://ngdc.cncb.ac.cn/biocode) | [FIPRESCI_Tools](https://ngdc.cncb.ac.cn/biocode/tool/BT007793) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/biocode/tool/BT007793 |
| [BioCode](https://ngdc.cncb.ac.cn/biocode) | [ME Pipeline](https://ngdc.cncb.ac.cn/biocode/tool/BT007800) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/biocode/tool/BT007800 |
| [BioCode](https://ngdc.cncb.ac.cn/biocode) | [fqPack](https://ngdc.cncb.ac.cn/biocode/tool/BT007810) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/biocode/tool/BT007810 |
| [BioCode](https://ngdc.cncb.ac.cn/biocode) | [StreamCompute](https://ngdc.cncb.ac.cn/biocode/tool/BT007811) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/biocode/tool/BT007811 |
| [BioCode](https://ngdc.cncb.ac.cn/biocode) | [SARS-G2V](https://ngdc.cncb.ac.cn/biocode/tool/BT007884) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/biocode/tool/BT007884 |
| [BioCode](https://ngdc.cncb.ac.cn/biocode) | [SARS-Anno](https://ngdc.cncb.ac.cn/biocode/tool/BT007885) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/biocode/tool/BT007885 |
| [BioCode](https://ngdc.cncb.ac.cn/biocode) | [Monkeypox-G2V](https://ngdc.cncb.ac.cn/biocode/tool/BT007887) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/biocode/tool/BT007887 |
| [BioCode](https://ngdc.cncb.ac.cn/biocode) | [Monkeypox-Anno](https://ngdc.cncb.ac.cn/biocode/tool/BT007888) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/biocode/tool/BT007888 |
| [BioCode](https://ngdc.cncb.ac.cn/biocode) | [HaploThread](https://ngdc.cncb.ac.cn/biocode/tool/BT007948) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/biocode/tool/BT007948 |
| [BioCode](https://ngdc.cncb.ac.cn/biocode) | [MRAS](https://ngdc.cncb.ac.cn/biocode/tool/BT007960) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/biocode/tool/BT007960 |
| [BioCode](https://ngdc.cncb.ac.cn/biocode) | [GOT](https://ngdc.cncb.ac.cn/biocode/tool/BT007981) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/biocode/tool/BT007981 |
| [BioCode](https://ngdc.cncb.ac.cn/biocode) | [OmniExtract](https://ngdc.cncb.ac.cn/biocode/tool/BT007992) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/biocode/tool/BT007992 |
| [BioCode](https://ngdc.cncb.ac.cn/biocode) | [scVar](https://ngdc.cncb.ac.cn/biocode/tool/BT008000) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/biocode/tool/BT008000 |
| [BioCode](https://ngdc.cncb.ac.cn/biocode) | [Dingent](https://ngdc.cncb.ac.cn/biocode/tool/BT008001) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/biocode/tool/BT008001 |
| [GSA-Human](https://ngdc.cncb.ac.cn/gsa-human/) | [BLAST](https://ngdc.cncb.ac.cn/blast/home) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |
| [GSA-Human](https://ngdc.cncb.ac.cn/gsa-human/) | [Needle](https://ngdc.cncb.ac.cn/bit/needle) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |
| [GSA-Human](https://ngdc.cncb.ac.cn/gsa-human/) | [Water](https://ngdc.cncb.ac.cn/bit/water) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |
| [GenBase](https://ngdc.cncb.ac.cn/genbase/) | [BLAST](https://ngdc.cncb.ac.cn/blast/home) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |
| [GenBase](https://ngdc.cncb.ac.cn/genbase/) | [Needle](https://ngdc.cncb.ac.cn/bit/needle) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |
| [GenBase](https://ngdc.cncb.ac.cn/genbase/) | [Water](https://ngdc.cncb.ac.cn/bit/water) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |
| [GWAS Atlas](https://ngdc.cncb.ac.cn/gwas/) | [LeadSNPFinder](https://ngdc.cncb.ac.cn/gwas/tools/leadSnpRun) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/gwas/tools/leadSnpRun |
| [GWAS Atlas](https://ngdc.cncb.ac.cn/gwas/) | [GeneFinder](https://ngdc.cncb.ac.cn/gwas/tool/geneFinder) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/gwas/tool/geneFinder |
| [GWH](https://ngdc.cncb.ac.cn/gwh/) | [BLAST](https://ngdc.cncb.ac.cn/blast/home) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |
| [GWH](https://ngdc.cncb.ac.cn/gwh/) | [Needle](https://ngdc.cncb.ac.cn/bit/needle) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |
| [GWH](https://ngdc.cncb.ac.cn/gwh/) | [Water](https://ngdc.cncb.ac.cn/bit/water) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |
| [CompoDynamics](https://ngdc.cncb.ac.cn/compodynamics/) | [SpeciesComparator](https://ngdc.cncb.ac.cn/compodynamics/tools/speciescompare) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/compodynamics/tools/speciescompare |
| [CompoDynamics](https://ngdc.cncb.ac.cn/compodynamics/) | [BLAST](https://ngdc.cncb.ac.cn/blast/home) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |
| [CompoDynamics](https://ngdc.cncb.ac.cn/compodynamics/) | [Needle](https://ngdc.cncb.ac.cn/bit/needle) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |
| [CompoDynamics](https://ngdc.cncb.ac.cn/compodynamics/) | [Water](https://ngdc.cncb.ac.cn/bit/water) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |
| [CGB](https://ngdc.cncb.ac.cn/cgb/) | [BLAST](https://ngdc.cncb.ac.cn/blast/home) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |
| [CGB](https://ngdc.cncb.ac.cn/cgb/) | [Needle](https://ngdc.cncb.ac.cn/bit/needle) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |
| [CGB](https://ngdc.cncb.ac.cn/cgb/) | [Water](https://ngdc.cncb.ac.cn/bit/water) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |
| [GEN](https://ngdc.cncb.ac.cn/gen/) | [RNA-seq Analysis](https://ngdc.cncb.ac.cn/gen/analysis) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/gen/analysis |
| [GEN](https://ngdc.cncb.ac.cn/gen/) | [Bulk RNA-seq Data Analysis](https://ngdc.cncb.ac.cn/gen/online/tools/) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/gen/online/tools/ |
| [GEN](https://ngdc.cncb.ac.cn/gen/) | [scRNA-SeqDA](https://ngdc.cncb.ac.cn/gen/online/tools_cerobro/) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/gen/online/tools_cerobro/ |
| [GEN](https://ngdc.cncb.ac.cn/gen/) | [Visualization of scRNA-seq Data Analysis Results](https://ngdc.cncb.ac.cn/gen/online/visualization4sc/) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/gen/online/visualization4sc/ |
| [GEN](https://ngdc.cncb.ac.cn/gen/) | [GENtools-bulk](https://ngdc.cncb.ac.cn/gen/online/tools/) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/gen/online/tools/ |
| [LncRNAWiki](https://ngdc.cncb.ac.cn/lncrnawiki) | [Functional Prediction](https://ngdc.cncb.ac.cn/lncrnawiki/tool) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/lncrnawiki/tool |
| [LncRNAWiki](https://ngdc.cncb.ac.cn/lncrnawiki) | [LncBot](https://ngdc.cncb.ac.cn/lncrnawiki/lncbot) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/lncrnawiki/lncbot |
| [LncBook](http://ngdc.cncb.ac.cn/lncbook) | [ID conversion](https://ngdc.cncb.ac.cn/lncbook/tools/conversion) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/lncbook/tools/conversion |
| [LncBook](http://ngdc.cncb.ac.cn/lncbook) | [lncbook-BLAST](https://ngdc.cncb.ac.cn/lncbook/tools/blast) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/lncbook/tools/blast |
| [LncBook](http://ngdc.cncb.ac.cn/lncbook) | [Classification](https://ngdc.cncb.ac.cn/lncbook/tools/classification) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/lncbook/tools/classification |
| [MethBank](https://ngdc.cncb.ac.cn/methbank) | [Age Predictor](https://ngdc.cncb.ac.cn/methbank/tools/age/predictor) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/methbank/tools/age/predictor |
| [MethBank](https://ngdc.cncb.ac.cn/methbank) | [DMR Toolkit](https://ngdc.cncb.ac.cn/methbank/tools/dmr/toolkit) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/methbank/tools/dmr/toolkit |
| [MethBank](https://ngdc.cncb.ac.cn/methbank) | [IDMP](https://ngdc.cncb.ac.cn/methbank/tools/idmp) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/methbank/tools/idmp |
| [NucMap](http://ngdc.cncb.ac.cn/nucmap) | [Enrichment analysis in nucleosome occupancy](https://ngdc.cncb.ac.cn/nucmap/enrichment-analysis) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/nucmap/enrichment-analysis |
| [NucMap](http://ngdc.cncb.ac.cn/nucmap) | [CAIN](https://ngdc.cncb.ac.cn/nucmap/comparative-analysis) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/nucmap/comparative-analysis |
| [EWAS Atlas](http://ngdc.cncb.ac.cn/ewas) | [GMQN](https://ngdc.cncb.ac.cn/ewas/toolkitGmqn) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/ewas/toolkitGmqn |
| [EWAS Atlas](http://ngdc.cncb.ac.cn/ewas) | [EWAS Network Visualization](https://ngdc.cncb.ac.cn/ewas/network) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/ewas/network |
| [EWAS Atlas](http://ngdc.cncb.ac.cn/ewas) | [Enrichment & Annotation](https://ngdc.cncb.ac.cn/ewas/toolkit) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/ewas/toolkit |
| [Cell Taxonomy](https://ngdc.cncb.ac.cn/celltaxonomy/) | [CTC](https://ngdc.cncb.ac.cn/celltaxonomy/ctc) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/celltaxonomy/ctc |
| [Cell Taxonomy](https://ngdc.cncb.ac.cn/celltaxonomy/) | [CTP](https://ngdc.cncb.ac.cn/celltaxonomy/ctp) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/celltaxonomy/ctp |
| [iDOG](https://ngdc.cncb.ac.cn/idog) | [BLAST](https://ngdc.cncb.ac.cn/blast/home) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |
| [iDOG](https://ngdc.cncb.ac.cn/idog) | [Needle](https://ngdc.cncb.ac.cn/bit/needle) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |
| [iDOG](https://ngdc.cncb.ac.cn/idog) | [Water](https://ngdc.cncb.ac.cn/bit/water) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |
| [TCOD](https://ngdc.cncb.ac.cn/tcod) | [Homolog Finder](https://ngdc.cncb.ac.cn/tcod/homolog) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/tcod/homolog |
| [TCOD](https://ngdc.cncb.ac.cn/tcod) | [BLAST](https://ngdc.cncb.ac.cn/blast/home) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |
| [TCOD](https://ngdc.cncb.ac.cn/tcod) | [Needle](https://ngdc.cncb.ac.cn/bit/needle) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |
| [TCOD](https://ngdc.cncb.ac.cn/tcod) | [Water](https://ngdc.cncb.ac.cn/bit/water) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |
| [EDK](https://ngdc.cncb.ac.cn/edk) | [Editing site identification](https://ngdc.cncb.ac.cn/edk/tool/editing/identifier) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/edk/tool/editing/identifier |
| [EDK](https://ngdc.cncb.ac.cn/edk) | [EDK-DP](https://ngdc.cncb.ac.cn/edk/tool/disease/predictor) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/edk/tool/disease/predictor |
| [EDK](https://ngdc.cncb.ac.cn/edk) | [GDNC](https://ngdc.cncb.ac.cn/edk/tool/gene/network) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/edk/tool/gene/network |
| [EDK](https://ngdc.cncb.ac.cn/edk) | [CDA](https://ngdc.cncb.ac.cn/edk/tool/crossdisease) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/edk/tool/crossdisease |
| [EDK](https://ngdc.cncb.ac.cn/edk) | [Editing site annotation](https://ngdc.cncb.ac.cn/edk/tool/site/annotation) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/edk/tool/site/annotation |
| [RCoV19](https://ngdc.cncb.ac.cn/ncov) | [Fastq-to-Variants](https://ngdc.cncb.ac.cn/ncov/online/tool/variation) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/ncov/online/tool/variation |
| [RCoV19](https://ngdc.cncb.ac.cn/ncov) | [COVID-19 genome variation annotation](https://ngdc.cncb.ac.cn/ncov/online/tool/annotation) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/ncov/online/tool/annotation |
| [RCoV19](https://ngdc.cncb.ac.cn/ncov) | [COVID-19 haplotype analysis](https://ngdc.cncb.ac.cn/ncov/online/tool/haplotype) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/ncov/online/tool/haplotype |
| [RCoV19](https://ngdc.cncb.ac.cn/ncov) | [Pangolin COVID-19 Lineage Assigner](https://ngdc.cncb.ac.cn/ncov/online/tool/lineage-assigner) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/ncov/online/tool/lineage-assigner |
| [RCoV19](https://ngdc.cncb.ac.cn/ncov) | [BLAST](https://ngdc.cncb.ac.cn/blast/home) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |
| [RCoV19](https://ngdc.cncb.ac.cn/ncov) | [Genome Tracing](https://ngdc.cncb.ac.cn/ncov/online/tool/genome-tracing/) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/ncov/online/tool/genome-tracing/ |
| [RCoV19](https://ngdc.cncb.ac.cn/ncov) | [Denovo Assembly](https://ngdc.cncb.ac.cn/ncov/online/tool/denovo-assembly) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/ncov/online/tool/denovo-assembly |
| [RCoV19](https://ngdc.cncb.ac.cn/ncov) | [Genome Annotation](https://ngdc.cncb.ac.cn/ncov/online/tool/genome-annotation) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/ncov/online/tool/genome-annotation |
| [RCoV19](https://ngdc.cncb.ac.cn/ncov) | [Evolutionary tree](https://ngdc.cncb.ac.cn/ncov/online/tool/tree) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/ncov/online/tool/tree |
| [RCoV19](https://ngdc.cncb.ac.cn/ncov) | [GEA](https://ngdc.cncb.ac.cn/ncov/online/tool/lineage-tree) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/ncov/online/tool/lineage-tree |
| [RCoV19](https://ngdc.cncb.ac.cn/ncov) | [Needle](https://ngdc.cncb.ac.cn/bit/needle) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |
| [RCoV19](https://ngdc.cncb.ac.cn/ncov) | [Water](https://ngdc.cncb.ac.cn/bit/water) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |
| [RCoV19](https://ngdc.cncb.ac.cn/ncov) | [VISTA](https://ngdc.cncb.ac.cn/vista/) | COMPATIBLE | 数据库明确存储病毒基因组序列；VISTA提供基于病毒序列的分类功能。 |
| [ASCancer Atlas](https://ngdc.cncb.ac.cn/ascancer) | [CASA](https://ngdc.cncb.ac.cn/ascancer/tools) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/ascancer/tools |
| [SmProt](http://bigdata.ibp.ac.cn/SmProt/) | [BLAST](https://ngdc.cncb.ac.cn/blast/home) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |
| [SmProt](http://bigdata.ibp.ac.cn/SmProt/) | [Needle](https://ngdc.cncb.ac.cn/bit/needle) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |
| [SmProt](http://bigdata.ibp.ac.cn/SmProt/) | [Water](https://ngdc.cncb.ac.cn/bit/water) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |
| [GTDB](https://www.biosino.org/gtdb/) | [BLAST](https://ngdc.cncb.ac.cn/blast/home) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |
| [GTDB](https://www.biosino.org/gtdb/) | [Needle](https://ngdc.cncb.ac.cn/bit/needle) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |
| [GTDB](https://www.biosino.org/gtdb/) | [Water](https://ngdc.cncb.ac.cn/bit/water) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |
| [OpenLB](https://ngdc.cncb.ac.cn/openlb) | [OpenLB-API](https://ngdc.cncb.ac.cn/openlb/doc-api) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/openlb/doc-api |
| [OpenLB](https://ngdc.cncb.ac.cn/openlb) | [OpenLB API](https://ngdc.cncb.ac.cn/openlb/doc-api) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/openlb/doc-api |
| [OPIA](https://ngdc.cncb.ac.cn/opia/) | [RiceYE](https://ngdc.cncb.ac.cn/opia/tools/rice_yield_wgsr) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/opia/tools/rice_yield_wgsr |
| [OPIA](https://ngdc.cncb.ac.cn/opia/) | [WheatHE](https://ngdc.cncb.ac.cn/opia/tools/wheat_head_gwhd2020) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/opia/tools/wheat_head_gwhd2020 |
| [OPIA](https://ngdc.cncb.ac.cn/opia/) | [RiceVI](https://ngdc.cncb.ac.cn/opia/tools/identify_rice_varieties) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/opia/tools/identify_rice_varieties |
| [OPIA](https://ngdc.cncb.ac.cn/opia/) | [IC](https://ngdc.cncb.ac.cn/opia/tools/cropping) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/opia/tools/cropping |
| [OPIA](https://ngdc.cncb.ac.cn/opia/) | [IR](https://ngdc.cncb.ac.cn/opia/tools/resizing) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/opia/tools/resizing |
| [OPIA](https://ngdc.cncb.ac.cn/opia/) | [IF](https://ngdc.cncb.ac.cn/opia/tools/flipping) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/opia/tools/flipping |
| [OPIA](https://ngdc.cncb.ac.cn/opia/) | [ID](https://ngdc.cncb.ac.cn/opia/tools/denoising) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/opia/tools/denoising |
| [OPIA](https://ngdc.cncb.ac.cn/opia/) | [IP](https://ngdc.cncb.ac.cn/opia/tools/partitioning) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/opia/tools/partitioning |
| [HemAtlas](https://ngdc.cncb.ac.cn/hematlas/) | [CTA](https://ngdc.cncb.ac.cn/hematlas/featured/organs/) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/hematlas/featured/organs/ |
| [HemAtlas](https://ngdc.cncb.ac.cn/hematlas/) | [Cross-species analysis](https://ngdc.cncb.ac.cn/hematlas/featured/species/) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/hematlas/featured/species/ |
| [HemAtlas](https://ngdc.cncb.ac.cn/hematlas/) | [CMA](https://ngdc.cncb.ac.cn/hematlas/featured/invitro/) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/hematlas/featured/invitro/ |
| [MPoxVR](https://ngdc.cncb.ac.cn/gwh/poxvirus/) | [Monkeypox virus genome variation identification](https://ngdc.cncb.ac.cn/gwh/poxvirus/online/tools) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/gwh/poxvirus/online/tools |
| [MPoxVR](https://ngdc.cncb.ac.cn/gwh/poxvirus/) | [Monkeypox virus genome variation annotation](https://ngdc.cncb.ac.cn/gwh/poxvirus/online/tool/annotation) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/gwh/poxvirus/online/tool/annotation |
| [MPoxVR](https://ngdc.cncb.ac.cn/gwh/poxvirus/) | [BLAST](https://ngdc.cncb.ac.cn/blast/home) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |
| [MPoxVR](https://ngdc.cncb.ac.cn/gwh/poxvirus/) | [Needle](https://ngdc.cncb.ac.cn/bit/needle) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |
| [MPoxVR](https://ngdc.cncb.ac.cn/gwh/poxvirus/) | [Water](https://ngdc.cncb.ac.cn/bit/water) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |
| [MPoxVR](https://ngdc.cncb.ac.cn/gwh/poxvirus/) | [VISTA](https://ngdc.cncb.ac.cn/vista/) | COMPATIBLE | 数据库明确存储病毒基因组序列；VISTA提供基于病毒序列的分类功能。 |
| [Propan](https://ngdc.cncb.ac.cn/propan/) | [BLAST](https://ngdc.cncb.ac.cn/blast/home) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |
| [Propan](https://ngdc.cncb.ac.cn/propan/) | [Needle](https://ngdc.cncb.ac.cn/bit/needle) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |
| [Propan](https://ngdc.cncb.ac.cn/propan/) | [Water](https://ngdc.cncb.ac.cn/bit/water) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |
| [SoyOmics](https://ngdc.cncb.ac.cn/soyomics/) | [easyGWAS](https://ngdc.cncb.ac.cn/soyomics/tools/gwas/run) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/soyomics/tools/gwas/run |
| [SoyOmics](https://ngdc.cncb.ac.cn/soyomics/) | [ExpPattern](https://ngdc.cncb.ac.cn/soyomics/expression_tool/) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/soyomics/expression_tool/ |
| [SoyOmics](https://ngdc.cncb.ac.cn/soyomics/) | [HapSnap](https://ngdc.cncb.ac.cn/soyomics/haplotype/) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/soyomics/haplotype/ |
| [SoyOmics](https://ngdc.cncb.ac.cn/soyomics/) | [SeqFetch](https://ngdc.cncb.ac.cn/soyomics/seqFetch/run) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/soyomics/seqFetch/run |
| [SoyOmics](https://ngdc.cncb.ac.cn/soyomics/) | [BLAST](https://ngdc.cncb.ac.cn/blast/home) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |
| [SoyOmics](https://ngdc.cncb.ac.cn/soyomics/) | [Needle](https://ngdc.cncb.ac.cn/bit/needle) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |
| [SoyOmics](https://ngdc.cncb.ac.cn/soyomics/) | [Water](https://ngdc.cncb.ac.cn/bit/water) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |
| [scMethBank](https://ngdc.cncb.ac.cn/methbank/scm/) | [DMR Annotation](https://ngdc.cncb.ac.cn/methbank/scm/tool/annotation) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/methbank/scm/tool/annotation |
| [scMethBank](https://ngdc.cncb.ac.cn/methbank/scm/) | [Lollipop Plotter](https://ngdc.cncb.ac.cn/methbank/scm/plotDot) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/methbank/scm/plotDot |
| [CGIR](https://ngdc.cncb.ac.cn/cgir/) | [BarcodeBLAST](https://ngdc.cncb.ac.cn/cgir/tool/barcode/) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/cgir/tool/barcode/ |
| [CGIR](https://ngdc.cncb.ac.cn/cgir/) | [BarcodeFinder](https://ngdc.cncb.ac.cn/cgir/tool/barcodeFinder/) | NATIVE | 工具入口位于数据库官方URL路径下：https://ngdc.cncb.ac.cn/cgir/tool/barcodeFinder/ |
| [CGIR](https://ngdc.cncb.ac.cn/cgir/) | [BLAST](https://ngdc.cncb.ac.cn/blast/home) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |
| [CGIR](https://ngdc.cncb.ac.cn/cgir/) | [Needle](https://ngdc.cncb.ac.cn/bit/needle) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |
| [CGIR](https://ngdc.cncb.ac.cn/cgir/) | [Water](https://ngdc.cncb.ac.cn/bit/water) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |
| [P10K](https://ngdc.cncb.ac.cn/p10k) | [BLAST](https://ngdc.cncb.ac.cn/blast/home) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |
| [P10K](https://ngdc.cncb.ac.cn/p10k) | [Needle](https://ngdc.cncb.ac.cn/bit/needle) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |
| [P10K](https://ngdc.cncb.ac.cn/p10k) | [Water](https://ngdc.cncb.ac.cn/bit/water) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |
| [GlobalMGD](https://mdc.im.ac.cn/microxiandao/genome) | [BLAST](https://ngdc.cncb.ac.cn/blast/home) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |
| [GlobalMGD](https://mdc.im.ac.cn/microxiandao/genome) | [Needle](https://ngdc.cncb.ac.cn/bit/needle) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |
| [GlobalMGD](https://mdc.im.ac.cn/microxiandao/genome) | [Water](https://ngdc.cncb.ac.cn/bit/water) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |
| [DNAbcQS](https://mdc.im.ac.cn/microxiandao/databaselist) | [BarcodeBLAST](https://ngdc.cncb.ac.cn/cgir/tool/barcode/) | COMPATIBLE | 数据库明确存储DNA条形码序列；工具提供条形码比对或条形码识别功能。 |
| [DNAbcQS](https://mdc.im.ac.cn/microxiandao/databaselist) | [BarcodeFinder](https://ngdc.cncb.ac.cn/cgir/tool/barcodeFinder/) | COMPATIBLE | 数据库明确存储DNA条形码序列；工具提供条形码比对或条形码识别功能。 |
| [DNAbcQS](https://mdc.im.ac.cn/microxiandao/databaselist) | [BLAST](https://ngdc.cncb.ac.cn/blast/home) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |
| [DNAbcQS](https://mdc.im.ac.cn/microxiandao/databaselist) | [Needle](https://ngdc.cncb.ac.cn/bit/needle) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |
| [DNAbcQS](https://mdc.im.ac.cn/microxiandao/databaselist) | [Water](https://ngdc.cncb.ac.cn/bit/water) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |
| [DNAbcIMD](https://mdc.im.ac.cn/microxiandao/dnabarCop) | [BarcodeBLAST](https://ngdc.cncb.ac.cn/cgir/tool/barcode/) | COMPATIBLE | 数据库明确存储DNA条形码序列；工具提供条形码比对或条形码识别功能。 |
| [DNAbcIMD](https://mdc.im.ac.cn/microxiandao/dnabarCop) | [BarcodeFinder](https://ngdc.cncb.ac.cn/cgir/tool/barcodeFinder/) | COMPATIBLE | 数据库明确存储DNA条形码序列；工具提供条形码比对或条形码识别功能。 |
| [DNAbcIMD](https://mdc.im.ac.cn/microxiandao/dnabarCop) | [BLAST](https://ngdc.cncb.ac.cn/blast/home) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |
| [DNAbcIMD](https://mdc.im.ac.cn/microxiandao/dnabarCop) | [Needle](https://ngdc.cncb.ac.cn/bit/needle) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |
| [DNAbcIMD](https://mdc.im.ac.cn/microxiandao/dnabarCop) | [Water](https://ngdc.cncb.ac.cn/bit/water) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |
| [PathAMR](http://nmdc.cn/gcpathogen/drugresistance/) | [BLAST](https://ngdc.cncb.ac.cn/blast/home) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |
| [PathAMR](http://nmdc.cn/gcpathogen/drugresistance/) | [Needle](https://ngdc.cncb.ac.cn/bit/needle) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |
| [PathAMR](http://nmdc.cn/gcpathogen/drugresistance/) | [Water](https://ngdc.cncb.ac.cn/bit/water) | COMPATIBLE | 数据库明确存储序列；工具目录描述表明该工具提供核酸或蛋白序列比对。 |

## 明确排除示例

| 数据库 | 未打标签 | 原因 |
| --- | --- | --- |
| BioProject | DNA、文献 | 保存项目、提交者、基金和文献关联元数据，不是序列库或文献库 |
| GVM | DNA | 保存SNP/INDEL等变异记录，不等同保存DNA序列 |
| MethBank / EWAS Data Hub | DNA | 保存甲基化组或甲基化值，不等同DNA序列 |
| Cell Taxonomy | 物种 | 核心对象是细胞类型和标记，species只是覆盖范围 |
| BioKA | 物种 | 核心对象是动物生物标志物，物种只是记录维度 |

## 待人工复核

- “genomic data”“multi-omics”“pan-genome analysis results”等描述未明确是否保存可检索/下载序列时，本轮不标。
- NATIVE按URL路径归属判断，仍应抽查页面是否为可运行工具、下载代码还是介绍页。
- COMPATIBLE只表示数据与功能相容，不代表工具可自动接收该数据库记录或支持URL预填。
- SQL使用上游db_id和bt_id反查本地主键，可在自增ID不同的数据库中重复执行。

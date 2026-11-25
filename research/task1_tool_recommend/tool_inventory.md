# 在线工具与BIT平台调研

调研日期：2026-11-06。以下各行访问日期均为本日。**目录存在、配置页可读、任务运行成功、公开API可用是四个不同结论。此次未提交计算任务，运行成功数量未确认。**

## BIT确认情况

官方入口：[Bioinformatics Toolkit](https://ngdc.cncb.ac.cn/bit/)。本次打开首页显示148个工具，分类计数合计148。搜索缓存和部分旧详情页仍显示146或9个单细胞工具，与最新首页的11个不同；采用首页148的口径，不当作全部服务已可运行。

- 在线调用：Needle等页面可读取输入配置、Submit按钮及结果示例，确认提供在线计算界面；实际提交、排队、输出、会话要求未验证。
- 工具详情与直达URL：已确认，例如[Needle](https://ngdc.cncb.ac.cn/bit/needle)。直达不等于支持URL预填数据或免登录执行。
- API：本次官方检索未找到公开的统一BIT任务提交/状态/结果API文档。不将网页内部请求当作公开稳定API。
- 数据范围：DNA和蛋白序列有Needle实证；物种关联分析有条形码等目录入口。没有找到可确认面向文献计量的BIT工具。RNA、甲基化、变异也不能强行归为DNA序列输入。
- 归属：BIT链接有的跳到独立数据库，有的跳BioCode详情或外站。收录不能证明中心自研、中心托管或中心可计算。
- 用户手册：[官方PDF](https://ngdc.cncb.ac.cn/bit/example/NGDC_BIT_User_Manual_v3.pdf)，本环境读取因24,968,796字节超过工具限制失败，未完成全文核验。

## 32个重点工具与服务入口

含分析工具、浏览服务及少量帮助入口；**32不是32个已成功运行的在线工具**。支持在线调用列描述页面证据，登录列保持未知，不因能看见Sign in而推断强制登录。源数据库未证实则明确不指定。

| ID | 工具/服务名称 | 所属数据库 | 功能 | 支持数据类型 | 输入类型 | 工具访问地址 | 所属平台 | 在线调用与验证状态 | 是否需要登录 | 能否直接跳转 | 信息来源 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| T01 | BLAST | 跨库/中心BLAST | 核酸或蛋白序列相似性比对 | DNA/蛋白质 | 序列；具体格式待核 | [入口](https://ngdc.cncb.ac.cn/blast/home) | 中心BLAST（BIT导航跳转） | 官方文档确认；目标页面空壳 | 待验证；页面无正文 | 是（入口）；数据预填未证实 | [官方来源](https://www.cncb.ac.cn/) |
| T02 | Needle | 无特定数据库归属确认 | 双序列全局比对 | DNA/蛋白质 | 两条序列文本或文件 | [入口](https://ngdc.cncb.ac.cn/bit/needle) | BIT | 已验证配置页；运行待验证 | 页面公开；提交是否要求登录待验证 | 是 | [官方来源](https://ngdc.cncb.ac.cn/bit/needle) |
| T03 | Water | 无特定数据库归属确认 | 双序列局部比对 | 序列；DNA/蛋白类型需复核 | 两条序列文本或文件 | [入口](https://ngdc.cncb.ac.cn/bit/water) | BIT | 已验证配置页；运行待验证 | 待验证 | 是 | [官方来源](https://ngdc.cncb.ac.cn/bit/water) |
| T04 | Dotmatcher | 无特定数据库归属确认 | 两序列阈值点阵图 | DNA/蛋白质 | 两条序列文本或文件 | [入口](https://ngdc.cncb.ac.cn/bit/dotmatcher) | BIT | 已验证配置页；运行待验证 | 待验证 | 是 | [官方来源](https://ngdc.cncb.ac.cn/bit/dotmatcher) |
| T05 | Dotpath | 无特定数据库归属确认 | 两序列非重叠word匹配点阵图 | 序列；具体分子类型待核 | 两条序列文本或文件 | [入口](https://ngdc.cncb.ac.cn/bit/dotpath) | BIT | 已验证配置页；运行待验证 | 待验证 | 是 | [官方来源](https://ngdc.cncb.ac.cn/bit/dotpath) |
| T06 | Dottup | 无特定数据库归属确认 | 序列word匹配可视化 | 序列；具体分子类型待核 | 两条序列文本或文件 | [入口](https://ngdc.cncb.ac.cn/bit/dottup) | BIT | 已验证输入页；运行待验证 | 待验证 | 是 | [官方来源](https://ngdc.cncb.ac.cn/bit/dottup) |
| T07 | Polydot | 无特定数据库归属确认 | 多序列点阵图（具体参数待复核） | 序列；具体类型待核 | 序列输入；格式待核 | [入口](https://ngdc.cncb.ac.cn/bit/polydot) | BIT | 已验证页面；参数/运行待验证 | 待验证 | 是 | [官方来源](https://ngdc.cncb.ac.cn/bit/polydot) |
| T08 | Genome Browser | GWH | 在线基因组可视化 | DNA/基因组注释 | 基因组、染色体、区间 | [入口](https://ngdc.cncb.ac.cn/gwh/browser/index/Zea_mays/GWHAAJX00000000/GWHAAJX00000002/1/200) | GWH | 已验证浏览器入口；轨道交互待验证 | 待验证 | 是（官网示例上下文） | [官方来源](https://ngdc.cncb.ac.cn/gwh/api_documents) |
| T09 | Clustal Omega | Taxonomy | 多序列比对与系统进化入口 | DNA/蛋白质/RNA | 粘贴序列或.fa/.fasta/.fna，页面标注最大4MB | [入口](https://ngdc.cncb.ac.cn/taxonomy/clustalOmega) | Taxonomy | 已验证配置页；构树结果待验证 | 待验证 | 是 | [官方来源](https://ngdc.cncb.ac.cn/taxonomy/clustalOmega) |
| T10 | MAFFT | Taxonomy | 多序列比对/系统进化工具入口 | 序列；具体分子类型待核 | 序列；格式上限待复核 | [入口](https://ngdc.cncb.ac.cn/taxonomy/mafft) | Taxonomy | 已验证页面；运行待验证 | 待验证 | 是 | [官方来源](https://ngdc.cncb.ac.cn/taxonomy/mafft) |
| T11 | Kalign | Taxonomy | 多序列比对工具入口 | 序列；具体分子类型待核 | 序列；格式待复核 | [入口](https://ngdc.cncb.ac.cn/taxonomy/kalign) | Taxonomy | 已验证页面；运行待验证 | 待验证 | 是 | [官方来源](https://ngdc.cncb.ac.cn/taxonomy/kalign) |
| T12 | MUSCLE | Taxonomy | 多序列比对工具入口 | 序列；具体分子类型待核 | 序列；格式待复核 | [入口](https://ngdc.cncb.ac.cn/taxonomy/muscle) | Taxonomy | 已验证页面；运行待验证 | 待验证 | 是 | [官方来源](https://ngdc.cncb.ac.cn/taxonomy/muscle) |
| T13 | Biological Barcode Identification | Taxonomy | DNA条形码鉴定：BLAST、比对、构树定位 | DNA/物种 | 页面明确仅DNA序列 | [入口](https://ngdc.cncb.ac.cn/taxonomy/barCode) | Taxonomy | 已验证输入页和功能说明；运行待验证 | 待验证 | 是 | [官方来源](https://ngdc.cncb.ac.cn/taxonomy/barCode) |
| T14 | Taxonomy详情/分类画像候选 | Taxonomy | 分类名称、谱系和分类树导航；不是已证实完整物种画像 | 物种 | Taxonomy ID/学名；示例9605为Homo属 | [入口](https://ngdc.cncb.ac.cn/taxonomy/detail?id=9605) | Taxonomy | 已验证详情入口；画像范围待人工核实 | 待验证 | 是（示例参数来自官网链接） | [官方来源](https://ngdc.cncb.ac.cn/taxonomy/index_new) |
| T15 | Evolutionary tree | RCoV19 | 病毒系统进化构建入口 | 病毒序列；确切输入约束待核 | 序列；格式/数量待核 | [入口](https://ngdc.cncb.ac.cn/ncov/online/tool/tree) | RCoV19（BIT收录） | 已验证工具页；运行待验证 | 待验证 | 是 | [官方来源](https://ngdc.cncb.ac.cn/ncov/online/tool/tree) |
| T16 | McAN | BIT收录；BioCode详情 | 单体型网络构建及可视化（官方培训说明） | 病毒/核酸；确切输入待核 | 待核 | [入口](https://ngdc.cncb.ac.cn/biocode/tool/BT007301) | BIT→BioCode | 已验证详情；此链接不是已证实计算服务 | 待验证 | 是（详情）；在线执行未确认 | [官方来源](https://ngdc.cncb.ac.cn/biocode/tool/BT007301) |
| T17 | Age Predictor | MethBank | 人类血液DNA甲基化年龄预测 | DNA甲基化（不是DNA序列输入） | 甲基化资料；格式待核 | [入口](https://ngdc.cncb.ac.cn/methbank/tools/age/predictor) | MethBank（BIT收录） | 已验证工具页；运行待验证 | 待验证 | 是 | [官方来源](https://ngdc.cncb.ac.cn/methbank/) |
| T18 | IDMP | MethBank | 差异甲基化启动子识别 | DNA甲基化 | 甲基化资料；格式待核 | [入口](https://ngdc.cncb.ac.cn/methbank/tools/idmp) | MethBank（BIT收录） | 已验证工具页；运行待验证 | 待验证 | 是 | [官方来源](https://ngdc.cncb.ac.cn/methbank/) |
| T19 | DMR Toolkit | MethBank | 差异甲基化区域分析 | DNA甲基化 | 甲基化资料；格式待核 | [入口](https://ngdc.cncb.ac.cn/methbank/tools/dmr/toolkit) | MethBank（BIT收录） | 已验证工具页；运行待验证 | 待验证 | 是 | [官方来源](https://ngdc.cncb.ac.cn/methbank/) |
| T20 | HapView | GVM | 变异/单体型浏览分析入口；功能边界待核 | 变异 | 待核 | [入口](https://ngdc.cncb.ac.cn/gvmhp/hapview) | GVM | 已验证页面；运行待验证 | 待验证 | 是 | [官方来源](https://ngdc.cncb.ac.cn/gvm/) |
| T21 | VersionMap | GVM | 基因组版本坐标转换 | 变异/坐标 | 待核 | [入口](https://ngdc.cncb.ac.cn/gvmhp/versionmap) | GVM（BIT收录） | 官方导航确认；目标502 | 待验证 | 有URL；本环境不可用 | [官方来源](https://ngdc.cncb.ac.cn/gvm/) |
| T22 | lncbook-BLAST | LncBook | lncRNA序列比对入口 | RNA/核酸；DNA适配待核 | 序列；格式待核 | [入口](https://ngdc.cncb.ac.cn/lncbook/tools/blast) | LncBook（BIT收录） | 官方导航确认；目标445 | 待验证 | 有URL；本环境不可用 | [官方来源](https://ngdc.cncb.ac.cn/lncbook/) |
| T23 | LGC | LncBook导航关联；独立LGC站 | 编码潜能分析入口；细节待核 | 核酸序列（具体分子范围待核） | 待核 | [入口](https://ngdc.cncb.ac.cn/lgc/) | LGC（BIT/LncBook收录） | 已验证主页；运行待验证 | 待验证 | 是 | [官方来源](https://ngdc.cncb.ac.cn/lgc/) |
| T24 | ID Conversion | LncBook | 跨库lncRNA标识转换 | RNA标识；不是序列比对 | ID；支持列表待核 | [入口](https://ngdc.cncb.ac.cn/lncbook/tools/conversion) | LncBook（BIT收录） | 已验证页面；运行待验证 | 待验证 | 是 | [官方来源](https://ngdc.cncb.ac.cn/lncbook/tools/conversion) |
| T25 | Classification | LncBook | 分类工具入口；算法与输入待核 | 待核 | 待核 | [入口](https://ngdc.cncb.ac.cn/lncbook/tools/classification) | LncBook（BIT收录） | 官方导航确认；目标445 | 待验证 | 有URL；本环境不可用 | [官方来源](https://ngdc.cncb.ac.cn/lncbook/) |
| T26 | Online Analysis | GEN | 转录组在线分析入口；不能当成单一算法 | RNA表达 | 待核 | [入口](https://ngdc.cncb.ac.cn/gen/analysis) | GEN | 已验证工具入口；子工具运行待验证 | 待验证 | 是 | [官方来源](https://ngdc.cncb.ac.cn/gen/analysis) |
| T27 | LncNet | LncExpDB | lncRNA-mRNA共表达网络可视化 | RNA表达/基因ID | 基因ID或Symbol | [入口](https://ngdc.cncb.ac.cn/lncexpdb/lncnet) | LncExpDB | 官方索引确认输入和图表；交互待验证 | 待验证 | 是（索引入口） | [官方来源](https://ngdc.cncb.ac.cn/lncexpdb/lncnet) |
| T28 | BLAST（IC4R） | IC4R | 水稻资源序列比对入口 | 序列；DNA/蛋白选项待核 | 待核 | [入口](https://ngdc.cncb.ac.cn/ic4r/blast) | IC4R | 已验证页面；运行待验证 | 待验证 | 是 | [官方来源](https://ngdc.cncb.ac.cn/ic4r/blast) |
| T29 | Genome Browser（IC4R） | IC4R | 水稻基因组可视化 | DNA/基因组注释 | 官网预置区间；输入规范待核 | [入口](https://bigd.big.ac.cn/browser/?data=data%2FOryza_sativa&highlight=&loc=1%3A13004..14283&overview=0&tracks=DNA%2CIC4R%2COSA1R7%2CRPDB) | 旧域名浏览器 | 官方导航确认；目标Cache miss | 待验证 | 旧链接；需人工核实迁移 | [官方来源](https://ngdc.cncb.ac.cn/ic4r/) |
| T30 | ID Conversion（IC4R） | IC4R | 水稻基因标识转换 | 基因ID | 基因ID；格式待复核 | [入口](https://ngdc.cncb.ac.cn/ic4r/idmapper) | IC4R | 已验证页面；运行待验证 | 待验证 | 是 | [官方来源](https://ngdc.cncb.ac.cn/ic4r/idmapper) |
| T31 | HK-TS Gene Finder | IC4R | 管家/组织特异基因查找入口；判定细节待核 | 表达/基因 | 待核 | [入口](https://ngdc.cncb.ac.cn/ic4r/hk-ts) | IC4R | 已验证页面；运行待验证 | 待验证 | 是 | [官方来源](https://ngdc.cncb.ac.cn/ic4r/hk-ts) |
| T32 | 序列校验 | GenBase | 提交前序列校验帮助入口；不确认独立在线服务 | 核酸 | 待核 | [入口](https://ngdc.cncb.ac.cn/genbase/gbthelp) | GenBase | 官方导航确认；目标超时 | 待验证 | 有URL；本环境不可用 | [官方来源](https://ngdc.cncb.ac.cn/genbase/) |

## BIT完整目录抄录

以下名称为本次官方页面可见目录，共148项。逐项功能、真实目标URL、输入和登录要求尚未全部验证，不补造。未进入上表的项均为“官方目录确认；详情/运行待验证”，工具地址暂使用分类入口，不能用于自动提交数据。

| 分类 | 首页数量 | 收录工具名称 | 官方来源 |
| --- | --- | --- | --- |
| 可视化 | 31 | Bar、Box、Comutation、Dot、Forest、Heatmap、Histogram、Lollipop、Mafsummary、Maftitv、Manhattan、Oncoprint、PM-Adiversity、PM-comp-cor、PM-comp-func、PM-comp-taxa、PM-Distribution、PM-Hcluster、PM-Heatmap、PM-Marker-Corr、PM-Marker-RFscore、PM-Marker-Test、PM-Network、PM-Pca、PM-Pcoa、PM-plot-taxa、PM-rare-curv、Q-Q、Survival、Upset、Venn | [页面](https://ngdc.cncb.ac.cn/bit/) |
| 序列比对 | 12 | BLAST、Dotmatcher、Dotpath、Dottup、Needle、PM-Bdiversity、PM-extract-rna、PM-parallel-meta、PM-pipeline、PM-profiler、Polydot、Water | [页面](https://ngdc.cncb.ac.cn/bit/seqaln) |
| RNA表达 | 20 | Bulk RNA-seq Data Analysis、Cancer Alternative Splicing Analysis、CCLHunter、CIRI-deep、CIRI3、Cross-disease analysis、Disease prediction、Editing site annotation、Editing site identification、EFDetector、FunGen、Gene-disease network construction、HELIX、NCSelPred、RNA-seq Analysis、Single-cell RNA-seq Data Analysis、SPIRAL、TIVar diff、TIVar predict、Visualization of scRNA-seq Data Analysis Results | [页面](https://ngdc.cncb.ac.cn/bit/dotmatcher) |
| 表观组 | 12 | Age Predictor、BS-RNA、Comparative analysis in nucleosomes、DMR Annotation、DMR Toolkit、Enrichment & Annotation、Enrichment analysis in nucleosome occupancy、EWAS Network Visualization、GMQN、IDMP、Lollipop Plotter、MRAS | [页面](https://ngdc.cncb.ac.cn/bit/dotmatcher) |
| 变异组 | 15 | BarcodeBLAST、BarcodeFinder、easyGWAS、ExpPattern、GeneFinder、Haplotype analysis、HapMap、HapSnap、LeadSNPFinder、Pangolin LOCATOR、Rice Varieties Identification、Rice Yield Estimation、SeqFetch、VersionMap、Wheat Head Estimation | [页面](https://ngdc.cncb.ac.cn/bit/variome) |
| 长非编码RNA | 6 | Classification、Functional Prediction、ID conversion、LGC、lncbook-BLAST、LncBot | [页面](https://ngdc.cncb.ac.cn/bit/) |
| 病毒 | 16 | COVID-19 genome variation annotation、COVID-19 haplotype analysis、Denovo Assembly、Evolutionary tree、Fastq-to-Variants、Genealogy and Evolutionary Analysis、Genome Annotation、Genome Tracing、InfluProto、McAN、Monkeypox virus genome variation annotation、Monkeypox virus genome variation identification、Pangolin COVID-19 Lineage Assigner、SeqQC、VENAS、VISTA | [页面](https://ngdc.cncb.ac.cn/bit/virus) |
| 单细胞 | 11 | Cell Type Comparator、Cell Type Predictor、FGOT、GOT、MultiChat、RegChat、SCSES、SpaDC、spaDIVA、SpaMI、TACOS | [页面](https://ngdc.cncb.ac.cn/bit/dotmatcher) |
| 图像 | 5 | Image Cropping、Image Denoising、Image Flipping、Image Partitioning、Image Resizing | [页面](https://ngdc.cncb.ac.cn/bit/) |
| 其他 | 20 | Composition analysis、Cross-model analysis、Cross-species analysis、Cross-stages analysis、Homolog Finder、LUTLSRS、MEFE、MIA、Microbiome Search Engine、MT-tracker、ncRNA-eQTL、PM-parse-mip、PM-predict-func、PM-predict-func-contribute、PM-predict-func-nsti、PM-rand-rare、PM-select-func、PM-select-taxa、PM-split-seq、TaxaCal | [页面](https://ngdc.cncb.ac.cn/bit/others) |

## 六类重点需求覆盖

| 需求方向 | 发现证据 | 结论 |
| --- | --- | --- |
| DNA序列比对 | Needle输入类型包含Nucleotide；中心主页介绍BLAST | 已验证页面，计算待验证 |
| 蛋白质序列比对 | Needle明确Protein；BLAST官方介绍支持蛋白 | 已验证页面，计算待验证 |
| 在线基因组可视化 | GWH Genome Browser；IC4R旧浏览器链接 | GWH入口已验证，轨道交互待验证；IC4R旧链接失败 |
| 物种画像 | Taxonomy详情、分类树、名称和谱系 | 可以作为分类信息服务；完整画像定义、覆盖度和数据加载待核实 |
| 系统进化构建 | Taxonomy Clustal Omega/MAFFT等入口；RCoV19 Evolutionary tree | 已找到官方实际入口，尚未取得任务结果 |
| 物种鉴定 | Taxonomy Biological Barcode Identification说明DNA→BLAST→比对→构树 | 已验证功能与DNA输入说明，准确率及运行待验证 |

McAN在BIT中的链接实际指向BioCode工具介绍页，不能将该详情链接标成在线任务提交。GenBase序列校验链接指帮助页面，不能按独立计算工具上线。


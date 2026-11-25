USE bio_portal;
SET NAMES utf8mb4;

-- 给全局整合表保留各来源的CNCB资源关联；动态DDL可重复执行。
SET @add_cncb_banks_sql = IF(
    EXISTS (SELECT 1 FROM information_schema.columns
            WHERE table_schema=DATABASE() AND table_name='research_stat_publication' AND column_name='cncb_banks_json'),
    'SELECT 1',
    'ALTER TABLE research_stat_publication ADD COLUMN cncb_banks_json LONGTEXT NULL COMMENT ''整合CNCB关联数据库与资源编号JSON，用于研究主题分类'' AFTER mesh_headings_json'
);
PREPARE add_cncb_banks_stmt FROM @add_cncb_banks_sql;
EXECUTE add_cncb_banks_stmt;
DEALLOCATE PREPARE add_cncb_banks_stmt;

-- 本地目录前13条NGDC核心资源记录的标签；数据库代码同时兼容cncbBanks.db短名和历史别名。
INSERT INTO research_database_topic(database_code,topic_id,evidence,verification_status)
SELECT m.database_code,c.id,m.evidence,'CONFIRMED'
FROM (
    SELECT '3CDB' database_code,'OMICS_DATASET' topic_code,
           '3CDB收录染色质构象捕获(3C)相互作用数据；以组学数据集归类。https://ngdc.cncb.ac.cn/3cdb/' evidence
    UNION ALL SELECT 'GSA','SEQUENCE','GSA归档原始测序reads，项目/样本记录仅用于组织元数据。https://ngdc.cncb.ac.cn/education/tutorials/gsa/'
    UNION ALL SELECT 'Genome Sequence Archive','SEQUENCE','GSA归档原始测序reads，项目/样本记录仅用于组织元数据。https://ngdc.cncb.ac.cn/education/tutorials/gsa/'
    UNION ALL SELECT 'Database Commons','DATABASE','Database Commons是全球生物数据库目录。https://ngdc.cncb.ac.cn/databasecommons/help'
    UNION ALL SELECT 'dbcommons','DATABASE','Database Commons是全球生物数据库目录。https://ngdc.cncb.ac.cn/databasecommons/help'
    UNION ALL SELECT 'BioCode','TOOL','BioCode归档、浏览和下载生物信息学工具代码。https://ngdc.cncb.ac.cn/biocode/help'
    UNION ALL SELECT 'Biological Tool Codes','TOOL','BioCode归档、浏览和下载生物信息学工具代码。https://ngdc.cncb.ac.cn/biocode/help'
    UNION ALL SELECT 'BioProject','PROJECT','BioProject提供研究项目描述元数据。https://ngdc.cncb.ac.cn/bioproject/'
    UNION ALL SELECT 'Biological Project Library','PROJECT','BioProject提供研究项目描述元数据。https://ngdc.cncb.ac.cn/bioproject/'
    UNION ALL SELECT 'BioSample','SAMPLE','BioSample描述生物来源材料及其属性。https://ngdc.cncb.ac.cn/gsub/document/BioSample-BioSample_Submission_Guide_2.1.cn.pdf'
    UNION ALL SELECT 'Biological Sample Library','SAMPLE','BioSample描述生物来源材料及其属性。https://ngdc.cncb.ac.cn/gsub/document/BioSample-BioSample_Submission_Guide_2.1.cn.pdf'
    UNION ALL SELECT 'GSA-Human','SEQUENCE','GSA-Human归档人类遗传资源相关组学原始测序数据。https://ngdc.cncb.ac.cn/gsa-human/'
    UNION ALL SELECT 'HRA','SEQUENCE','HRA是GSA-Human的目录标识，归档人类遗传资源相关组学原始测序数据。https://ngdc.cncb.ac.cn/gsa-human/'
    UNION ALL SELECT 'OMIX','OMICS_DATASET','OMIX收录转录组、蛋白质组、表观组、脂质组、代谢组及其他组学/多模态数据。https://ngdc.cncb.ac.cn/omix/'
    UNION ALL SELECT 'Open Archive for Miscellaneous Data','OMICS_DATASET','OMIX收录转录组、蛋白质组、表观组、脂质组、代谢组及其他组学/多模态数据。https://ngdc.cncb.ac.cn/omix/'
    UNION ALL SELECT 'GenBase','SEQUENCE','GenBase存储带注释的核苷酸和蛋白质序列。https://ngdc.cncb.ac.cn/genbase/document'
    UNION ALL SELECT 'Genetic Sequence Database','SEQUENCE','GenBase存储带注释的核苷酸和蛋白质序列。https://ngdc.cncb.ac.cn/genbase/document'
    UNION ALL SELECT 'nucbank_nuc2','SEQUENCE','GenBase核苷酸序列业务标识别名。https://ngdc.cncb.ac.cn/genbase/document'
    UNION ALL SELECT 'nucbank_pro2','SEQUENCE','GenBase蛋白质序列业务标识别名。https://ngdc.cncb.ac.cn/genbase/document'
    UNION ALL SELECT 'SDAS','OTHER','SDAS提供汇交计划、进度监督和证明服务，官方说明不存储科学数据。https://ngdc.cncb.ac.cn/sdas/help'
    UNION ALL SELECT 'Scientific Data Archive System','OTHER','SDAS提供汇交计划、进度监督和证明服务，官方说明不存储科学数据。https://ngdc.cncb.ac.cn/sdas/help'
    UNION ALL SELECT 'HGRIP','OTHER','HGRIP是人类遗传资源管理与备份平台，整合其他数据资源系统；暂归其他平台类。https://ngdc.cncb.ac.cn/news/146'
    UNION ALL SELECT 'GVM','VARIATION','GVM核心内容为SNP和小片段插入缺失变异数据。https://ngdc.cncb.ac.cn/gvm/faq'
    UNION ALL SELECT 'Genome Variation Map','VARIATION','GVM核心内容为SNP和小片段插入缺失变异数据。https://ngdc.cncb.ac.cn/gvm/faq'
    UNION ALL SELECT 'GWAS Atlas','VARIATION','GWAS Atlas整理基因组变异与性状关联(G2P)数据。https://ngdc.cncb.ac.cn/gwas/documentation'
    UNION ALL SELECT 'Genome-Wide SNP-trait Associations Atlas','VARIATION','GWAS Atlas整理基因组变异与性状关联(G2P)数据。https://ngdc.cncb.ac.cn/gwas/documentation'
    UNION ALL SELECT 'GWAS','VARIATION','GWAS Atlas目录标识别名，内容为基因组变异与性状关联数据。https://ngdc.cncb.ac.cn/gwas/documentation'
) m
JOIN research_topic_category c ON c.topic_code=m.topic_code AND c.status='ENABLED'
ON DUPLICATE KEY UPDATE evidence=VALUES(evidence),verification_status='CONFIRMED',update_time=CURRENT_TIMESTAMP;

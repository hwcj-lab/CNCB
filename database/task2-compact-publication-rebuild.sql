-- MySQL 8.0.18 / bio_portal
-- 前提：旧表已重命名为 openlb_publication1、genbase_publication1。
-- 本脚本新建精简正式表并搬运数据；不会删除、清空或修改两个旧表，也不会访问外部接口。
USE bio_portal;
SET NAMES utf8mb4;

CREATE TABLE openlb_publication (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '本地自增主键',
  pmid VARCHAR(32) NOT NULL COMMENT 'OpenLB查询和数据库内去重唯一值',
  doi VARCHAR(255) NULL COMMENT '规范化DOI；PMID之外的辅助标识',
  title TEXT NULL COMMENT '文献标题',
  abstract_text LONGTEXT NULL COMMENT '论文摘要；用于详情和后续摘要主题分析',
  pub_year SMALLINT NULL COMMENT '发表年份；用于年度文献趋势',
  journal_title VARCHAR(500) NULL COMMENT '期刊名称',
  source_url VARCHAR(1500) NOT NULL COMMENT '文献官方来源地址',
  authors_json LONGTEXT NULL COMMENT '作者数组JSON，包含作者与署名机构对应关系',
  affiliation_info LONGTEXT NULL COMMENT '文章署名机构汇总原文',
  keyword_list LONGTEXT NULL COMMENT '关键词原文；用于研究主题',
  mesh_headings_json LONGTEXT NULL COMMENT 'PubMed MeSH主题数组JSON',
  grants_json LONGTEXT NULL COMMENT '基金原始数组JSON；当前统计口径待复核',
  cncb_banks_json LONGTEXT NOT NULL COMMENT 'CNCB资源关联数组JSON；关联文献下钻入口',
  raw_payload LONGTEXT NOT NULL COMMENT 'OpenLB完整原始JSON；删除展开字段后仍可离线重解析',
  is_primary TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否为OpenLB原始文献：1是，0否',
  is_related TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否曾由关联链路发现；可与is_primary同时为1',
  relation_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '关联状态：PENDING/RUNNING/COMPLETED/FAILED',
  relation_attempt_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '关联发现累计尝试次数',
  relation_last_error VARCHAR(1000) NULL COMMENT '关联发现最近错误',
  relation_checked_at DATETIME NULL COMMENT '最近一次完整关联检查时间',
  source_hash CHAR(64) NULL COMMENT '来源内容SHA-256；用于判断上游记录是否变化',
  fetched_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近一次采集时间',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uq_openlb_pmid (pmid),
  KEY idx_openlb_doi (doi),
  KEY idx_openlb_pub_year (pub_year),
  KEY idx_openlb_relation_schedule (relation_status,id),
  KEY idx_openlb_primary_related (is_primary,is_related)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='精简OpenLB来源文献；保留统计字段、关联字段和完整原始JSON';

INSERT INTO openlb_publication(
  id,pmid,doi,title,abstract_text,pub_year,journal_title,source_url,
  authors_json,affiliation_info,keyword_list,mesh_headings_json,grants_json,cncb_banks_json,raw_payload,
  is_primary,is_related,relation_status,relation_attempt_count,source_hash,fetched_at,create_time,update_time
)
SELECT
  id,pmid,NULLIF(TRIM(doi),''),title,abstract_text,pub_year,journal_title,source_url,
  authors_json,affiliation_info,keyword_list,mesh_headings_json,grants_json,cncb_banks_json,raw_payload,
  1,0,'PENDING',0,SHA2(raw_payload,256),fetched_at,create_time,update_time
FROM openlb_publication1
ORDER BY id;

CREATE TABLE genbase_publication (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '本地自增主键',
  paper_id BIGINT UNSIGNED NULL COMMENT 'GenBase支持文章接口记录ID；纯关联文献可为空',
  pmid VARCHAR(32) NULL COMMENT 'PubMed ID；数据库内主要去重值',
  doi VARCHAR(255) NULL COMMENT '规范化DOI',
  bioproject_accession VARCHAR(500) NULL COMMENT 'BioProject编号；用于项目关联和研究主题',
  journal_name VARCHAR(500) NULL COMMENT '期刊名称',
  paper_title TEXT NOT NULL COMMENT '文献标题',
  publish_year SMALLINT NULL COMMENT '发表年份；用于年度文献趋势',
  genbase_accessions TEXT NULL COMMENT 'GenBase序列登录号或范围（可能包含多个登录号或范围）；用于资源关联',
  source_url VARCHAR(1500) NOT NULL COMMENT 'GenBase或补充来源地址',
  raw_payload JSON NOT NULL COMMENT 'GenBase接口单条原始JSON',
  openlb_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'OpenLB补全状态：PENDING/RUNNING/SUCCESS/EMPTY/FAILED/NO_PMID',
  openlb_attempt_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'OpenLB补全累计请求次数',
  openlb_last_error VARCHAR(1000) NULL COMMENT 'OpenLB补全最近错误',
  openlb_last_attempt_time DATETIME NULL COMMENT '最近一次OpenLB补全请求时间',
  openlb_completed_time DATETIME NULL COMMENT 'OpenLB补全完成时间',
  abstract_text LONGTEXT NULL COMMENT '通过OpenLB补全的论文摘要',
  affiliation_info LONGTEXT NULL COMMENT '论文署名机构汇总原文',
  keyword_list LONGTEXT NULL COMMENT '关键词原文；用于研究主题',
  authors_json LONGTEXT NULL COMMENT '作者数组JSON，包含作者与署名机构对应关系',
  mesh_headings_json LONGTEXT NULL COMMENT 'PubMed MeSH主题数组JSON',
  grants_json LONGTEXT NULL COMMENT '基金原始数组JSON；当前统计口径待复核',
  cncb_banks_json LONGTEXT NULL COMMENT 'CNCB资源关联数组JSON；关联文献下钻入口',
  openlb_raw_payload LONGTEXT NULL COMMENT 'OpenLB补充详情完整原始JSON',
  is_primary TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否来自GenBase支持文章列表：1是，0否',
  is_related TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否曾由关联链路发现；可与is_primary同时为1',
  relation_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '关联状态：PENDING/RUNNING/COMPLETED/FAILED',
  relation_attempt_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '关联发现累计尝试次数',
  relation_last_error VARCHAR(1000) NULL COMMENT '关联发现最近错误',
  relation_checked_at DATETIME NULL COMMENT '最近一次完整关联检查时间',
  source_hash CHAR(64) NULL COMMENT 'GenBase来源内容SHA-256；用于变化检测',
  fetched_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近一次采集时间',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uq_genbase_paper_id (paper_id),
  UNIQUE KEY uq_genbase_pmid (pmid),
  UNIQUE KEY uq_genbase_doi (doi),
  KEY idx_genbase_bioproject (bioproject_accession),
  KEY idx_genbase_publish_year (publish_year),
  KEY idx_genbase_openlb_schedule (openlb_status,id),
  KEY idx_genbase_relation_schedule (relation_status,id),
  KEY idx_genbase_primary_related (is_primary,is_related)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='精简GenBase来源文献；保留统计、资源关联、OpenLB补全及原始JSON';

INSERT INTO genbase_publication(
  id,paper_id,pmid,doi,bioproject_accession,journal_name,paper_title,publish_year,genbase_accessions,source_url,raw_payload,
  openlb_status,openlb_attempt_count,openlb_last_error,openlb_last_attempt_time,openlb_completed_time,
  abstract_text,affiliation_info,keyword_list,authors_json,mesh_headings_json,grants_json,cncb_banks_json,openlb_raw_payload,
  is_primary,is_related,relation_status,relation_attempt_count,source_hash,fetched_at,create_time,update_time
)
SELECT
  id,paper_id,NULLIF(TRIM(pmid),''),NULLIF(TRIM(doi),''),bioproject_accession,journal_name,paper_title,publish_year,
  accession_range,source_url,raw_payload,openlb_status,openlb_attempt_count,openlb_last_error,
  openlb_last_attempt_time,openlb_completed_time,abstract_text,affiliation_info,keyword_list,authors_json,
  mesh_headings_json,grants_json,cncb_banks_json,openlb_raw_payload,
  1,0,'PENDING',0,SHA2(CAST(raw_payload AS CHAR CHARACTER SET utf8mb4),256),fetched_at,create_time,update_time
FROM genbase_publication1
ORDER BY id;

-- 双向关系、人工作业、持久化visited队列。
CREATE TABLE IF NOT EXISTS research_publication_relation (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '关联关系ID',
  database_code VARCHAR(40) NOT NULL COMMENT 'OPENLB/GENBASE；不同数据库允许相同关系',
  left_identifier_type VARCHAR(20) NOT NULL COMMENT '排序后左端唯一值类型', left_identifier VARCHAR(255) NOT NULL COMMENT '排序后左端唯一值',
  right_identifier_type VARCHAR(20) NOT NULL COMMENT '排序后右端唯一值类型', right_identifier VARCHAR(255) NOT NULL COMMENT '排序后右端唯一值',
  relation_type VARCHAR(40) NOT NULL DEFAULT 'RELATED_PUBLICATION' COMMENT '关系类型', evidence_url VARCHAR(1500) NULL COMMENT '官方证据页',
  discovery_depth INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '最短发现深度', active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '当前是否有效',
  last_seen_run_id BIGINT UNSIGNED NULL COMMENT '最近确认作业ID', first_seen_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次发现时间',
  last_seen_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近确认时间', create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uq_publication_relation(database_code,left_identifier_type,left_identifier,right_identifier_type,right_identifier,relation_type),
  KEY idx_publication_relation_left(database_code,left_identifier_type,left_identifier,active),
  KEY idx_publication_relation_right(database_code,right_identifier_type,right_identifier,active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据库范围内双向文献关联；A-B和B-A只保存一条';

CREATE TABLE IF NOT EXISTS research_relation_crawl_job (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '关联采集作业ID', database_code VARCHAR(40) NOT NULL COMMENT 'OPENLB/GENBASE',
  root_identifier_type VARCHAR(20) NOT NULL COMMENT '根唯一值类型', root_identifier VARCHAR(255) NOT NULL COMMENT '根唯一值',
  refresh_publication TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否刷新根文献', refresh_relations TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否刷新关联',
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/PAUSED/SUCCESS/FAILED/STOPPED',
  max_publications INT UNSIGNED NOT NULL DEFAULT 1000 COMMENT '单次运行安全预算', request_delay_ms INT UNSIGNED NOT NULL DEFAULT 500 COMMENT '请求最小间隔毫秒',
  processed_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '已处理节点数', discovered_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '新发现文献数',
  relation_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '确认关系数', last_error VARCHAR(2000) NULL COMMENT '最后错误或暂停原因',
  started_time DATETIME NULL COMMENT '开始时间', completed_time DATETIME NULL COMMENT '结束时间', create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间', KEY idx_relation_job_status(status,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人工触发的递归关联采集作业';

CREATE TABLE IF NOT EXISTS research_relation_crawl_queue (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '队列ID', job_id BIGINT UNSIGNED NOT NULL COMMENT '作业ID',
  identifier_type VARCHAR(20) NOT NULL COMMENT 'PMID/PAPER_ID/DOI', identifier_value VARCHAR(255) NOT NULL COMMENT '规范化唯一值',
  depth INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '发现深度', status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/SUCCESS/EMPTY/FAILED',
  attempt_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '尝试次数', last_error VARCHAR(1000) NULL COMMENT '最后错误',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间', update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uq_relation_queue_node(job_id,identifier_type,identifier_value), KEY idx_relation_queue_schedule(job_id,status,id),
  CONSTRAINT fk_relation_queue_job FOREIGN KEY(job_id) REFERENCES research_relation_crawl_job(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='递归关联持久化队列和visited集合';

CREATE TABLE IF NOT EXISTS research_topic_category (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '研究主题ID', topic_code VARCHAR(50) NOT NULL COMMENT '稳定编码', topic_name VARCHAR(100) NOT NULL COMMENT '中文名称',
  description VARCHAR(1000) NULL COMMENT '分类口径', sort_order INT NOT NULL DEFAULT 0 COMMENT '排序', status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间', update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uq_research_topic_code(topic_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='可动态维护的研究主题';

CREATE TABLE IF NOT EXISTS research_database_topic (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '关联ID', database_code VARCHAR(40) NOT NULL COMMENT '统计数据库编码', topic_id BIGINT UNSIGNED NOT NULL COMMENT '研究主题ID',
  evidence VARCHAR(1500) NULL COMMENT '官方依据', verification_status VARCHAR(30) NOT NULL DEFAULT 'PENDING' COMMENT 'CONFIRMED/PENDING/REJECTED',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间', update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uq_research_database_topic(database_code,topic_id), CONSTRAINT fk_research_database_topic FOREIGN KEY(topic_id) REFERENCES research_topic_category(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据库与研究主题多对多关系';

INSERT IGNORE INTO research_topic_category(topic_code,topic_name,description,sort_order,status) VALUES
('PROJECT','项目','以项目为主要资源组织单位',10,'ENABLED'),('SAMPLE','样本','以样本及元数据为主要资源',20,'ENABLED'),
('TOOL','工具','软件、算法或在线分析工具',30,'ENABLED'),('DATABASE','数据库','数据库目录、条目或知识资源',40,'ENABLED'),
('SEQUENCE','序列','核酸、蛋白质或其他序列数据',50,'ENABLED'),('VARIATION','变异','基因组变异数据',60,'ENABLED'),
('OMICS_DATASET','组学数据集','组学数据集及元数据',70,'ENABLED'),('OTHER','其他','尚未归类资源',999,'ENABLED');

-- MySQL 8.0.18没有可靠的ADD COLUMN IF NOT EXISTS，使用information_schema动态判断。
SET @has_include_related := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='research_stat_snapshot' AND COLUMN_NAME='include_related'
);
SET @snapshot_sql := IF(@has_include_related=0,
  'ALTER TABLE research_stat_snapshot ADD COLUMN include_related TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''是否将纯关联发现文献纳入快照统计'' AFTER source_count',
  'SELECT ''research_stat_snapshot.include_related already exists''');
PREPARE snapshot_stmt FROM @snapshot_sql;
EXECUTE snapshot_stmt;
DEALLOCATE PREPARE snapshot_stmt;

-- 迁移核对：两组数量应分别一致。旧表仍保留，确认无误后也不要立即删除，建议至少保留到新快照验证完成。
SELECT 'OPENLB' source_name,
       (SELECT COUNT(*) FROM openlb_publication1) old_count,
       (SELECT COUNT(*) FROM openlb_publication) new_count;
SELECT 'GENBASE' source_name,
       (SELECT COUNT(*) FROM genbase_publication1) old_count,
       (SELECT COUNT(*) FROM genbase_publication) new_count;

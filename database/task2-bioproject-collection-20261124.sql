-- MySQL 8；先执行再启动新版后端。不爬取、不回填、不删除历史数据。
USE bio_portal;
SET NAMES utf8mb4;
CREATE TABLE IF NOT EXISTS bioproject_publication LIKE openlb_publication;
-- 兼容已通过LIKE创建的BIOPROJECT表，合并为一次ALTER；不修改其他库。
SET @bioproject_columns='';
SET @bioproject_columns=CONCAT(@bioproject_columns,IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='bioproject_publication' AND column_name='bioproject_record_key'),'',', ADD COLUMN bioproject_record_key VARCHAR(400) NULL COMMENT ''BIOPROJECT原始文献稳定键：PMID/DOI/标题摘要；统计穿透使用'''));
SET @bioproject_columns=CONCAT(@bioproject_columns,IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='bioproject_publication' AND column_name='bioproject_accession'),'',', ADD COLUMN bioproject_accession TEXT NULL COMMENT ''原始文章关联的BioProject编号列表'''));
SET @bioproject_columns=CONCAT(@bioproject_columns,IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='bioproject_publication' AND column_name='bioproject_raw_payload'),'',', ADD COLUMN bioproject_raw_payload LONGTEXT NULL COMMENT ''BIOPROJECT支持文章列表单条完整原始JSON'''));
SET @bioproject_columns=CONCAT(@bioproject_columns,IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='bioproject_publication' AND column_name='bioproject_source_hash'),'',', ADD COLUMN bioproject_source_hash CHAR(64) NULL COMMENT ''列表原始内容SHA256；独立于PMID补全结果哈希'''));
SET @bioproject_columns=CONCAT(@bioproject_columns,IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='bioproject_publication' AND column_name='bioproject_enrichment_status'),'',', ADD COLUMN bioproject_enrichment_status VARCHAR(20) NOT NULL DEFAULT ''PENDING'' COMMENT ''PENDING/SUCCESS/EMPTY/NO_PMID/FAILED元数据补全状态'''));
SET @bioproject_columns=CONCAT(@bioproject_columns,IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='bioproject_publication' AND column_name='bioproject_enrichment_error'),'',', ADD COLUMN bioproject_enrichment_error VARCHAR(1800) NULL COMMENT ''最近一次补全失败原因'''));
SET @bioproject_columns=CONCAT(@bioproject_columns,IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='bioproject_publication' AND column_name='bioproject_enrichment_attempts'),'',', ADD COLUMN bioproject_enrichment_attempts INT NOT NULL DEFAULT 0 COMMENT ''已提交的补全批次累计尝试次数'''));
SET @bioproject_columns=CONCAT(@bioproject_columns,IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='bioproject_publication' AND column_name='bioproject_metadata_checked_at'),'',', ADD COLUMN bioproject_metadata_checked_at DATETIME NULL COMMENT ''最近一次元数据补全提交时间'''));
SET @bioproject_indexes=IF(EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='bioproject_publication' AND index_name='uq_bioproject_record_key'),'',', ADD UNIQUE KEY uq_bioproject_record_key(bioproject_record_key)');
SET @bioproject_indexes=CONCAT(@bioproject_indexes,IF(EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='bioproject_publication' AND index_name='idx_bioproject_enrichment'),'',', ADD KEY idx_bioproject_enrichment(is_primary,bioproject_enrichment_status,id)'));
SET @bioproject_ddl=CONCAT('ALTER TABLE bioproject_publication MODIFY COLUMN pmid VARCHAR(32) NULL COMMENT ''PubMed编号；原始条目无PMID时允许为空，不丢弃文献''',@bioproject_columns,@bioproject_indexes,', COMMENT=''BIOPROJECT项目出版文献及元数据；包含项目全部出版文献；提交单位不冒充作者机构''');
PREPARE bioproject_ddl_stmt FROM @bioproject_ddl;EXECUTE bioproject_ddl_stmt;DEALLOCATE PREPARE bioproject_ddl_stmt;
CREATE TABLE IF NOT EXISTS bioproject_collection_job (
 id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT 'BIOPROJECT独立扫描作业ID',
 status VARCHAR(20) NOT NULL COMMENT 'RUNNING/STOPPED/FAILED/SUCCESS',
 phase VARCHAR(20) NOT NULL COMMENT 'DOWNLOAD/IMPORT/ENRICH/COMPLETE',
 next_index BIGINT NOT NULL DEFAULT 0 COMMENT '同一固定JSON快照下已提交的项目数量；不使用线上不稳定偏移',
 rows_imported BIGINT NOT NULL DEFAULT 0 COMMENT '累计接受的原始列表条数，包含重复更新，不代表新增文献数',
 metadata_processed BIGINT NOT NULL DEFAULT 0 COMMENT '本作业已提交的补全条数',
 cache_file VARCHAR(1000) NOT NULL COMMENT '本作业磁盘固定JSON快照绝对路径',
 last_error VARCHAR(1800) NULL COMMENT '失败/停止原因',
 started_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次开始时间',
 completed_time DATETIME NULL COMMENT '本次结束时间',
 update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后进度更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BIOPROJECT固定快照扫描与元数据补全的持久化断点';
INSERT INTO openlb_bank_route(bank_db,source_code,source_name,enabled) VALUES('BioProject','BIOPROJECT','BioProject项目文献',1)
ON DUPLICATE KEY UPDATE source_code=VALUES(source_code),source_name=VALUES(source_name),enabled=1;
INSERT INTO research_database_topic(database_code,topic_id,evidence,verification_status)
SELECT 'BIOPROJECT',id,'BioProject以项目为资源组织单位；按项目全部出版信息归集。','CONFIRMED'
FROM research_topic_category WHERE topic_code='PROJECT'
ON DUPLICATE KEY UPDATE evidence=VALUES(evidence),verification_status=VALUES(verification_status);

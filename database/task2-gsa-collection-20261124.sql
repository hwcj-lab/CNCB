-- MySQL 8；先执行再启动新版后端。不爬取、不回填、不删除历史数据。
USE bio_portal;
SET NAMES utf8mb4;
CREATE TABLE IF NOT EXISTS gsa_publication LIKE openlb_publication;
-- 兼容已通过LIKE创建的GSA表，合并为一次ALTER；不修改其他库。
SET @gsa_columns='';
SET @gsa_columns=CONCAT(@gsa_columns,IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='gsa_publication' AND column_name='gsa_record_key'),'',', ADD COLUMN gsa_record_key VARCHAR(400) NULL COMMENT ''GSA原始文献稳定键：PMID/DOI/标题摘要；统计穿透使用'''));
SET @gsa_columns=CONCAT(@gsa_columns,IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='gsa_publication' AND column_name='gsa_accessions'),'',', ADD COLUMN gsa_accessions TEXT NULL COMMENT ''支持文章页面原始资源编号，含CRA/HRA/OMIX等；全部保留'''));
SET @gsa_columns=CONCAT(@gsa_columns,IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='gsa_publication' AND column_name='bioproject_accession'),'',', ADD COLUMN bioproject_accession TEXT NULL COMMENT ''原始文章关联的BioProject编号列表'''));
SET @gsa_columns=CONCAT(@gsa_columns,IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='gsa_publication' AND column_name='gsa_raw_payload'),'',', ADD COLUMN gsa_raw_payload LONGTEXT NULL COMMENT ''GSA支持文章列表单条完整原始JSON'''));
SET @gsa_columns=CONCAT(@gsa_columns,IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='gsa_publication' AND column_name='gsa_source_hash'),'',', ADD COLUMN gsa_source_hash CHAR(64) NULL COMMENT ''列表原始内容SHA256；独立于PMID补全结果哈希'''));
SET @gsa_columns=CONCAT(@gsa_columns,IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='gsa_publication' AND column_name='gsa_enrichment_status'),'',', ADD COLUMN gsa_enrichment_status VARCHAR(20) NOT NULL DEFAULT ''PENDING'' COMMENT ''PENDING/SUCCESS/EMPTY/NO_PMID/FAILED元数据补全状态'''));
SET @gsa_columns=CONCAT(@gsa_columns,IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='gsa_publication' AND column_name='gsa_enrichment_error'),'',', ADD COLUMN gsa_enrichment_error VARCHAR(1800) NULL COMMENT ''最近一次补全失败原因'''));
SET @gsa_columns=CONCAT(@gsa_columns,IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='gsa_publication' AND column_name='gsa_enrichment_attempts'),'',', ADD COLUMN gsa_enrichment_attempts INT NOT NULL DEFAULT 0 COMMENT ''已提交的补全批次累计尝试次数'''));
SET @gsa_columns=CONCAT(@gsa_columns,IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='gsa_publication' AND column_name='gsa_metadata_checked_at'),'',', ADD COLUMN gsa_metadata_checked_at DATETIME NULL COMMENT ''最近一次元数据补全提交时间'''));
SET @gsa_indexes=IF(EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='gsa_publication' AND index_name='uq_gsa_record_key'),'',', ADD UNIQUE KEY uq_gsa_record_key(gsa_record_key)');
SET @gsa_indexes=CONCAT(@gsa_indexes,IF(EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='gsa_publication' AND index_name='idx_gsa_enrichment'),'',', ADD KEY idx_gsa_enrichment(is_primary,gsa_enrichment_status,id)'));
SET @gsa_ddl=CONCAT('ALTER TABLE gsa_publication MODIFY COLUMN pmid VARCHAR(32) NULL COMMENT ''PubMed编号；原始条目无PMID时允许为空，不丢弃文献''',@gsa_columns,@gsa_indexes,', COMMENT=''GSA支持文章全列表文献及元数据；包含页面HRA/OMIX条目，用户指定统一归入GSA''');
PREPARE gsa_ddl_stmt FROM @gsa_ddl;EXECUTE gsa_ddl_stmt;DEALLOCATE PREPARE gsa_ddl_stmt;
CREATE TABLE IF NOT EXISTS gsa_collection_job (
 id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT 'GSA独立扫描作业ID',
 status VARCHAR(20) NOT NULL COMMENT 'RUNNING/STOPPED/FAILED/SUCCESS',
 phase VARCHAR(20) NOT NULL COMMENT 'DOWNLOAD/IMPORT/ENRICH/COMPLETE',
 next_index BIGINT NOT NULL DEFAULT 0 COMMENT '同一固定JSON快照下已提交的文章数量；不使用线上不稳定偏移',
 rows_imported BIGINT NOT NULL DEFAULT 0 COMMENT '累计接受的原始列表条数，包含重复更新，不代表新增文献数',
 metadata_processed BIGINT NOT NULL DEFAULT 0 COMMENT '本作业已提交的补全条数',
 cache_file VARCHAR(1000) NOT NULL COMMENT '本作业磁盘固定JSON快照绝对路径',
 last_error VARCHAR(1800) NULL COMMENT '失败/停止原因',
 started_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次开始时间',
 completed_time DATETIME NULL COMMENT '本次结束时间',
 update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后进度更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='GSA固定快照扫描与元数据补全的持久化断点';
INSERT INTO openlb_bank_route(bank_db,source_code,source_name,enabled) VALUES('GSA','GSA','GSA支持文章',1)
ON DUPLICATE KEY UPDATE source_code=VALUES(source_code),source_name=VALUES(source_name),enabled=1;
INSERT INTO research_database_topic(database_code,topic_id,evidence,verification_status)
SELECT 'GSA',id,'GSA原始测序数据资源；本来源按支持文章页面全部条目收集。','CONFIRMED'
FROM research_topic_category WHERE topic_code='SEQUENCE'
ON DUPLICATE KEY UPDATE evidence=VALUES(evidence),verification_status=VALUES(verification_status);

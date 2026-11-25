-- MySQL 8: additive-only schema repair; no backfill and no PMID column rebuild.
USE bio_portal;
SET NAMES utf8mb4;
SET SESSION lock_wait_timeout=5;
SET @ddl=IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='bioproject_publication' AND column_name='bioproject_record_key'),'DO 0','ALTER TABLE bioproject_publication ADD COLUMN bioproject_record_key VARCHAR(400) NULL COMMENT ''BIOPROJECT原始文献稳定键：PMID/DOI/标题摘要；统计穿透使用'', ALGORITHM=INSTANT');
PREPARE schema_patch FROM @ddl; EXECUTE schema_patch; DEALLOCATE PREPARE schema_patch;
SET @ddl=IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='bioproject_publication' AND column_name='bioproject_accession'),'DO 0','ALTER TABLE bioproject_publication ADD COLUMN bioproject_accession TEXT NULL COMMENT ''原始文章关联的BioProject编号列表'', ALGORITHM=INSTANT');
PREPARE schema_patch FROM @ddl; EXECUTE schema_patch; DEALLOCATE PREPARE schema_patch;
SET @ddl=IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='bioproject_publication' AND column_name='bioproject_raw_payload'),'DO 0','ALTER TABLE bioproject_publication ADD COLUMN bioproject_raw_payload LONGTEXT NULL COMMENT ''BIOPROJECT支持文章列表单条完整原始JSON'', ALGORITHM=INSTANT');
PREPARE schema_patch FROM @ddl; EXECUTE schema_patch; DEALLOCATE PREPARE schema_patch;
SET @ddl=IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='bioproject_publication' AND column_name='bioproject_source_hash'),'DO 0','ALTER TABLE bioproject_publication ADD COLUMN bioproject_source_hash CHAR(64) NULL COMMENT ''列表原始内容SHA256；独立于PMID补全结果哈希'', ALGORITHM=INSTANT');
PREPARE schema_patch FROM @ddl; EXECUTE schema_patch; DEALLOCATE PREPARE schema_patch;
SET @ddl=IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='bioproject_publication' AND column_name='bioproject_enrichment_status'),'DO 0','ALTER TABLE bioproject_publication ADD COLUMN bioproject_enrichment_status VARCHAR(20) NOT NULL DEFAULT ''PENDING'' COMMENT ''PENDING/SUCCESS/EMPTY/NO_PMID/FAILED元数据补全状态'', ALGORITHM=INSTANT');
PREPARE schema_patch FROM @ddl; EXECUTE schema_patch; DEALLOCATE PREPARE schema_patch;
SET @ddl=IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='bioproject_publication' AND column_name='bioproject_enrichment_error'),'DO 0','ALTER TABLE bioproject_publication ADD COLUMN bioproject_enrichment_error VARCHAR(1800) NULL COMMENT ''最近一次补全失败原因'', ALGORITHM=INSTANT');
PREPARE schema_patch FROM @ddl; EXECUTE schema_patch; DEALLOCATE PREPARE schema_patch;
SET @ddl=IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='bioproject_publication' AND column_name='bioproject_enrichment_attempts'),'DO 0','ALTER TABLE bioproject_publication ADD COLUMN bioproject_enrichment_attempts INT NOT NULL DEFAULT 0 COMMENT ''已提交的补全批次累计尝试次数'', ALGORITHM=INSTANT');
PREPARE schema_patch FROM @ddl; EXECUTE schema_patch; DEALLOCATE PREPARE schema_patch;
SET @ddl=IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='bioproject_publication' AND column_name='bioproject_metadata_checked_at'),'DO 0','ALTER TABLE bioproject_publication ADD COLUMN bioproject_metadata_checked_at DATETIME NULL COMMENT ''最近一次元数据补全提交时间'', ALGORITHM=INSTANT');
PREPARE schema_patch FROM @ddl; EXECUTE schema_patch; DEALLOCATE PREPARE schema_patch;
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
SET @ddl=IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='gsa_publication' AND column_name='gsa_record_key'),'DO 0','ALTER TABLE gsa_publication ADD COLUMN gsa_record_key VARCHAR(400) NULL COMMENT ''GSA原始文献稳定键：PMID/DOI/标题摘要；统计穿透使用'', ALGORITHM=INSTANT');
PREPARE schema_patch FROM @ddl; EXECUTE schema_patch; DEALLOCATE PREPARE schema_patch;
SET @ddl=IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='gsa_publication' AND column_name='gsa_accessions'),'DO 0','ALTER TABLE gsa_publication ADD COLUMN gsa_accessions TEXT NULL COMMENT ''支持文章页面原始资源编号，含CRA/HRA/OMIX等；全部保留'', ALGORITHM=INSTANT');
PREPARE schema_patch FROM @ddl; EXECUTE schema_patch; DEALLOCATE PREPARE schema_patch;
SET @ddl=IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='gsa_publication' AND column_name='bioproject_accession'),'DO 0','ALTER TABLE gsa_publication ADD COLUMN bioproject_accession TEXT NULL COMMENT ''原始文章关联的BioProject编号列表'', ALGORITHM=INSTANT');
PREPARE schema_patch FROM @ddl; EXECUTE schema_patch; DEALLOCATE PREPARE schema_patch;
SET @ddl=IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='gsa_publication' AND column_name='gsa_raw_payload'),'DO 0','ALTER TABLE gsa_publication ADD COLUMN gsa_raw_payload LONGTEXT NULL COMMENT ''GSA支持文章列表单条完整原始JSON'', ALGORITHM=INSTANT');
PREPARE schema_patch FROM @ddl; EXECUTE schema_patch; DEALLOCATE PREPARE schema_patch;
SET @ddl=IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='gsa_publication' AND column_name='gsa_source_hash'),'DO 0','ALTER TABLE gsa_publication ADD COLUMN gsa_source_hash CHAR(64) NULL COMMENT ''列表原始内容SHA256；独立于PMID补全结果哈希'', ALGORITHM=INSTANT');
PREPARE schema_patch FROM @ddl; EXECUTE schema_patch; DEALLOCATE PREPARE schema_patch;
SET @ddl=IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='gsa_publication' AND column_name='gsa_enrichment_status'),'DO 0','ALTER TABLE gsa_publication ADD COLUMN gsa_enrichment_status VARCHAR(20) NOT NULL DEFAULT ''PENDING'' COMMENT ''PENDING/SUCCESS/EMPTY/NO_PMID/FAILED元数据补全状态'', ALGORITHM=INSTANT');
PREPARE schema_patch FROM @ddl; EXECUTE schema_patch; DEALLOCATE PREPARE schema_patch;
SET @ddl=IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='gsa_publication' AND column_name='gsa_enrichment_error'),'DO 0','ALTER TABLE gsa_publication ADD COLUMN gsa_enrichment_error VARCHAR(1800) NULL COMMENT ''最近一次补全失败原因'', ALGORITHM=INSTANT');
PREPARE schema_patch FROM @ddl; EXECUTE schema_patch; DEALLOCATE PREPARE schema_patch;
SET @ddl=IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='gsa_publication' AND column_name='gsa_enrichment_attempts'),'DO 0','ALTER TABLE gsa_publication ADD COLUMN gsa_enrichment_attempts INT NOT NULL DEFAULT 0 COMMENT ''已提交的补全批次累计尝试次数'', ALGORITHM=INSTANT');
PREPARE schema_patch FROM @ddl; EXECUTE schema_patch; DEALLOCATE PREPARE schema_patch;
SET @ddl=IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='gsa_publication' AND column_name='gsa_metadata_checked_at'),'DO 0','ALTER TABLE gsa_publication ADD COLUMN gsa_metadata_checked_at DATETIME NULL COMMENT ''最近一次元数据补全提交时间'', ALGORITHM=INSTANT');
PREPARE schema_patch FROM @ddl; EXECUTE schema_patch; DEALLOCATE PREPARE schema_patch;
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

-- Build only missing indexes online.
USE bio_portal;
SET SESSION lock_wait_timeout=5;
SET @ddl=IF(EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='bioproject_publication' AND index_name='uq_bioproject_record_key'),'DO 0','ALTER TABLE bioproject_publication ADD UNIQUE KEY uq_bioproject_record_key(bioproject_record_key), ALGORITHM=INPLACE, LOCK=NONE');
PREPARE schema_patch FROM @ddl; EXECUTE schema_patch; DEALLOCATE PREPARE schema_patch;
SET @ddl=IF(EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='bioproject_publication' AND index_name='idx_bioproject_enrichment'),'DO 0','ALTER TABLE bioproject_publication ADD KEY idx_bioproject_enrichment(is_primary,bioproject_enrichment_status,id), ALGORITHM=INPLACE, LOCK=NONE');
PREPARE schema_patch FROM @ddl; EXECUTE schema_patch; DEALLOCATE PREPARE schema_patch;
SET @ddl=IF(EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='gsa_publication' AND index_name='uq_gsa_record_key'),'DO 0','ALTER TABLE gsa_publication ADD UNIQUE KEY uq_gsa_record_key(gsa_record_key), ALGORITHM=INPLACE, LOCK=NONE');
PREPARE schema_patch FROM @ddl; EXECUTE schema_patch; DEALLOCATE PREPARE schema_patch;
SET @ddl=IF(EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='gsa_publication' AND index_name='idx_gsa_enrichment'),'DO 0','ALTER TABLE gsa_publication ADD KEY idx_gsa_enrichment(is_primary,gsa_enrichment_status,id), ALGORITHM=INPLACE, LOCK=NONE');
PREPARE schema_patch FROM @ddl; EXECUTE schema_patch; DEALLOCATE PREPARE schema_patch;

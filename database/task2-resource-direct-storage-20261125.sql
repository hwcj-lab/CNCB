-- MySQL 8. Direct resource storage; no crawling or historical backfill.
USE bio_portal;
SET NAMES utf8mb4;
SET SESSION lock_wait_timeout=5;
SET @ddl=IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='research_stat_publication' AND column_name='raw_payload'),'DO 0','ALTER TABLE research_stat_publication ADD COLUMN raw_payload LONGTEXT NULL COMMENT ''无文献资源原始JSON；文献可为空'', ALGORITHM=INSTANT');
PREPARE resource_patch FROM @ddl; EXECUTE resource_patch; DEALLOCATE PREPARE resource_patch;
SET @ddl=IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='research_stat_publication' AND column_name='relation_status'),'DO 0','ALTER TABLE research_stat_publication ADD COLUMN relation_status VARCHAR(20) NOT NULL DEFAULT ''PENDING'' COMMENT ''无文献资源关联发现状态；文献行不使用'', ALGORITHM=INSTANT');
PREPARE resource_patch FROM @ddl; EXECUTE resource_patch; DEALLOCATE PREPARE resource_patch;
SET @ddl=IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='research_stat_publication' AND column_name='relation_last_error'),'DO 0','ALTER TABLE research_stat_publication ADD COLUMN relation_last_error VARCHAR(1800) NULL COMMENT ''无文献资源最近关联错误'', ALGORITHM=INSTANT');
PREPARE resource_patch FROM @ddl; EXECUTE resource_patch; DEALLOCATE PREPARE resource_patch;
SET @ddl=IF(EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='research_stat_publication' AND index_name='idx_integrated_resource_scan'),'DO 0','ALTER TABLE research_stat_publication ADD KEY idx_integrated_resource_scan(record_kind,source_codes,is_active,id), ALGORITHM=INPLACE, LOCK=NONE');
PREPARE resource_patch FROM @ddl; EXECUTE resource_patch; DEALLOCATE PREPARE resource_patch;

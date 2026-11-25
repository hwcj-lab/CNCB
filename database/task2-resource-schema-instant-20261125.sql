-- MySQL 8；在启动新版后端前执行。仅建表/加字段，不回填、不运行采集。
USE bio_portal;
SET NAMES utf8mb4;
SET SESSION lock_wait_timeout=5;
-- Resource rows now live directly in research_stat_publication.
SET @resource_ddl=IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='research_stat_snapshot' AND column_name='include_resources'),'SELECT 1','ALTER TABLE research_stat_snapshot ADD COLUMN include_resources TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''是否纳入无原生文献资源'', ALGORITHM=INSTANT');
PREPARE resource_stmt FROM @resource_ddl;EXECUTE resource_stmt;DEALLOCATE PREPARE resource_stmt;
SET @resource_ddl=IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='research_stat_snapshot' AND column_name='resource_count'),'SELECT 1','ALTER TABLE research_stat_snapshot ADD COLUMN resource_count BIGINT NOT NULL DEFAULT 0 COMMENT ''本快照无文献资源数量'', ALGORITHM=INSTANT');
PREPARE resource_stmt FROM @resource_ddl;EXECUTE resource_stmt;DEALLOCATE PREPARE resource_stmt;
SET @resource_ddl=IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='research_stat_publication' AND column_name='record_kind'),'SELECT 1','ALTER TABLE research_stat_publication ADD COLUMN record_kind VARCHAR(20) NOT NULL DEFAULT ''PUBLICATION'' COMMENT ''PUBLICATION文献或RESOURCE无文献资源'', ALGORITHM=INSTANT');
PREPARE resource_stmt FROM @resource_ddl;EXECUTE resource_stmt;DEALLOCATE PREPARE resource_stmt;
SET @resource_ddl=IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='research_stat_publication' AND column_name='resource_accession'),'SELECT 1','ALTER TABLE research_stat_publication ADD COLUMN resource_accession VARCHAR(320) NULL COMMENT ''RESOURCE原库编号'', ALGORITHM=INSTANT');
PREPARE resource_stmt FROM @resource_ddl;EXECUTE resource_stmt;DEALLOCATE PREPARE resource_stmt;
SET @resource_ddl=IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='research_stat_publication' AND column_name='resource_url'),'SELECT 1','ALTER TABLE research_stat_publication ADD COLUMN resource_url VARCHAR(1500) NULL COMMENT ''RESOURCE原库详情页'', ALGORITHM=INSTANT');
PREPARE resource_stmt FROM @resource_ddl;EXECUTE resource_stmt;DEALLOCATE PREPARE resource_stmt;

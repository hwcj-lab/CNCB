-- Run only after deploying direct-storage code. Never drops a nonempty table.
USE bio_portal;
SET SESSION lock_wait_timeout=5;
SET @resource_rows=0;
SET @ddl=IF(EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='research_resource_record'),'SELECT COUNT(*) INTO @resource_rows FROM research_resource_record','DO 0');
PREPARE resource_patch FROM @ddl; EXECUTE resource_patch; DEALLOCATE PREPARE resource_patch;
SET @ddl=IF(@resource_rows=0,'DROP TABLE IF EXISTS research_resource_record','SELECT ''Resource table is not empty; migrate and verify its rows before dropping'' AS warning');
PREPARE resource_patch FROM @ddl; EXECUTE resource_patch; DEALLOCATE PREPARE resource_patch;

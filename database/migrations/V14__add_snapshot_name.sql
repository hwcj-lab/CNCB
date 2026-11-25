USE bio_portal;
SET NAMES utf8mb4;

ALTER TABLE research_stat_snapshot
    ADD COLUMN snapshot_name VARCHAR(100) NULL COMMENT '用户可修改的快照名称' AFTER version_no,
    ALGORITHM=INPLACE;

UPDATE research_stat_snapshot
SET snapshot_name=CONCAT('统计快照 v',version_no)
WHERE snapshot_name IS NULL OR snapshot_name='';


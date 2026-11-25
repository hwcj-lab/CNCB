-- 为作者维度保存结构化作者名与署名机构，并从作者JSON回填文章级机构摘要。
ALTER TABLE research_stat_dimension
    ADD COLUMN author_name VARCHAR(500) NULL COMMENT 'AUTHOR维度的作者姓名' AFTER display_name,
    ADD COLUMN institution_name VARCHAR(1000) NULL COMMENT 'AUTHOR维度中该作者自己的署名机构；未知为NULL' AFTER author_name,
    ALGORITHM=INPLACE;

ALTER TABLE research_stat_publication
    ADD COLUMN authors_json LONGTEXT NULL COMMENT '快照作者数组JSON' AFTER source_record_keys,
    ADD COLUMN affiliation_info LONGTEXT NULL COMMENT '从作者数组汇总的文章署名机构' AFTER authors_json,
    ADD COLUMN grants_json LONGTEXT NULL COMMENT '快照基金数组JSON' AFTER affiliation_info,
    ALGORITHM=INPLACE;

UPDATE research_stat_dimension
SET author_name = TRIM(SUBSTRING_INDEX(display_name, ' · ', 1)),
    institution_name = CASE
        WHEN LOCATE(' · ', display_name) = 0 THEN NULL
        ELSE NULLIF(TRIM(SUBSTRING_INDEX(display_name, ' · ', -1)), '机构未提供')
    END
WHERE dimension_type = 'AUTHOR';

-- 大表历史回填不放入结构迁移，避免部署时长事务锁表；新采集与新快照由Java直接提取。
-- 如需补历史记录，使用 database/manual/backfill_author_institution.sql 分批执行。

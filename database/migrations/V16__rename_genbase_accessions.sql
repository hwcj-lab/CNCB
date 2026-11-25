USE bio_portal;

-- 将容易被误解为单个范围的旧字段名，统一改为可表示多个 GenBase 登录号或范围的名称。
ALTER TABLE `genbase_publication`
    CHANGE COLUMN `accession_range` `genbase_accessions` TEXT NULL
    COMMENT 'GenBase序列登录号或范围（可能包含多个登录号或范围）';

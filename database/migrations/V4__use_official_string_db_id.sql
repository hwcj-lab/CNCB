USE bio_portal;

-- 前置条件：已执行 V3，database_resource 同时存在数值 db_id 与 search_ids。
-- 数值目录id改名为 catalog_id；官方字符串 dbId 成为业务字段 db_id。
ALTER TABLE database_resource
    CHANGE COLUMN db_id catalog_id BIGINT NOT NULL COMMENT 'CNCB目录接口外层数值id',
    CHANGE COLUMN search_ids db_id VARCHAR(500) NULL COMMENT 'CNCB目录dbId及跨库搜索标识，多个值以分号分隔';

CREATE UNIQUE INDEX uq_database_db_id ON database_resource(db_id);

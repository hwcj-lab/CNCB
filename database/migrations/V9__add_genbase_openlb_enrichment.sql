USE bio_portal;
SET NAMES utf8mb4;

ALTER TABLE genbase_publication
    ADD COLUMN openlb_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/SUCCESS/EMPTY/FAILED/NO_PMID' AFTER raw_payload,
    ADD COLUMN openlb_attempt_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'OpenLB补全请求次数' AFTER openlb_status,
    ADD COLUMN openlb_last_error VARCHAR(1000) NULL COMMENT 'OpenLB补全最后错误' AFTER openlb_attempt_count,
    ADD COLUMN openlb_last_attempt_time DATETIME NULL COMMENT '最近一次OpenLB请求时间' AFTER openlb_last_error,
    ADD COLUMN openlb_completed_time DATETIME NULL COMMENT 'OpenLB补全完成时间' AFTER openlb_last_attempt_time,
    ADD COLUMN openlb_id VARCHAR(64) NULL COMMENT 'OpenLB内部记录ID' AFTER openlb_completed_time,
    ADD COLUMN abstract_text LONGTEXT NULL COMMENT '论文摘要' AFTER openlb_id,
    ADD COLUMN author_string LONGTEXT NULL COMMENT '作者原始文本' AFTER abstract_text,
    ADD COLUMN affiliation_info LONGTEXT NULL COMMENT '论文署名机构原始文本' AFTER author_string,
    ADD COLUMN language VARCHAR(80) NULL COMMENT '文献语言' AFTER affiliation_info,
    ADD COLUMN publication_type_list TEXT NULL COMMENT '文献类型列表' AFTER language,
    ADD COLUMN country VARCHAR(120) NULL COMMENT '文献国家或地区' AFTER publication_type_list,
    ADD COLUMN keyword_list LONGTEXT NULL COMMENT '关键词原始文本' AFTER country,
    ADD COLUMN pmcid VARCHAR(64) NULL COMMENT 'PubMed Central ID' AFTER keyword_list,
    ADD COLUMN authors_json LONGTEXT NULL COMMENT 'OpenLB作者数组JSON' AFTER pmcid,
    ADD COLUMN mesh_headings_json LONGTEXT NULL COMMENT 'MeSH主题数组JSON' AFTER authors_json,
    ADD COLUMN grants_json LONGTEXT NULL COMMENT '基金数组JSON' AFTER mesh_headings_json,
    ADD COLUMN cncb_banks_json LONGTEXT NULL COMMENT 'CNCB数据库关联数组JSON；允许null或空数组' AFTER grants_json,
    ADD COLUMN openlb_raw_payload LONGTEXT NULL COMMENT 'OpenLB单条记录原始JSON' AFTER cncb_banks_json,
    ADD KEY idx_genbase_openlb_schedule(openlb_status,id);

UPDATE genbase_publication SET openlb_status='NO_PMID' WHERE pmid IS NULL OR pmid='';


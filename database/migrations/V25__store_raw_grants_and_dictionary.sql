-- MySQL 8.0 / Task 2 raw-grant storage migration
-- Apply once to bio_portal after backing up the three affected tables.
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS research_fund_dictionary (
  grant_id VARCHAR(255) NOT NULL COMMENT '资助项目唯一编号',
  full_name VARCHAR(1000) NULL COMMENT '基金/资助项目英文全称',
  acronym VARCHAR(255) NULL COMMENT '官方缩写',
  agency VARCHAR(1000) NULL COMMENT '资助机构名称',
  source_name VARCHAR(100) NULL COMMENT '字典数据来源',
  status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (grant_id),
  KEY idx_research_fund_dictionary_status (status)
) COMMENT='基金编号及其规范英文名称字典';

ALTER TABLE openlb_publication
  ADD COLUMN grants_json LONGTEXT NULL COMMENT '原始基金JSON；优先grants，缺失时funding转为grantId对象' AFTER mesh_headings_json;
ALTER TABLE genbase_publication
  ADD COLUMN grants_json LONGTEXT NULL COMMENT '原始基金JSON；优先grants，缺失时funding转为grantId对象' AFTER mesh_headings_json;
ALTER TABLE research_stat_publication
  ADD COLUMN grants_json LONGTEXT NULL COMMENT '合并来源的原始基金JSON数组；快照阶段解析' AFTER affiliation_info;

-- OpenLB raw_payload stores the complete API response. Preserve the upstream grants object;
-- funding IDs are normalized to the same JSON shape when grants is absent.
UPDATE openlb_publication
SET grants_json = CASE
  WHEN JSON_TYPE(JSON_EXTRACT(raw_payload, '$.result.data[0].grants')) = 'ARRAY'
       AND JSON_LENGTH(JSON_EXTRACT(raw_payload, '$.result.data[0].grants')) > 0
    THEN JSON_EXTRACT(raw_payload, '$.result.data[0].grants')
  WHEN JSON_TYPE(JSON_EXTRACT(raw_payload, '$.result.data[0].funding')) = 'ARRAY'
       AND JSON_LENGTH(JSON_EXTRACT(raw_payload, '$.result.data[0].funding')) > 0
    THEN (SELECT COALESCE(JSON_ARRAYAGG(JSON_OBJECT('grantId', funding_id)), JSON_ARRAY())
          FROM JSON_TABLE(JSON_EXTRACT(openlb_publication.raw_payload, '$.result.data[0].funding'), '$[*]'
               COLUMNS(funding_id VARCHAR(255) PATH '$')) f)
  WHEN COALESCE(JSON_LENGTH(CAST(fund_level1_json AS JSON)),0) > 0 OR COALESCE(JSON_LENGTH(CAST(fund_level2_json AS JSON)),0) > 0
    THEN JSON_ARRAY(JSON_OBJECT('legacyFundLevel1', COALESCE(CAST(fund_level1_json AS JSON), JSON_ARRAY()), 'legacyFundLevel2', COALESCE(CAST(fund_level2_json AS JSON), JSON_ARRAY())))
  ELSE JSON_ARRAY()
END;

-- GenBase retains both its API row and the OpenLB PMID response. Prefer the PMID grants;
-- otherwise preserve GenBase grants/funding and finally retain a valid empty array.
UPDATE genbase_publication
SET grants_json = CASE
  WHEN JSON_TYPE(JSON_EXTRACT(openlb_raw_payload, '$.grants')) = 'ARRAY'
       AND JSON_LENGTH(JSON_EXTRACT(openlb_raw_payload, '$.grants')) > 0
    THEN JSON_EXTRACT(openlb_raw_payload, '$.grants')
  WHEN JSON_TYPE(JSON_EXTRACT(raw_payload, '$.grants')) = 'ARRAY'
       AND JSON_LENGTH(JSON_EXTRACT(raw_payload, '$.grants')) > 0
    THEN JSON_EXTRACT(raw_payload, '$.grants')
  WHEN JSON_TYPE(JSON_EXTRACT(raw_payload, '$.funding')) = 'ARRAY'
       AND JSON_LENGTH(JSON_EXTRACT(raw_payload, '$.funding')) > 0
    THEN (SELECT COALESCE(JSON_ARRAYAGG(JSON_OBJECT('grantId', funding_id)), JSON_ARRAY())
          FROM JSON_TABLE(JSON_EXTRACT(genbase_publication.raw_payload, '$.funding'), '$[*]'
               COLUMNS(funding_id VARCHAR(255) PATH '$')) f)
  WHEN COALESCE(JSON_LENGTH(CAST(fund_level1_json AS JSON)),0) > 0 OR COALESCE(JSON_LENGTH(CAST(fund_level2_json AS JSON)),0) > 0
    THEN JSON_ARRAY(JSON_OBJECT('legacyFundLevel1', COALESCE(CAST(fund_level1_json AS JSON), JSON_ARRAY()), 'legacyFundLevel2', COALESCE(CAST(fund_level2_json AS JSON), JSON_ARRAY())))
  ELSE JSON_ARRAY()
END;

-- Existing global records are hydrated from their source rows. A later full integration rebuild
-- will merge cross-source raw grants and refresh content hashes in the normal supported workflow.
UPDATE research_stat_publication r
LEFT JOIN openlb_publication o ON o.pmid = r.pmid
LEFT JOIN genbase_publication g ON g.pmid = r.pmid
SET r.grants_json = JSON_MERGE_PRESERVE(
      COALESCE(CAST(o.grants_json AS JSON), JSON_ARRAY()),
      COALESCE(CAST(g.grants_json AS JSON), JSON_ARRAY()))
WHERE o.id IS NOT NULL OR g.id IS NOT NULL;
UPDATE research_stat_publication SET grants_json = JSON_ARRAY() WHERE grants_json IS NULL;

ALTER TABLE openlb_publication
  MODIFY grants_json LONGTEXT NOT NULL COMMENT '原始基金JSON；优先grants，缺失时funding转为grantId对象',
  DROP COLUMN fund_level1_json,
  DROP COLUMN fund_level2_json;
ALTER TABLE genbase_publication
  MODIFY grants_json LONGTEXT NOT NULL COMMENT '原始基金JSON；优先grants，缺失时funding转为grantId对象',
  DROP COLUMN fund_level1_json,
  DROP COLUMN fund_level2_json;
ALTER TABLE research_stat_publication
  MODIFY grants_json LONGTEXT NOT NULL COMMENT '合并来源的原始基金JSON数组；快照阶段解析',
  DROP COLUMN fund_level1_json,
  DROP COLUMN fund_level2_json;



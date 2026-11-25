-- MySQL 8.0.18 / bio_portal
SET NAMES utf8mb4;
-- Adds two-level statistical fields while retaining grants_json as the source funding payload.

ALTER TABLE openlb_publication
  ADD COLUMN fund_level1_json LONGTEXT NULL COMMENT '一级基金机构/资助方（OpenLB agency）去重JSON数组' AFTER mesh_headings_json,
  ADD COLUMN fund_level2_json LONGTEXT NULL COMMENT '二级基金缩写（OpenLB acronym）或项目名称去重JSON数组' AFTER fund_level1_json;

ALTER TABLE genbase_publication
  ADD COLUMN fund_level1_json LONGTEXT NULL COMMENT '一级基金机构/资助方（OpenLB agency）去重JSON数组' AFTER mesh_headings_json,
  ADD COLUMN fund_level2_json LONGTEXT NULL COMMENT '二级基金缩写（OpenLB acronym）或项目名称去重JSON数组' AFTER fund_level1_json;

ALTER TABLE research_stat_publication
  ADD COLUMN fund_level1_json LONGTEXT NULL COMMENT '整合文献一级基金机构/资助方去重JSON数组' AFTER affiliation_info,
  ADD COLUMN fund_level2_json LONGTEXT NULL COMMENT '整合文献二级基金缩写/项目名称去重JSON数组' AFTER fund_level1_json;

-- OpenLB中agency是一级资助方，acronym（如GM）是二级标记；无acronym时才回退到项目/计划名称。
UPDATE openlb_publication p
SET fund_level1_json = COALESCE((
  SELECT JSON_ARRAYAGG(x.value) FROM (
    SELECT DISTINCT TRIM(j.value) value FROM JSON_TABLE(p.grants_json, '$[*]'
      COLUMNS(value VARCHAR(1000) PATH '$.agency' NULL ON EMPTY)) j
    WHERE j.value IS NOT NULL AND TRIM(j.value) <> ''
  ) x
), JSON_ARRAY()),
fund_level2_json = COALESCE((
  SELECT JSON_ARRAYAGG(x.value) FROM (
    SELECT DISTINCT COALESCE(NULLIF(TRIM(j.acronym),''),NULLIF(TRIM(j.program),''),NULLIF(TRIM(j.program_name),''),
                             NULLIF(TRIM(j.grant_title),''),NULLIF(TRIM(j.project_title),''),NULLIF(TRIM(j.project_name),''),
                             NULLIF(TRIM(j.fund_name),''),NULLIF(TRIM(j.funding_name),'')) value
    FROM JSON_TABLE(p.grants_json, '$[*]'
      COLUMNS(program VARCHAR(1000) PATH '$.program' NULL ON EMPTY,
              program_name VARCHAR(1000) PATH '$.programName' NULL ON EMPTY,
              grant_title VARCHAR(1000) PATH '$.grantTitle' NULL ON EMPTY,
              project_title VARCHAR(1000) PATH '$.projectTitle' NULL ON EMPTY,
              project_name VARCHAR(1000) PATH '$.projectName' NULL ON EMPTY,
              fund_name VARCHAR(1000) PATH '$.fundName' NULL ON EMPTY,
              funding_name VARCHAR(1000) PATH '$.fundingName' NULL ON EMPTY,
              acronym VARCHAR(500) PATH '$.acronym' NULL ON EMPTY)) j
    WHERE COALESCE(NULLIF(TRIM(j.acronym),''),NULLIF(TRIM(j.program),''),NULLIF(TRIM(j.program_name),''),NULLIF(TRIM(j.grant_title),''),
                   NULLIF(TRIM(j.project_title),''),NULLIF(TRIM(j.project_name),''),NULLIF(TRIM(j.fund_name),''),
                   NULLIF(TRIM(j.funding_name),''),NULLIF(TRIM(j.acronym),'')) IS NOT NULL
  ) x
), JSON_ARRAY())
WHERE JSON_VALID(p.raw_payload)=1;

-- GenBase保存的OpenLB补充基金数组同样按两级字段回填。
UPDATE genbase_publication p
SET fund_level1_json = COALESCE((
  SELECT JSON_ARRAYAGG(x.value) FROM (
    SELECT DISTINCT TRIM(j.value) value FROM JSON_TABLE(p.grants_json, '$[*]'
      COLUMNS(value VARCHAR(1000) PATH '$.agency' NULL ON EMPTY)) j
    WHERE j.value IS NOT NULL AND TRIM(j.value) <> ''
  ) x
), JSON_ARRAY()),
fund_level2_json = COALESCE((
  SELECT JSON_ARRAYAGG(x.value) FROM (
    SELECT DISTINCT COALESCE(NULLIF(TRIM(j.acronym),''),NULLIF(TRIM(j.program),''),NULLIF(TRIM(j.program_name),''),
                             NULLIF(TRIM(j.grant_title),''),NULLIF(TRIM(j.project_title),''),NULLIF(TRIM(j.project_name),''),
                             NULLIF(TRIM(j.fund_name),''),NULLIF(TRIM(j.funding_name),'')) value
    FROM JSON_TABLE(p.grants_json, '$[*]'
      COLUMNS(program VARCHAR(1000) PATH '$.program' NULL ON EMPTY,
              program_name VARCHAR(1000) PATH '$.programName' NULL ON EMPTY,
              grant_title VARCHAR(1000) PATH '$.grantTitle' NULL ON EMPTY,
              project_title VARCHAR(1000) PATH '$.projectTitle' NULL ON EMPTY,
              project_name VARCHAR(1000) PATH '$.projectName' NULL ON EMPTY,
              fund_name VARCHAR(1000) PATH '$.fundName' NULL ON EMPTY,
              funding_name VARCHAR(1000) PATH '$.fundingName' NULL ON EMPTY,
              acronym VARCHAR(500) PATH '$.acronym' NULL ON EMPTY)) j
    WHERE COALESCE(NULLIF(TRIM(j.acronym),''),NULLIF(TRIM(j.program),''),NULLIF(TRIM(j.program_name),''),NULLIF(TRIM(j.grant_title),''),
                   NULLIF(TRIM(j.project_title),''),NULLIF(TRIM(j.project_name),''),NULLIF(TRIM(j.fund_name),''),
                   NULLIF(TRIM(j.funding_name),''),NULLIF(TRIM(j.acronym),'')) IS NOT NULL
  ) x
), JSON_ARRAY())
WHERE p.grants_json IS NOT NULL AND JSON_VALID(p.grants_json)=1;

-- 整合表旧字段可能来自任一来源；保留基金对象的一级和二级可识别值。
UPDATE research_stat_publication p
SET fund_level1_json = COALESCE((
  SELECT JSON_ARRAYAGG(x.value) FROM (
    SELECT DISTINCT TRIM(j.value) value FROM JSON_TABLE(p.grants_json, '$[*]'
      COLUMNS(value VARCHAR(1000) PATH '$.agency' NULL ON EMPTY)) j
    WHERE j.value IS NOT NULL AND TRIM(j.value) <> ''
  ) x
), JSON_ARRAY()),
fund_level2_json = COALESCE((
  SELECT JSON_ARRAYAGG(x.value) FROM (
    SELECT DISTINCT COALESCE(NULLIF(TRIM(j.acronym),''),NULLIF(TRIM(j.program),''),NULLIF(TRIM(j.program_name),''),
                             NULLIF(TRIM(j.grant_title),''),NULLIF(TRIM(j.project_title),''),NULLIF(TRIM(j.project_name),''),
                             NULLIF(TRIM(j.fund_name),''),NULLIF(TRIM(j.funding_name),'')) value
    FROM JSON_TABLE(p.grants_json, '$[*]'
      COLUMNS(program VARCHAR(1000) PATH '$.program' NULL ON EMPTY,
              program_name VARCHAR(1000) PATH '$.programName' NULL ON EMPTY,
              grant_title VARCHAR(1000) PATH '$.grantTitle' NULL ON EMPTY,
              project_title VARCHAR(1000) PATH '$.projectTitle' NULL ON EMPTY,
              project_name VARCHAR(1000) PATH '$.projectName' NULL ON EMPTY,
              fund_name VARCHAR(1000) PATH '$.fundName' NULL ON EMPTY,
              funding_name VARCHAR(1000) PATH '$.fundingName' NULL ON EMPTY,
              acronym VARCHAR(500) PATH '$.acronym' NULL ON EMPTY)) j
    WHERE COALESCE(NULLIF(TRIM(j.acronym),''),NULLIF(TRIM(j.program),''),NULLIF(TRIM(j.program_name),''),NULLIF(TRIM(j.grant_title),''),
                   NULLIF(TRIM(j.project_title),''),NULLIF(TRIM(j.project_name),''),NULLIF(TRIM(j.fund_name),''),
                   NULLIF(TRIM(j.funding_name),''),NULLIF(TRIM(j.acronym),'')) IS NOT NULL
  ) x
), JSON_ARRAY())
WHERE JSON_VALID(p.grants_json)=1;

ALTER TABLE openlb_publication DROP COLUMN grants_json;
ALTER TABLE genbase_publication DROP COLUMN grants_json;
ALTER TABLE research_stat_publication DROP COLUMN grants_json;

-- MySQL 8.0.18 / bio_portal
SET NAMES utf8mb4;
-- Completes V20 from compact grants_json arrays. Keep grants_json as the original funding evidence.
-- agency is level 1; acronym is preferred as level 2; grant IDs are never used as fund names.

UPDATE openlb_publication p
SET fund_level1_json = IF(fund_level1_json IS NULL OR TRIM(fund_level1_json) IN ('','[]'), COALESCE((
  SELECT JSON_ARRAYAGG(x.value) FROM (
    SELECT DISTINCT TRIM(j.value) value FROM JSON_TABLE(p.grants_json, '$[*]'
      COLUMNS(value VARCHAR(1000) PATH '$.agency' NULL ON EMPTY)) j
    WHERE j.value IS NOT NULL AND TRIM(j.value) <> ''
  ) x
), JSON_ARRAY()), fund_level1_json),
fund_level2_json = IF(fund_level2_json IS NULL OR TRIM(fund_level2_json) IN ('','[]'), COALESCE((
  SELECT JSON_ARRAYAGG(x.value) FROM (
    SELECT DISTINCT COALESCE(NULLIF(TRIM(j.acronym),''),NULLIF(TRIM(j.program),''),NULLIF(TRIM(j.program_name),''),
      NULLIF(TRIM(j.grant_title),''),NULLIF(TRIM(j.project_title),''),NULLIF(TRIM(j.project_name),''),
      NULLIF(TRIM(j.fund_name),''),NULLIF(TRIM(j.funding_name),''),NULLIF(TRIM(j.acronym),'')) value
    FROM JSON_TABLE(p.grants_json, '$[*]' COLUMNS(
      program VARCHAR(1000) PATH '$.program' NULL ON EMPTY,
      program_name VARCHAR(1000) PATH '$.programName' NULL ON EMPTY,
      grant_title VARCHAR(1000) PATH '$.grantTitle' NULL ON EMPTY,
      project_title VARCHAR(1000) PATH '$.projectTitle' NULL ON EMPTY,
      project_name VARCHAR(1000) PATH '$.projectName' NULL ON EMPTY,
      fund_name VARCHAR(1000) PATH '$.fundName' NULL ON EMPTY,
      funding_name VARCHAR(1000) PATH '$.fundingName' NULL ON EMPTY,
      acronym VARCHAR(500) PATH '$.acronym' NULL ON EMPTY)) j
    WHERE COALESCE(NULLIF(TRIM(j.acronym),''),NULLIF(TRIM(j.program),''),NULLIF(TRIM(j.program_name),''),
      NULLIF(TRIM(j.grant_title),''),NULLIF(TRIM(j.project_title),''),NULLIF(TRIM(j.project_name),''),
      NULLIF(TRIM(j.fund_name),''),NULLIF(TRIM(j.funding_name),''),NULLIF(TRIM(j.acronym),'')) IS NOT NULL
  ) x
), JSON_ARRAY()), fund_level2_json)
WHERE p.grants_json IS NOT NULL AND JSON_VALID(p.grants_json)=1;

UPDATE genbase_publication p
SET fund_level1_json = IF(fund_level1_json IS NULL OR TRIM(fund_level1_json) IN ('','[]'), COALESCE((
  SELECT JSON_ARRAYAGG(x.value) FROM (
    SELECT DISTINCT TRIM(j.value) value FROM JSON_TABLE(p.grants_json, '$[*]'
      COLUMNS(value VARCHAR(1000) PATH '$.agency' NULL ON EMPTY)) j
    WHERE j.value IS NOT NULL AND TRIM(j.value) <> ''
  ) x
), JSON_ARRAY()), fund_level1_json),
fund_level2_json = IF(fund_level2_json IS NULL OR TRIM(fund_level2_json) IN ('','[]'), COALESCE((
  SELECT JSON_ARRAYAGG(x.value) FROM (
    SELECT DISTINCT COALESCE(NULLIF(TRIM(j.acronym),''),NULLIF(TRIM(j.program),''),NULLIF(TRIM(j.program_name),''),
      NULLIF(TRIM(j.grant_title),''),NULLIF(TRIM(j.project_title),''),NULLIF(TRIM(j.project_name),''),
      NULLIF(TRIM(j.fund_name),''),NULLIF(TRIM(j.funding_name),''),NULLIF(TRIM(j.acronym),'')) value
    FROM JSON_TABLE(p.grants_json, '$[*]' COLUMNS(
      program VARCHAR(1000) PATH '$.program' NULL ON EMPTY,
      program_name VARCHAR(1000) PATH '$.programName' NULL ON EMPTY,
      grant_title VARCHAR(1000) PATH '$.grantTitle' NULL ON EMPTY,
      project_title VARCHAR(1000) PATH '$.projectTitle' NULL ON EMPTY,
      project_name VARCHAR(1000) PATH '$.projectName' NULL ON EMPTY,
      fund_name VARCHAR(1000) PATH '$.fundName' NULL ON EMPTY,
      funding_name VARCHAR(1000) PATH '$.fundingName' NULL ON EMPTY,
      acronym VARCHAR(500) PATH '$.acronym' NULL ON EMPTY)) j
    WHERE COALESCE(NULLIF(TRIM(j.acronym),''),NULLIF(TRIM(j.program),''),NULLIF(TRIM(j.program_name),''),
      NULLIF(TRIM(j.grant_title),''),NULLIF(TRIM(j.project_title),''),NULLIF(TRIM(j.project_name),''),
      NULLIF(TRIM(j.fund_name),''),NULLIF(TRIM(j.funding_name),''),NULLIF(TRIM(j.acronym),'')) IS NOT NULL
  ) x
), JSON_ARRAY()), fund_level2_json)
WHERE p.grants_json IS NOT NULL AND JSON_VALID(p.grants_json)=1;

UPDATE research_stat_publication p
SET fund_level1_json = IF(fund_level1_json IS NULL OR TRIM(fund_level1_json) IN ('','[]'), COALESCE((
  SELECT JSON_ARRAYAGG(x.value) FROM (
    SELECT DISTINCT TRIM(j.value) value FROM JSON_TABLE(p.grants_json, '$[*]'
      COLUMNS(value VARCHAR(1000) PATH '$.agency' NULL ON EMPTY)) j
    WHERE j.value IS NOT NULL AND TRIM(j.value) <> ''
  ) x
), JSON_ARRAY()), fund_level1_json),
fund_level2_json = IF(fund_level2_json IS NULL OR TRIM(fund_level2_json) IN ('','[]'), COALESCE((
  SELECT JSON_ARRAYAGG(x.value) FROM (
    SELECT DISTINCT COALESCE(NULLIF(TRIM(j.acronym),''),NULLIF(TRIM(j.program),''),NULLIF(TRIM(j.program_name),''),
      NULLIF(TRIM(j.grant_title),''),NULLIF(TRIM(j.project_title),''),NULLIF(TRIM(j.project_name),''),
      NULLIF(TRIM(j.fund_name),''),NULLIF(TRIM(j.funding_name),''),NULLIF(TRIM(j.acronym),'')) value
    FROM JSON_TABLE(p.grants_json, '$[*]' COLUMNS(
      program VARCHAR(1000) PATH '$.program' NULL ON EMPTY,
      program_name VARCHAR(1000) PATH '$.programName' NULL ON EMPTY,
      grant_title VARCHAR(1000) PATH '$.grantTitle' NULL ON EMPTY,
      project_title VARCHAR(1000) PATH '$.projectTitle' NULL ON EMPTY,
      project_name VARCHAR(1000) PATH '$.projectName' NULL ON EMPTY,
      fund_name VARCHAR(1000) PATH '$.fundName' NULL ON EMPTY,
      funding_name VARCHAR(1000) PATH '$.fundingName' NULL ON EMPTY,
      acronym VARCHAR(500) PATH '$.acronym' NULL ON EMPTY)) j
    WHERE COALESCE(NULLIF(TRIM(j.acronym),''),NULLIF(TRIM(j.program),''),NULLIF(TRIM(j.program_name),''),
      NULLIF(TRIM(j.grant_title),''),NULLIF(TRIM(j.project_title),''),NULLIF(TRIM(j.project_name),''),
      NULLIF(TRIM(j.fund_name),''),NULLIF(TRIM(j.funding_name),''),NULLIF(TRIM(j.acronym),'')) IS NOT NULL
  ) x
), JSON_ARRAY()), fund_level2_json)
WHERE p.grants_json IS NOT NULL AND JSON_VALID(p.grants_json)=1;


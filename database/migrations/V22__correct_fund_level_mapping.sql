-- MySQL 8.0 / bio_portal
-- 历史数据修正：不访问上游；从仍保留的原始响应JSON重新解析基金层级。
-- 一级 = grants[].agency；二级优先 grants[].acronym（如 GM），缺少缩写时回退到项目/计划名称。
-- grantId/grantNumber 是编号，不作为统计名称。旧 grants_json 列可按既定迁移移除，raw_payload 保留原始基金数据。
SET NAMES utf8mb4;

UPDATE openlb_publication p
SET fund_level1_json = COALESCE((
      SELECT JSON_ARRAYAGG(x.value) FROM (
        SELECT DISTINCT TRIM(j.agency) value
        FROM JSON_TABLE(p.raw_payload, '$.result.data[*]' COLUMNS(
          record_pmid VARCHAR(32) PATH '$.pmid' NULL ON EMPTY,
          NESTED PATH '$.grants[*]' COLUMNS(
            agency VARCHAR(1000) PATH '$.agency' NULL ON EMPTY,
            acronym VARCHAR(500) PATH '$.acronym' NULL ON EMPTY,
            program VARCHAR(1000) PATH '$.program' NULL ON EMPTY,
            program_name VARCHAR(1000) PATH '$.programName' NULL ON EMPTY,
            grant_title VARCHAR(1000) PATH '$.grantTitle' NULL ON EMPTY,
            project_title VARCHAR(1000) PATH '$.projectTitle' NULL ON EMPTY,
            project_name VARCHAR(1000) PATH '$.projectName' NULL ON EMPTY,
            fund_name VARCHAR(1000) PATH '$.fundName' NULL ON EMPTY,
            funding_name VARCHAR(1000) PATH '$.fundingName' NULL ON EMPTY
          )
        )) j
        WHERE j.record_pmid = p.pmid AND j.agency IS NOT NULL AND TRIM(j.agency) <> ''
      ) x
    ), JSON_ARRAY()),
    fund_level2_json = COALESCE((
      SELECT JSON_ARRAYAGG(x.value) FROM (
        SELECT DISTINCT COALESCE(NULLIF(TRIM(j.acronym),''),NULLIF(TRIM(j.program),''),
          NULLIF(TRIM(j.program_name),''),NULLIF(TRIM(j.grant_title),''),NULLIF(TRIM(j.project_title),''),
          NULLIF(TRIM(j.project_name),''),NULLIF(TRIM(j.fund_name),''),NULLIF(TRIM(j.funding_name),'')) value
        FROM JSON_TABLE(p.raw_payload, '$.result.data[*]' COLUMNS(
          record_pmid VARCHAR(32) PATH '$.pmid' NULL ON EMPTY,
          NESTED PATH '$.grants[*]' COLUMNS(
            agency VARCHAR(1000) PATH '$.agency' NULL ON EMPTY,
            acronym VARCHAR(500) PATH '$.acronym' NULL ON EMPTY,
            program VARCHAR(1000) PATH '$.program' NULL ON EMPTY,
            program_name VARCHAR(1000) PATH '$.programName' NULL ON EMPTY,
            grant_title VARCHAR(1000) PATH '$.grantTitle' NULL ON EMPTY,
            project_title VARCHAR(1000) PATH '$.projectTitle' NULL ON EMPTY,
            project_name VARCHAR(1000) PATH '$.projectName' NULL ON EMPTY,
            fund_name VARCHAR(1000) PATH '$.fundName' NULL ON EMPTY,
            funding_name VARCHAR(1000) PATH '$.fundingName' NULL ON EMPTY
          )
        )) j
        WHERE j.record_pmid = p.pmid AND COALESCE(NULLIF(TRIM(j.acronym),''),NULLIF(TRIM(j.program),''),
          NULLIF(TRIM(j.program_name),''),NULLIF(TRIM(j.grant_title),''),NULLIF(TRIM(j.project_title),''),
          NULLIF(TRIM(j.project_name),''),NULLIF(TRIM(j.fund_name),''),NULLIF(TRIM(j.funding_name),'')) IS NOT NULL
      ) x
    ), JSON_ARRAY())
WHERE JSON_VALID(p.raw_payload) = 1;

UPDATE genbase_publication p
SET fund_level1_json = COALESCE((
      SELECT JSON_ARRAYAGG(x.value) FROM (
        SELECT DISTINCT TRIM(j.agency) value
        FROM JSON_TABLE(p.openlb_raw_payload, '$.grants[*]' COLUMNS(
          agency VARCHAR(1000) PATH '$.agency' NULL ON EMPTY,
          acronym VARCHAR(500) PATH '$.acronym' NULL ON EMPTY,
          program VARCHAR(1000) PATH '$.program' NULL ON EMPTY,
          program_name VARCHAR(1000) PATH '$.programName' NULL ON EMPTY,
          grant_title VARCHAR(1000) PATH '$.grantTitle' NULL ON EMPTY,
          project_title VARCHAR(1000) PATH '$.projectTitle' NULL ON EMPTY,
          project_name VARCHAR(1000) PATH '$.projectName' NULL ON EMPTY,
          fund_name VARCHAR(1000) PATH '$.fundName' NULL ON EMPTY,
          funding_name VARCHAR(1000) PATH '$.fundingName' NULL ON EMPTY
        )) j
        WHERE j.agency IS NOT NULL AND TRIM(j.agency) <> ''
      ) x
    ), JSON_ARRAY()),
    fund_level2_json = COALESCE((
      SELECT JSON_ARRAYAGG(x.value) FROM (
        SELECT DISTINCT COALESCE(NULLIF(TRIM(j.acronym),''),NULLIF(TRIM(j.program),''),
          NULLIF(TRIM(j.program_name),''),NULLIF(TRIM(j.grant_title),''),NULLIF(TRIM(j.project_title),''),
          NULLIF(TRIM(j.project_name),''),NULLIF(TRIM(j.fund_name),''),NULLIF(TRIM(j.funding_name),'')) value
        FROM JSON_TABLE(p.openlb_raw_payload, '$.grants[*]' COLUMNS(
          agency VARCHAR(1000) PATH '$.agency' NULL ON EMPTY,
          acronym VARCHAR(500) PATH '$.acronym' NULL ON EMPTY,
          program VARCHAR(1000) PATH '$.program' NULL ON EMPTY,
          program_name VARCHAR(1000) PATH '$.programName' NULL ON EMPTY,
          grant_title VARCHAR(1000) PATH '$.grantTitle' NULL ON EMPTY,
          project_title VARCHAR(1000) PATH '$.projectTitle' NULL ON EMPTY,
          project_name VARCHAR(1000) PATH '$.projectName' NULL ON EMPTY,
          fund_name VARCHAR(1000) PATH '$.fundName' NULL ON EMPTY,
          funding_name VARCHAR(1000) PATH '$.fundingName' NULL ON EMPTY
        )) j
        WHERE COALESCE(NULLIF(TRIM(j.acronym),''),NULLIF(TRIM(j.program),''),
          NULLIF(TRIM(j.program_name),''),NULLIF(TRIM(j.grant_title),''),NULLIF(TRIM(j.project_title),''),
          NULLIF(TRIM(j.project_name),''),NULLIF(TRIM(j.fund_name),''),NULLIF(TRIM(j.funding_name),'')) IS NOT NULL
      ) x
    ), JSON_ARRAY())
WHERE p.openlb_raw_payload IS NOT NULL AND JSON_VALID(p.openlb_raw_payload) = 1;

-- 同一 PMID 在来源表中已修正，按 PMID 更新全局去重表，不扩展穿透记录。
UPDATE research_stat_publication r
LEFT JOIN openlb_publication o ON o.pmid = r.pmid
LEFT JOIN genbase_publication g ON g.pmid = r.pmid
SET r.fund_level1_json = JSON_MERGE_PRESERVE(
      COALESCE(CAST(o.fund_level1_json AS JSON), JSON_ARRAY()),
      COALESCE(CAST(g.fund_level1_json AS JSON), JSON_ARRAY())),
    r.fund_level2_json = JSON_MERGE_PRESERVE(
      COALESCE(CAST(o.fund_level2_json AS JSON), JSON_ARRAY()),
      COALESCE(CAST(g.fund_level2_json AS JSON), JSON_ARRAY())),
    r.update_time = NOW()
WHERE o.id IS NOT NULL OR g.id IS NOT NULL;

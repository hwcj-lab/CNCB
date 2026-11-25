-- MySQL 8.0 / bio_portal
-- V22 已从来源表原始JSON修正基金层级；本脚本只按 PMID 刷新全局整合表。
SET NAMES utf8mb4;
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

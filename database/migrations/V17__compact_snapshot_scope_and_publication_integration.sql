-- 快照统计范围统一使用scope_code：TOTAL表示总统级，其他值为来源数据库代码。
-- 应用此迁移前请停止正在运行的旧版快照构建器。
UPDATE research_stat_snapshot
SET status='FAILED',error_message='统计模型升级：旧版构建已结束',completed_time=NOW()
WHERE status='BUILDING';

-- 取消旧版“全部文献”虚拟维度及其全量关系；五个真实统计维度的穿透保留。
DELETE FROM research_stat_dimension WHERE dimension_type='SNAPSHOT_ALL';

ALTER TABLE research_stat_dimension
  DROP INDEX idx_research_dimension_query,
  DROP INDEX uq_research_snapshot_dimension,
  DROP COLUMN dimension_hash,
  DROP COLUMN scope_type,
  ADD COLUMN dimension_hash BINARY(32)
    GENERATED ALWAYS AS (UNHEX(SHA2(CONCAT(scope_code,'#',dimension_type,'#',dimension_key,'#',stat_year),256))) STORED
    COMMENT '范围内维度唯一摘要' AFTER dimension_key,
  ADD UNIQUE KEY uq_research_snapshot_dimension(snapshot_id,dimension_hash),
  ADD KEY idx_research_dimension_query(snapshot_id,scope_code,dimension_type,publication_count);

ALTER TABLE research_stat_publication
  ADD COLUMN keyword_list LONGTEXT NULL COMMENT '整合关键词，用于快照主题统计' AFTER grants_json,
  ADD COLUMN mesh_headings_json LONGTEXT NULL COMMENT '整合MeSH主题，用于快照主题统计' AFTER keyword_list,
  ADD COLUMN has_primary_source TINYINT(1) NOT NULL DEFAULT 1 COMMENT '至少一个来源将该记录标记为原始文献' AFTER mesh_headings_json,
  ADD COLUMN is_active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '当前来源仍存在；逻辑重建时保留历史快照引用' AFTER has_primary_source,
  ADD KEY idx_research_global_active(is_active,has_primary_source);

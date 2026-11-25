-- 统计穿透改为业务唯一值：TOTAL使用全局publication_key，分数据库使用来源自身source_record_key。
-- 现有关系通过research_stat_publication.source_record_keys回填；若历史记录缺少来源键，NOT NULL变更会失败，需先补齐映射。

ALTER TABLE research_stat_dimension_publication
  ADD COLUMN publication_key VARCHAR(320) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL
    COMMENT '统计范围内文献业务唯一值；TOTAL为全局键，分库为来源键' AFTER dimension_id;

UPDATE research_stat_dimension_publication r
JOIN research_stat_dimension d ON d.id=r.dimension_id
JOIN research_stat_publication p ON p.id=r.publication_id
SET r.publication_key = CASE
  WHEN d.scope_code='TOTAL' THEN p.publication_key
  ELSE NULLIF(JSON_UNQUOTE(JSON_EXTRACT(p.source_record_keys,CONCAT('$."',d.scope_code,'"'))),'null')
END;

ALTER TABLE research_stat_dimension_publication
  DROP FOREIGN KEY fk_research_dimension_publication_publication,
  DROP INDEX idx_research_dimension_publication_publication,
  DROP PRIMARY KEY,
  DROP COLUMN publication_id,
  MODIFY COLUMN publication_key VARCHAR(320) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL
    COMMENT '统计范围内文献业务唯一值；TOTAL为全局键，分库为来源键',
  ADD PRIMARY KEY(dimension_id,publication_key),
  ADD KEY idx_research_dimension_publication_key(publication_key);

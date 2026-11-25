-- 修复V18迁移在Windows PowerShell管道执行时可能发生的中文列注释编码问题。
ALTER TABLE research_stat_dimension_publication
  MODIFY COLUMN publication_key VARCHAR(320) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL
    COMMENT '统计范围内文献业务唯一值；TOTAL为全局键，分库为来源键';

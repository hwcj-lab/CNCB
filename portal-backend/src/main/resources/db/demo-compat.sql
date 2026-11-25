-- H2 demo兼容：MySQL前缀索引不受H2支持；只在demo配置执行，不替代MySQL迁移。
CREATE TABLE IF NOT EXISTS research_stat_dimension_candidate (
 snapshot_id BIGINT UNSIGNED NOT NULL COMMENT '正在构建的快照ID；该表只存构建中间结果，快照完成后清空',
 scope_code VARCHAR(100) NOT NULL COMMENT 'TOTAL为全局统计，其他值为来源数据库代码',
 dimension_type VARCHAR(30) NOT NULL COMMENT '仅暂存跨批排名候选：AUTHOR/INSTITUTION/FUND',
 dimension_hash BINARY(32) NOT NULL COMMENT '维度键SHA-256，用于低成本唯一索引与去重',
 dimension_key VARCHAR(700) NOT NULL COMMENT '维度稳定业务键；作者维度包含姓名和机构',
 display_name VARCHAR(1000) NOT NULL COMMENT '维度展示名称',
 author_name VARCHAR(500) NULL COMMENT 'AUTHOR维度作者姓名',
 institution_name VARCHAR(1000) NULL COMMENT 'AUTHOR维度机构名称',
 stat_year SMALLINT NOT NULL DEFAULT 0 COMMENT 'YEAR维度年份，其他维度为0',
 publication_key VARCHAR(320) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '文献来源业务唯一值；TOTAL为全局键，分库为来源键',
 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '候选关系写入时间',
 PRIMARY KEY(snapshot_id,scope_code,dimension_type,dimension_hash,publication_key),
 KEY idx_dimension_candidate_rank(snapshot_id,scope_code,dimension_type,dimension_hash),
 KEY idx_dimension_candidate_publication(snapshot_id,publication_key),
 KEY idx_dimension_candidate_key(snapshot_id,scope_code,dimension_type,dimension_key),
 CONSTRAINT fk_dimension_candidate_snapshot FOREIGN KEY(snapshot_id) REFERENCES research_stat_snapshot(id) ON DELETE CASCADE
) COMMENT='快照分批解析的临时维度候选及文献关联；用于数据库端计数、排名和生成穿透，不保留历史';

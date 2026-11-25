-- 快照采用游标分页和批量候选关系，避免把全部作者/机构/文献键保存在Java堆内存。
-- 仅在创建快照期间使用；成功、失败或停止后由服务删除该快照的候选行。
CREATE TABLE IF NOT EXISTS research_stat_dimension_candidate (
 snapshot_id BIGINT UNSIGNED NOT NULL COMMENT '正在构建的快照ID；该表只存构建中间结果，快照完成后清空',
 scope_code VARCHAR(100) NOT NULL COMMENT 'TOTAL为全局统计，其他值为来源数据库代码',
 dimension_type VARCHAR(30) NOT NULL COMMENT '仅暂存跨批排名候选：AUTHOR/INSTITUTION/FUND',
 dimension_hash BINARY(32) NOT NULL COMMENT '维度键SHA-256，用于唯一索引去重',
 dimension_key VARCHAR(700) NOT NULL COMMENT '维度稳定业务键；作者维度包含姓名和机构',
 display_name VARCHAR(1000) NOT NULL COMMENT '维度展示名称',
 author_name VARCHAR(500) NULL COMMENT 'AUTHOR维度作者姓名',
 institution_name VARCHAR(1000) NULL COMMENT 'AUTHOR维度机构名称',
 stat_year SMALLINT NOT NULL DEFAULT 0 COMMENT 'YEAR维度年份，其他维度为0',
 publication_key VARCHAR(320) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '文献业务唯一值；TOTAL为全局键，分库为来源键',
 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '候选关系写入时间',
 PRIMARY KEY(snapshot_id,scope_code,dimension_type,dimension_hash,publication_key),
 KEY idx_dimension_candidate_rank(snapshot_id,scope_code,dimension_type,dimension_hash),
 KEY idx_dimension_candidate_publication(snapshot_id,publication_key),
 KEY idx_dimension_candidate_key(snapshot_id,scope_code,dimension_type,dimension_key(191)),
 CONSTRAINT fk_dimension_candidate_snapshot FOREIGN KEY(snapshot_id)
   REFERENCES research_stat_snapshot(id) ON DELETE CASCADE
) COMMENT='快照分批解析作者、机构、基金候选；年度和主题直接写正式关系表，不在此暂存';

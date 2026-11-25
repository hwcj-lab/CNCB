USE bio_portal;
SET NAMES utf8mb4;

-- 重要：本脚本只重建统计快照四张表，会删除旧快照；不会删除OpenLB、GenBase、采集队列或关联文献数据。
DROP TABLE IF EXISTS research_stat_dimension_publication;
DROP TABLE IF EXISTS research_stat_dimension;
DROP TABLE IF EXISTS research_stat_publication;
DROP TABLE IF EXISTS research_stat_snapshot;

CREATE TABLE research_stat_snapshot (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '统计快照主键',
  version_no BIGINT UNSIGNED NOT NULL COMMENT '单调递增统计版本',
  snapshot_name VARCHAR(100) NULL COMMENT '用户可修改的快照名称',
  status VARCHAR(20) NOT NULL COMMENT 'BUILDING/SUCCESS/FAILED/STOPPED',
  source_codes VARCHAR(500) NOT NULL COMMENT '参与统计的来源代码',
  source_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '来源数量',
  include_related TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否包含纯关联发现文献',
  publication_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '全局去重后的文献数量',
  year_coverage_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '有年份的文献数',
  topic_coverage_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '有主题的文献数',
  author_coverage_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '有作者的文献数',
  institution_coverage_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '有机构的文献数',
  fund_coverage_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '有基金信息的文献数',
  error_message VARCHAR(2000) NULL COMMENT '失败或停止原因',
  started_time DATETIME NOT NULL COMMENT '构建开始时间', completed_time DATETIME NULL COMMENT '完成时间',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uq_research_snapshot_version(version_no), KEY idx_research_snapshot_status_version(status,version_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='科研统计快照版本';

CREATE TABLE research_stat_publication (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '全局文献主键',
  publication_key VARCHAR(320) NOT NULL COMMENT '全局去重键：PMID优先、DOI其次、最后来源记录键',
  content_hash CHAR(64) NOT NULL COMMENT '合并内容SHA-256；内容未变化时不更新',
  pmid VARCHAR(32) NULL COMMENT 'PubMed ID', doi VARCHAR(255) NULL COMMENT '规范化DOI',
  title TEXT NULL COMMENT '合并标题', pub_year SMALLINT NULL COMMENT '发表年份', journal_title VARCHAR(500) NULL COMMENT '期刊',
  source_codes VARCHAR(500) NOT NULL COMMENT '提供该文献的来源代码',
  source_record_keys JSON NOT NULL COMMENT '各来源原始记录标识',
  authors_json LONGTEXT NULL COMMENT '作者及作者署名机构JSON',
  affiliation_info LONGTEXT NULL COMMENT '文章机构汇总', grants_json LONGTEXT NULL COMMENT '基金原始JSON',
  keyword_list LONGTEXT NULL COMMENT '整合关键词', mesh_headings_json LONGTEXT NULL COMMENT '整合MeSH主题',
  has_primary_source TINYINT(1) NOT NULL DEFAULT 1 COMMENT '至少一个来源标记为原始文献',
  is_active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '当前来源仍存在；逻辑重建时保留历史快照引用',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次合并时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '内容最近变化时间',
  UNIQUE KEY uq_research_global_publication(publication_key),
  KEY idx_research_global_publication_year(pub_year), KEY idx_research_global_publication_pmid(pmid),
  KEY idx_research_global_publication_doi(doi), KEY idx_research_global_active(is_active,has_primary_source)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='跨来源全局去重文献；一篇文献只保存一次';

CREATE TABLE research_stat_dimension (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '统计维度主键', snapshot_id BIGINT UNSIGNED NOT NULL COMMENT '所属快照',
  scope_code VARCHAR(100) NOT NULL COMMENT 'TOTAL表示总统级；其他值为动态数据库来源代码',
  dimension_type VARCHAR(30) NOT NULL COMMENT 'YEAR/TOPIC/AUTHOR/INSTITUTION/FUND',
  dimension_key VARCHAR(700) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '归一化维度键',
  dimension_hash BINARY(32) GENERATED ALWAYS AS
    (UNHEX(SHA2(CONCAT(scope_code,'#',dimension_type,'#',dimension_key,'#',stat_year),256))) STORED COMMENT '范围内维度唯一摘要',
  display_name VARCHAR(1000) NOT NULL COMMENT '展示名称', author_name VARCHAR(500) NULL COMMENT '作者名',
  institution_name VARCHAR(1000) NULL COMMENT '作者对应署名机构', stat_year SMALLINT NOT NULL DEFAULT 0 COMMENT '年份；非年度为0',
  publication_count INT UNSIGNED NOT NULL COMMENT '该维度去重文献数', create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY uq_research_snapshot_dimension(snapshot_id,dimension_hash),
  KEY idx_research_dimension_query(snapshot_id,scope_code,dimension_type,publication_count),
  CONSTRAINT fk_research_dimension_snapshot FOREIGN KEY(snapshot_id) REFERENCES research_stat_snapshot(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='快照预聚合统计维度';

CREATE TABLE research_stat_dimension_publication (
  dimension_id BIGINT UNSIGNED NOT NULL COMMENT '维度ID',
  publication_key VARCHAR(320) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '统计范围内文献业务唯一值；TOTAL为全局键，分库为来源键',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY(dimension_id,publication_key),
  CONSTRAINT fk_research_dimension_publication_dimension FOREIGN KEY(dimension_id) REFERENCES research_stat_dimension(id) ON DELETE CASCADE,
  KEY idx_research_dimension_publication_key(publication_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仅保存年度、主题、作者、机构、基金五类统计维度与文献的穿透关系';

-- MySQL不允许对被外键引用的父表直接TRUNCATE，即使外键为ON DELETE CASCADE。
-- 统一通过该过程执行高速重置；异常时会恢复当前会话的外键检查状态。
DELIMITER //
DROP PROCEDURE IF EXISTS reset_research_statistics//
CREATE PROCEDURE reset_research_statistics()
BEGIN
  DECLARE old_fk INT DEFAULT @@FOREIGN_KEY_CHECKS;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    SET FOREIGN_KEY_CHECKS = old_fk;
    RESIGNAL;
  END;
  SET FOREIGN_KEY_CHECKS = 0;
  TRUNCATE TABLE research_stat_dimension_publication;
  TRUNCATE TABLE research_stat_dimension;
  TRUNCATE TABLE research_stat_snapshot;
  TRUNCATE TABLE research_stat_publication;
  SET FOREIGN_KEY_CHECKS = old_fk;
END//
DELIMITER ;

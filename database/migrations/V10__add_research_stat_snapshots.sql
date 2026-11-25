USE bio_portal;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS research_stat_snapshot (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '统计快照主键',
    version_no BIGINT UNSIGNED NOT NULL COMMENT '单调递增的统计版本号',
    status VARCHAR(20) NOT NULL COMMENT 'BUILDING/SUCCESS/FAILED/STOPPED',
    source_codes VARCHAR(500) NOT NULL COMMENT '本版本实际参与统计的来源代码，逗号分隔',
    source_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '参与统计的数据来源数量',
    publication_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '来源间去重后的文献数量',
    year_coverage_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '具有有效发表年份的文献数量',
    topic_coverage_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '具有关键词或MeSH主题的文献数量',
    author_coverage_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '具有可解析作者的文献数量',
    institution_coverage_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '具有可解析机构的文献数量',
    fund_coverage_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '具有可解析基金信息的文献数量',
    error_message VARCHAR(2000) NULL COMMENT '构建失败原因；成功版本为空',
    started_time DATETIME NOT NULL COMMENT '统计开始时间',
    completed_time DATETIME NULL COMMENT '统计完成或失败时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uq_research_snapshot_version (version_no),
    KEY idx_research_snapshot_status_version (status, version_no)
) COMMENT='科研产出统计历史版本；成功版本建立后不可修改';

CREATE TABLE IF NOT EXISTS research_stat_publication (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '快照文献主键',
    snapshot_id BIGINT UNSIGNED NOT NULL COMMENT '所属统计快照',
    publication_key VARCHAR(320) NOT NULL COMMENT '跨来源去重键：PMID优先，其次DOI，最后来源记录键',
    pmid VARCHAR(32) NULL COMMENT 'PubMed ID',
    doi VARCHAR(255) NULL COMMENT '规范化DOI',
    title TEXT NULL COMMENT '合并后的文献标题',
    pub_year SMALLINT NULL COMMENT '发表年份',
    journal_title VARCHAR(500) NULL COMMENT '期刊名称',
    source_codes VARCHAR(500) NOT NULL COMMENT '提供该文献的来源代码，逗号分隔',
    source_record_keys JSON NOT NULL COMMENT '各来源原始记录标识，用于回溯',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uq_research_snapshot_publication (snapshot_id, publication_key),
    KEY idx_research_snapshot_publication_year (snapshot_id, pub_year),
    KEY idx_research_snapshot_publication_pmid (pmid),
    CONSTRAINT fk_research_stat_publication_snapshot FOREIGN KEY (snapshot_id)
        REFERENCES research_stat_snapshot(id)
) COMMENT='每个统计版本内去重后的文献证据索引';

CREATE TABLE IF NOT EXISTS research_stat_dimension (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '聚合结果主键',
    snapshot_id BIGINT UNSIGNED NOT NULL COMMENT '所属统计快照',
    dimension_type VARCHAR(30) NOT NULL COMMENT 'YEAR/TOPIC/AUTHOR/INSTITUTION/FUND/SOURCE',
    dimension_key VARCHAR(700) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '归一化后的维度键；二进制排序确保唯一性与Java键一致',
    dimension_hash BINARY(32) GENERATED ALWAYS AS
        (UNHEX(SHA2(CONCAT(dimension_type, '#', dimension_key, '#', stat_year), 256))) STORED
        COMMENT '维度唯一键SHA-256，避免为长文本建立大型唯一索引',
    display_name VARCHAR(1000) NOT NULL COMMENT '图表展示名称',
    stat_year SMALLINT NOT NULL DEFAULT 0 COMMENT '趋势年份；非趋势维度固定为0',
    publication_count INT UNSIGNED NOT NULL COMMENT '去重文献计数',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uq_research_snapshot_dimension (snapshot_id, dimension_hash),
    KEY idx_research_dimension_query (snapshot_id, dimension_type, publication_count),
    CONSTRAINT fk_research_stat_dimension_snapshot FOREIGN KEY (snapshot_id)
        REFERENCES research_stat_snapshot(id)
) COMMENT='统计快照预聚合维度，供图表快速查询';

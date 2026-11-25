USE bio_portal;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS genbase_publication (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '本地自增主键',
    paper_id BIGINT UNSIGNED NOT NULL COMMENT 'GenBase支持文章接口的稳定记录ID',
    pmid VARCHAR(32) NULL COMMENT 'PubMed ID；接口字段名为pubmedId',
    doi VARCHAR(255) NULL COMMENT '规范化DOI（去除URL前缀并转小写）',
    bioproject_accession VARCHAR(500) NULL COMMENT 'BioProject编号；可能为空或包含多个编号，因此不设唯一约束',
    journal_name VARCHAR(500) NULL COMMENT '期刊名称',
    publisher VARCHAR(500) NULL COMMENT '出版商',
    paper_title TEXT NOT NULL COMMENT '文章标题',
    publish_time VARCHAR(32) NULL COMMENT '上游原始发表时间，如2026-7',
    publish_year SMALLINT NULL COMMENT '从publishTime解析出的年份',
    volume VARCHAR(100) NULL COMMENT '卷号',
    genbase_accessions TEXT NULL COMMENT 'GenBase序列登录号或范围（可能包含多个登录号或范围）',
    mission_id VARCHAR(1000) NULL COMMENT 'GenBase任务ID列表',
    access_link TEXT NULL COMMENT 'GenBase记录访问地址列表',
    source_url VARCHAR(1500) NOT NULL COMMENT '数据来源接口地址',
    raw_payload JSON NOT NULL COMMENT '接口单条记录原始JSON，便于追溯和重新解析',
    fetched_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近采集时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uq_genbase_paper_id (paper_id),
    UNIQUE KEY uq_genbase_pmid (pmid),
    UNIQUE KEY uq_genbase_doi (doi),
    KEY idx_genbase_bioproject (bioproject_accession),
    KEY idx_genbase_publish_year (publish_year)
) COMMENT='GenBase支持文章接口原始文献表';

CREATE TABLE IF NOT EXISTS genbase_collection_job (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '采集作业主键',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/STOPPED/SUCCESS/FAILED',
    next_start INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '下一页DataTables start偏移量',
    page_size INT UNSIGNED NOT NULL DEFAULT 100 COMMENT '每页请求记录数',
    records_total INT UNSIGNED NULL COMMENT '上游最近返回的总记录数',
    rows_seen INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '本次作业已读取记录数',
    rows_inserted INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '本次作业新增入库记录数',
    attempt_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '当前页连续失败次数',
    last_error VARCHAR(1000) NULL COMMENT '最后一次错误',
    started_time DATETIME NULL COMMENT '开始或恢复时间',
    completed_time DATETIME NULL COMMENT '成功、停止或失败时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_genbase_job_status_id (status, id)
) COMMENT='GenBase分页采集断点；失败或停止后从next_start继续';


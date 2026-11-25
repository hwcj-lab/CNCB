-- MySQL 8 / H2 MODE=MySQL；在选定数据库中执行；不DROP、不自动清空已有数据。
CREATE TABLE IF NOT EXISTS data_type (
 id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT, code VARCHAR(50) NOT NULL, name VARCHAR(100) NOT NULL, description VARCHAR(500), sort_order INT NOT NULL DEFAULT 0,
 is_visible BOOLEAN NOT NULL DEFAULT TRUE, status VARCHAR(20) NOT NULL DEFAULT 'ENABLED', create_by VARCHAR(20) NOT NULL, update_by VARCHAR(20) NOT NULL,
 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_data_type_visible_sort ON data_type(is_visible,sort_order);
CREATE TABLE IF NOT EXISTS database_resource (
 id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT, catalog_id BIGINT NULL UNIQUE COMMENT 'CNCB目录接口外层数值id；搜索目录独有记录为空', db_id VARCHAR(500) NULL COMMENT 'CNCB目录dbId及跨库搜索标识，多个值以分号分隔', name VARCHAR(160) NOT NULL, db_categories_id BIGINT, db_url VARCHAR(1000) NULL,
 http_method VARCHAR(10) DEFAULT 'GET', db_en_description TEXT, db_en_title TEXT, db_zh_description TEXT, db_zh_title TEXT,
 organization_id BIGINT, type_id BIGINT, is_visible BOOLEAN NOT NULL DEFAULT TRUE, status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
 create_by VARCHAR(20) NOT NULL, update_by VARCHAR(20) NOT NULL, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_database_name ON database_resource(name);
CREATE UNIQUE INDEX uq_database_db_id ON database_resource(db_id);
CREATE TABLE IF NOT EXISTS online_tool (
 id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT, bt_id VARCHAR(50) NOT NULL UNIQUE, name VARCHAR(160) NOT NULL, platform VARCHAR(160),
 bt_en_description TEXT, bt_en_title TEXT, bt_zh_description TEXT, bt_zh_title TEXT, bt_url VARCHAR(1500), http_method VARCHAR(10) DEFAULT 'GET',
 is_visible BOOLEAN NOT NULL DEFAULT TRUE, status VARCHAR(20) NOT NULL DEFAULT 'ENABLED', create_by VARCHAR(20) NOT NULL, update_by VARCHAR(20) NOT NULL,
 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_tool_name ON online_tool(name);
CREATE TABLE IF NOT EXISTS research_fund_dictionary (
 grant_id VARCHAR(255) NOT NULL COMMENT '资助项目唯一编号',
 full_name VARCHAR(1000) NULL COMMENT '基金/资助项目英文全称',
 acronym VARCHAR(255) NULL COMMENT '官方缩写',
 agency VARCHAR(1000) NULL COMMENT '资助机构名称',
 source_name VARCHAR(100) NULL COMMENT '字典数据来源',
 status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
 update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
 PRIMARY KEY(grant_id), KEY idx_research_fund_dictionary_status(status)
) COMMENT='基金编号及其规范英文名称字典；由管理员批量导入，不在快照时请求外部服务';
CREATE TABLE IF NOT EXISTS database_data_type (
 id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT, database_id BIGINT UNSIGNED NOT NULL, data_type_id BIGINT UNSIGNED NOT NULL,
 relation_role VARCHAR(30) NOT NULL DEFAULT 'DIRECT_DATA', relation_note VARCHAR(800), create_by VARCHAR(20) NOT NULL, update_by VARCHAR(20) NOT NULL,
 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(database_id,data_type_id), FOREIGN KEY(database_id) REFERENCES database_resource(id) ON DELETE CASCADE, FOREIGN KEY(data_type_id) REFERENCES data_type(id)
);
CREATE TABLE IF NOT EXISTS database_tool (
 id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT, database_id BIGINT UNSIGNED NOT NULL, tool_id BIGINT UNSIGNED NOT NULL,
 relation_type VARCHAR(30) NOT NULL DEFAULT 'COMPATIBLE', relation_basis VARCHAR(1000), create_by VARCHAR(20) NOT NULL, update_by VARCHAR(20) NOT NULL,
 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(database_id,tool_id), FOREIGN KEY(database_id) REFERENCES database_resource(id) ON DELETE CASCADE, FOREIGN KEY(tool_id) REFERENCES online_tool(id) ON DELETE CASCADE
);
CREATE TABLE IF NOT EXISTS openlb_publication (
 id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '本地自增主键',
 pmid VARCHAR(32) NOT NULL COMMENT 'OpenLB查询及数据库内唯一值', doi VARCHAR(255) COMMENT '规范化DOI',
 title TEXT COMMENT '文献标题', abstract_text LONGTEXT COMMENT '摘要；用于详情和后续主题分析', pub_year SMALLINT COMMENT '发表年份',
 journal_title VARCHAR(500) COMMENT '期刊名称', source_url VARCHAR(1500) NOT NULL COMMENT '官方来源地址',
 authors_json LONGTEXT COMMENT '作者数组及逐作者原始署名机构；快照构建时解析，不在采集时调用ROR', affiliation_info LONGTEXT COMMENT '文章机构汇总原文',
 keyword_list LONGTEXT COMMENT '关键词', mesh_headings_json LONGTEXT COMMENT 'MeSH主题数组',
 grants_json LONGTEXT NOT NULL COMMENT '上游原始基金JSON；优先grants，缺失时将funding编号标准化为grantId对象', cncb_banks_json LONGTEXT NOT NULL COMMENT 'CNCB资源关联数组',
 raw_payload LONGTEXT NOT NULL COMMENT 'OpenLB完整原始响应，供离线重解析',
 is_primary TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否为OpenLB初始/原始文献',
 is_related TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否曾由关联链路发现；可与is_primary同时为1',
 relation_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/COMPLETED/FAILED',
 relation_attempt_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '关联发现累计尝试次数',
 relation_last_error VARCHAR(1000) COMMENT '最近一次关联发现错误', relation_checked_at DATETIME COMMENT '最近一次完整关联检查时间',
 source_hash CHAR(64) COMMENT '来源内容SHA-256，用于变化检测',
 fetched_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 UNIQUE KEY uq_openlb_pmid(pmid), KEY idx_openlb_pub_year(pub_year), KEY idx_openlb_doi(doi),
 KEY idx_openlb_relation_schedule(relation_status,id), KEY idx_openlb_primary_related(is_primary,is_related)
) COMMENT='OpenLB原始文献采集表；仅保留cncbBanks非空记录';
CREATE TABLE IF NOT EXISTS openlb_pmid_task (
 id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '采集任务自增主键',
 pmid VARCHAR(32) NOT NULL COMMENT 'OpenLB查询使用的PubMed ID，全局唯一',
 status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/SUCCESS/EMPTY/FAILED',
 attempt_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '已执行请求次数',
 last_error VARCHAR(1000) COMMENT '最后一次失败或恢复原因',
 last_attempt_time DATETIME COMMENT '最后一次开始请求时间',
 next_retry_time DATETIME COMMENT '失败后允许下次重试的时间',
 completed_time DATETIME COMMENT 'SUCCESS或EMPTY完成时间',
 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
 update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
 UNIQUE KEY uq_openlb_task_pmid(pmid), KEY idx_openlb_task_schedule(status,next_retry_time,id)
) COMMENT='OpenLB PMID持久化采集队列，用于去重、断点续采和错误重试';
CREATE TABLE IF NOT EXISTS genbase_publication (
 id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '本地自增主键',
 paper_id BIGINT UNSIGNED NULL COMMENT 'GenBase支持文章接口记录ID；关联发现文献可为空',
 pmid VARCHAR(32) NULL COMMENT 'PubMed ID；接口字段名为pubmedId',
 doi VARCHAR(255) NULL COMMENT '规范化DOI',
 bioproject_accession VARCHAR(500) NULL COMMENT 'BioProject编号；不设唯一约束',
 journal_name VARCHAR(500) COMMENT '期刊名称', paper_title TEXT NOT NULL COMMENT '文章标题',
 publish_year SMALLINT COMMENT '解析后的发表年份', genbase_accessions TEXT COMMENT 'GenBase序列登录号或范围（可能包含多个登录号或范围）', source_url VARCHAR(1500) NOT NULL COMMENT '来源接口',
 raw_payload JSON NOT NULL COMMENT '接口单条记录原始JSON',
 openlb_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/SUCCESS/EMPTY/FAILED/NO_PMID',
 openlb_attempt_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'OpenLB补全请求次数', openlb_last_error VARCHAR(1000) COMMENT 'OpenLB补全最后错误',
 openlb_last_attempt_time DATETIME COMMENT '最近一次OpenLB请求时间', openlb_completed_time DATETIME COMMENT 'OpenLB补全完成时间',
 abstract_text LONGTEXT COMMENT '论文摘要', affiliation_info LONGTEXT COMMENT '论文署名机构原始文本', keyword_list LONGTEXT COMMENT '关键词原始文本',
 authors_json LONGTEXT COMMENT '作者数组及逐作者原始署名机构；快照构建时解析，不在采集时调用ROR', mesh_headings_json LONGTEXT COMMENT 'MeSH主题数组JSON', grants_json LONGTEXT COMMENT '上游原始基金JSON；优先grants，缺失时将funding编号标准化为grantId对象',
 cncb_banks_json LONGTEXT COMMENT 'CNCB数据库关联数组JSON；允许null或空数组', openlb_raw_payload LONGTEXT COMMENT 'OpenLB单条记录原始JSON',
 is_primary TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否来自GenBase支持文章列表',
 is_related TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否曾由关联链路发现；可与is_primary同时为1',
 relation_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/COMPLETED/FAILED',
 relation_attempt_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '关联发现累计尝试次数',
 relation_last_error VARCHAR(1000) COMMENT '最近一次关联发现错误', relation_checked_at DATETIME COMMENT '最近一次完整关联检查时间',
 source_hash CHAR(64) COMMENT '来源内容SHA-256，用于变化检测',
 fetched_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 UNIQUE KEY uq_genbase_paper_id(paper_id), UNIQUE KEY uq_genbase_pmid(pmid), UNIQUE KEY uq_genbase_doi(doi),
 KEY idx_genbase_bioproject(bioproject_accession), KEY idx_genbase_publish_year(publish_year), KEY idx_genbase_openlb_schedule(openlb_status,id),
 KEY idx_genbase_relation_schedule(relation_status,id), KEY idx_genbase_primary_related(is_primary,is_related)
) COMMENT='GenBase支持文章接口原始文献表';
CREATE TABLE IF NOT EXISTS genbase_collection_job (
 id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '采集作业主键',
 status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/STOPPED/SUCCESS/FAILED',
 next_start INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '下一页DataTables start偏移量',
 page_size INT UNSIGNED NOT NULL DEFAULT 100 COMMENT '每页请求记录数',
 records_total INT UNSIGNED COMMENT '上游最近返回总记录数', rows_seen INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '已读取记录数', rows_inserted INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '新增入库记录数',
 attempt_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '当前页连续失败次数', last_error VARCHAR(1000) COMMENT '最后一次错误', started_time DATETIME COMMENT '开始或恢复时间', completed_time DATETIME COMMENT '结束时间',
 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
 update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
 KEY idx_genbase_job_status_id(status,id)
) COMMENT='GenBase分页采集断点；失败或停止后从next_start继续';
CREATE TABLE IF NOT EXISTS research_publication_relation (
 id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '关联关系自增主键', database_code VARCHAR(40) NOT NULL COMMENT 'OPENLB/GENBASE',
 left_identifier_type VARCHAR(20) NOT NULL COMMENT '左端唯一值类型', left_identifier VARCHAR(255) NOT NULL COMMENT '排序后较小唯一值',
 right_identifier_type VARCHAR(20) NOT NULL COMMENT '右端唯一值类型', right_identifier VARCHAR(255) NOT NULL COMMENT '排序后较大唯一值',
 relation_type VARCHAR(40) NOT NULL DEFAULT 'RELATED_PUBLICATION' COMMENT '关系类型', evidence_url VARCHAR(1500) COMMENT '官方证据页',
 discovery_depth INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '最短发现深度', active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '当前是否有效',
 last_seen_run_id BIGINT UNSIGNED, first_seen_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, last_seen_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 UNIQUE(database_code,left_identifier_type,left_identifier,right_identifier_type,right_identifier,relation_type)
);
CREATE TABLE IF NOT EXISTS research_relation_crawl_job (
 id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '关联采集作业ID', database_code VARCHAR(40) NOT NULL, root_identifier_type VARCHAR(20) NOT NULL,
 root_identifier VARCHAR(255) NOT NULL, refresh_publication TINYINT(1) NOT NULL DEFAULT 0, refresh_relations TINYINT(1) NOT NULL DEFAULT 1,
 status VARCHAR(20) NOT NULL DEFAULT 'PENDING', max_publications INT UNSIGNED NOT NULL DEFAULT 1000, request_delay_ms INT UNSIGNED NOT NULL DEFAULT 500,
 processed_count INT UNSIGNED NOT NULL DEFAULT 0, discovered_count INT UNSIGNED NOT NULL DEFAULT 0, relation_count INT UNSIGNED NOT NULL DEFAULT 0,
 last_error VARCHAR(2000), started_time DATETIME, completed_time DATETIME, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS research_relation_crawl_queue (
 id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT, job_id BIGINT UNSIGNED NOT NULL, identifier_type VARCHAR(20) NOT NULL, identifier_value VARCHAR(255) NOT NULL,
 depth INT UNSIGNED NOT NULL DEFAULT 0, status VARCHAR(20) NOT NULL DEFAULT 'PENDING', attempt_count INT UNSIGNED NOT NULL DEFAULT 0,
 last_error VARCHAR(1000), create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 UNIQUE(job_id,identifier_type,identifier_value), FOREIGN KEY(job_id) REFERENCES research_relation_crawl_job(id) ON DELETE CASCADE
);
CREATE TABLE IF NOT EXISTS research_topic_category (
 id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT, topic_code VARCHAR(50) NOT NULL UNIQUE, topic_name VARCHAR(100) NOT NULL,
 description VARCHAR(1000), sort_order INT NOT NULL DEFAULT 0, status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS research_database_topic (
 id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT, database_code VARCHAR(40) NOT NULL, topic_id BIGINT UNSIGNED NOT NULL,
 evidence VARCHAR(1500), verification_status VARCHAR(30) NOT NULL DEFAULT 'PENDING', create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, UNIQUE(database_code,topic_id),
 FOREIGN KEY(topic_id) REFERENCES research_topic_category(id)
);
CREATE TABLE IF NOT EXISTS research_stat_snapshot (
 id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT, version_no BIGINT UNSIGNED NOT NULL UNIQUE, snapshot_name VARCHAR(100), status VARCHAR(20) NOT NULL,
 source_codes VARCHAR(500) NOT NULL, source_count INT UNSIGNED NOT NULL DEFAULT 0,
 include_resources TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否纳入无原生文献资源', resource_count BIGINT NOT NULL DEFAULT 0 COMMENT '本快照纳入的无文献资源数量',
 include_related TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否将纯关联发现文献纳入本快照统计', publication_count INT UNSIGNED NOT NULL DEFAULT 0,
 year_coverage_count INT UNSIGNED NOT NULL DEFAULT 0, topic_coverage_count INT UNSIGNED NOT NULL DEFAULT 0,
 author_coverage_count INT UNSIGNED NOT NULL DEFAULT 0, institution_coverage_count INT UNSIGNED NOT NULL DEFAULT 0,
 fund_coverage_count INT UNSIGNED NOT NULL DEFAULT 0, error_message VARCHAR(2000), started_time DATETIME NOT NULL,
 completed_time DATETIME, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS research_stat_publication (
 id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT, publication_key VARCHAR(320) NOT NULL COMMENT '全局文献去重键',
 record_kind VARCHAR(20) NOT NULL DEFAULT 'PUBLICATION' COMMENT 'PUBLICATION文献或RESOURCE无文献资源', resource_accession VARCHAR(320) NULL COMMENT 'RESOURCE原库编号', resource_url VARCHAR(1500) NULL COMMENT 'RESOURCE原库详情页',
 raw_payload LONGTEXT NULL COMMENT '无文献资源原始JSON；文献可为空',
 relation_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '无文献资源关联发现状态；文献行不使用',
 relation_last_error VARCHAR(1800) NULL COMMENT '无文献资源最近关联错误',
 content_hash CHAR(64) NOT NULL COMMENT '合并内容SHA-256；只有变化时才更新',
 pmid VARCHAR(32), doi VARCHAR(255), title TEXT, pub_year SMALLINT, journal_title VARCHAR(500), source_codes VARCHAR(500) NOT NULL,
 source_record_keys JSON NOT NULL, authors_json LONGTEXT COMMENT '快照作者数组JSON',
 affiliation_info LONGTEXT COMMENT '从作者数组汇总的文章署名机构原文', grants_json LONGTEXT COMMENT '合并来源的原始基金JSON数组；统计快照阶段解析',
 keyword_list LONGTEXT COMMENT '整合关键词', mesh_headings_json LONGTEXT COMMENT '整合MeSH主题',
 cncb_banks_json LONGTEXT COMMENT '整合CNCB关联数据库与资源编号JSON，用于研究主题分类',
 has_primary_source TINYINT(1) NOT NULL DEFAULT 1 COMMENT '至少一个来源标记为原始文献',
 is_active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '当前来源仍存在，逻辑重建不删除历史引用文献',
 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 UNIQUE KEY uq_research_global_publication(publication_key), KEY idx_research_global_publication_year(pub_year),
 KEY idx_research_global_active(is_active,has_primary_source),
 KEY idx_integrated_resource_scan(record_kind,source_codes,is_active,id),
 KEY idx_research_global_publication_pmid(pmid), KEY idx_research_global_publication_doi(doi)
);
CREATE TABLE IF NOT EXISTS research_stat_dimension (
 id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT, snapshot_id BIGINT UNSIGNED NOT NULL,
 scope_code VARCHAR(100) NOT NULL DEFAULT 'TOTAL' COMMENT 'TOTAL表示总统级；其他值为动态数据库来源代码',
 dimension_type VARCHAR(30) NOT NULL,
 dimension_key VARCHAR(700) NOT NULL, dimension_hash BINARY(32), display_name VARCHAR(1000) NOT NULL,
 author_name VARCHAR(500) NULL COMMENT 'AUTHOR维度作者姓名', institution_name VARCHAR(1000) NULL COMMENT 'AUTHOR维度作者署名机构',
 stat_year SMALLINT NOT NULL DEFAULT 0, publication_count INT UNSIGNED NOT NULL, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 KEY idx_research_dimension_query(snapshot_id,scope_code,dimension_type,publication_count),
 FOREIGN KEY(snapshot_id) REFERENCES research_stat_snapshot(id) ON DELETE CASCADE
);
CREATE TABLE IF NOT EXISTS research_stat_dimension_publication (
 dimension_id BIGINT UNSIGNED NOT NULL COMMENT '统计维度ID',
 publication_key VARCHAR(320) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '统计范围内文献业务唯一值；TOTAL为全局键，分库为来源键',
 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(dimension_id,publication_key),
 FOREIGN KEY(dimension_id) REFERENCES research_stat_dimension(id) ON DELETE CASCADE,
 KEY idx_research_dimension_publication_key(publication_key)
);
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
 KEY idx_dimension_candidate_key(snapshot_id,scope_code,dimension_type,dimension_key(191)),
 CONSTRAINT fk_dimension_candidate_snapshot FOREIGN KEY(snapshot_id) REFERENCES research_stat_snapshot(id) ON DELETE CASCADE
) COMMENT='快照分批解析的临时维度候选及文献关联；用于数据库端计数、排名和生成穿透，不保留历史';



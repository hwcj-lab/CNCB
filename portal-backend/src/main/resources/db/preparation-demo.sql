-- H2 demo only; MySQL acceptance uses real migration.
-- Three independent stages: collection -> preparation -> snapshots. No historical backfill.
CREATE TABLE IF NOT EXISTS research_preparation_job (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,status VARCHAR(20) NOT NULL,
 scan_mode VARCHAR(20) NOT NULL DEFAULT 'FULL',after_update_time DATETIME(6) NULL,
 source_codes_json TEXT NOT NULL,source_index INT NOT NULL DEFAULT 0,after_id BIGINT NOT NULL DEFAULT 0,batch_size INT NOT NULL DEFAULT 200,
 rows_scanned BIGINT NOT NULL DEFAULT 0,rows_changed BIGINT NOT NULL DEFAULT 0,rebuild BOOLEAN NOT NULL DEFAULT 0,all_sources BOOLEAN NOT NULL DEFAULT 1,
 error_message VARCHAR(2000),started_time DATETIME NOT NULL,completed_time DATETIME,
 running_slot INT GENERATED ALWAYS AS (CASE WHEN status='RUNNING' THEN 1 ELSE NULL END),
 UNIQUE KEY uk_preparation_running(running_slot)
) ;
CREATE TABLE IF NOT EXISTS research_prepared_document (
 source_row_id BIGINT NULL,
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,source_code VARCHAR(40) NOT NULL,source_key VARCHAR(1000) NOT NULL,source_hash CHAR(64) NOT NULL,
 document_key VARCHAR(80) NOT NULL,pmid VARCHAR(32),doi VARCHAR(500),title TEXT,journal_title VARCHAR(1000),pub_year INT,source_url TEXT,
 record_kind VARCHAR(20) NOT NULL,resource_accession VARCHAR(255),gsa_accessions TEXT,bioproject_accession VARCHAR(255),primary_flag BOOLEAN NOT NULL,
 content_hash CHAR(64) NOT NULL,seen_run BIGINT NOT NULL,valid_from BIGINT NOT NULL,valid_until BIGINT NULL,
 active_source_hash CHAR(64) GENERATED ALWAYS AS (CASE WHEN valid_until IS NULL THEN source_hash ELSE NULL END),
 UNIQUE KEY uk_prepared_active(source_code,active_source_hash),UNIQUE KEY uk_prepared_version(source_code,source_hash,valid_from),
 KEY ix_prepared_source_row(source_code,source_row_id,valid_until),KEY ix_prepared_version(valid_from,valid_until),KEY ix_prepared_scope(source_code,primary_flag,record_kind,document_key),KEY ix_prepared_document(document_key),KEY ix_prepared_doi(doi)
) ;
CREATE TABLE IF NOT EXISTS research_prepared_identity (
 revision BIGINT NOT NULL,doi VARCHAR(500) NOT NULL,pmid VARCHAR(32),PRIMARY KEY(revision,doi)
) ;
CREATE TABLE IF NOT EXISTS research_prepared_author (
 document_id BIGINT NOT NULL,source_code VARCHAR(40) NOT NULL,document_key VARCHAR(80) NOT NULL,
 pmid VARCHAR(32),doi VARCHAR(500),source_url TEXT,member_key CHAR(64) NOT NULL,display_name VARCHAR(1000) NOT NULL,
 author_name VARCHAR(500),institution_name VARCHAR(1000),author_order INT,stat_year INT,
 PRIMARY KEY(document_id,member_key),KEY ix_prepared_author_rank(member_key,document_key,source_code),KEY ix_prepared_author_source(source_code,member_key,document_key)
 ) ;
CREATE TABLE IF NOT EXISTS research_prepared_institution (
 document_id BIGINT NOT NULL,source_code VARCHAR(40) NOT NULL,document_key VARCHAR(80) NOT NULL,
 pmid VARCHAR(32),doi VARCHAR(500),source_url TEXT,member_key CHAR(64) NOT NULL,display_name VARCHAR(1000) NOT NULL,
 author_name VARCHAR(500),institution_name VARCHAR(1000),author_order INT,stat_year INT,
 PRIMARY KEY(document_id,member_key),KEY ix_prepared_institution_rank(member_key,document_key,source_code),KEY ix_prepared_institution_source(source_code,member_key,document_key)
 ) ;
CREATE TABLE IF NOT EXISTS research_prepared_fund (
 document_id BIGINT NOT NULL,source_code VARCHAR(40) NOT NULL,document_key VARCHAR(80) NOT NULL,
 pmid VARCHAR(32),doi VARCHAR(500),source_url TEXT,member_key CHAR(64) NOT NULL,display_name VARCHAR(1000) NOT NULL,
 author_name VARCHAR(500),institution_name VARCHAR(1000),author_order INT,stat_year INT,
 PRIMARY KEY(document_id,member_key),KEY ix_prepared_fund_rank(member_key,document_key,source_code),KEY ix_prepared_fund_source(source_code,member_key,document_key)
 ) ;
CREATE TABLE IF NOT EXISTS research_prepared_topic (
 document_id BIGINT NOT NULL,source_code VARCHAR(40) NOT NULL,document_key VARCHAR(80) NOT NULL,
 pmid VARCHAR(32),doi VARCHAR(500),source_url TEXT,member_key CHAR(64) NOT NULL,display_name VARCHAR(1000) NOT NULL,
 author_name VARCHAR(500),institution_name VARCHAR(1000),author_order INT,stat_year INT,
 PRIMARY KEY(document_id,member_key),KEY ix_prepared_topic_rank(member_key,document_key,source_code),KEY ix_prepared_topic_source(source_code,member_key,document_key)
 ) ;
CREATE TABLE IF NOT EXISTS research_prepared_year (
 document_id BIGINT NOT NULL,source_code VARCHAR(40) NOT NULL,document_key VARCHAR(80) NOT NULL,
 pmid VARCHAR(32),doi VARCHAR(500),source_url TEXT,member_key CHAR(64) NOT NULL,display_name VARCHAR(1000) NOT NULL,
 author_name VARCHAR(500),institution_name VARCHAR(1000),author_order INT,stat_year INT,
 PRIMARY KEY(document_id,member_key),KEY ix_prepared_year_rank(member_key,document_key,source_code),KEY ix_prepared_year_source(source_code,member_key,document_key)
 ) ;
CREATE TABLE IF NOT EXISTS research_prepared_snapshot (
 snapshot_id BIGINT UNSIGNED NOT NULL PRIMARY KEY,preparation_revision BIGINT NOT NULL,source_names_json TEXT NOT NULL,topic_scopes_json TEXT NOT NULL,
 KEY ix_prepared_snapshot_revision(preparation_revision)
) ;
CREATE TABLE IF NOT EXISTS research_preparation_source_cursor (
 job_id BIGINT NOT NULL,source_code VARCHAR(40) NOT NULL,scan_mode VARCHAR(20) NOT NULL,status VARCHAR(20) NOT NULL,
 from_time DATETIME(6) NULL,until_time DATETIME(6) NOT NULL,after_update_time DATETIME(6) NULL,after_id BIGINT NOT NULL DEFAULT 0,
 PRIMARY KEY(job_id,source_code),KEY ix_preparation_source_baseline(source_code,job_id)
) ;

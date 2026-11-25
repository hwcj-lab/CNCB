USE bio_portal;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS openlb_publication (
 id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
 openlb_id VARCHAR(64) NOT NULL,
 pmid VARCHAR(32) NOT NULL,
 owner VARCHAR(80), pub_status VARCHAR(80), pub_year SMALLINT, pub_model VARCHAR(80),
 journal_issn VARCHAR(40), journal_eissn VARCHAR(40), journal_volume VARCHAR(80), journal_issue VARCHAR(80),
 pub_date VARCHAR(100), journal_title VARCHAR(500), journal_iso_abbr VARCHAR(200),
 title TEXT, pagination VARCHAR(100), abstract_text LONGTEXT, copyright_info TEXT,
 author_string LONGTEXT, affiliation_info LONGTEXT, language VARCHAR(80), vernacular_title TEXT,
 publication_type_list TEXT, country VARCHAR(120), keyword_list LONGTEXT,
 doi VARCHAR(255), pii VARCHAR(255), pmcid VARCHAR(64), pprid VARCHAR(128), license_text VARCHAR(500), category VARCHAR(500),
 source_url VARCHAR(1500) NOT NULL, source_last_modified DATETIME, source_create_date DATETIME, data_source VARCHAR(100),
 authors_json LONGTEXT, mesh_headings_json LONGTEXT, data_banks_json LONGTEXT, chemicals_json LONGTEXT,
 references_json LONGTEXT, grants_json LONGTEXT, cncb_banks_json LONGTEXT NOT NULL, raw_payload LONGTEXT NOT NULL,
 cncb_bank_count INT UNSIGNED NOT NULL, grant_count INT UNSIGNED NOT NULL DEFAULT 0,
 fetched_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 UNIQUE KEY uq_openlb_openlb_id(openlb_id), UNIQUE KEY uq_openlb_pmid(pmid),
 KEY idx_openlb_pub_year(pub_year), KEY idx_openlb_doi(doi), KEY idx_openlb_source_modified(source_last_modified)
) COMMENT='OpenLB原始文献采集表；仅保留cncbBanks非空记录';

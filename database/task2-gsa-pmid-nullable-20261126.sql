-- Compatibility repair: the existing native GSA collector supports DOI-only papers.
-- The unique gsa_record_key index already exists. No data deletion or historical backfill.
-- Fail quickly rather than wait indefinitely for a metadata lock.
SET SESSION lock_wait_timeout=5;
ALTER TABLE gsa_publication
 MODIFY COLUMN pmid VARCHAR(32) NULL COMMENT 'PubMed ID; NULL for native records without PMID',
 ALGORITHM=INPLACE, LOCK=NONE;

-- Add only when an equivalent index does not already exist. No OpenLB fallback table scan.
SET SESSION lock_wait_timeout=5;
ALTER TABLE ascanceratlas_publication ADD KEY ix_preparation_updated(update_time,id), ALGORITHM=INPLACE, LOCK=NONE;
ALTER TABLE biocode_publication ADD KEY ix_preparation_updated(update_time,id), ALGORITHM=INPLACE, LOCK=NONE;
ALTER TABLE bioproject_publication ADD KEY ix_preparation_updated(update_time,id), ALGORITHM=INPLACE, LOCK=NONE;
ALTER TABLE biosample_publication ADD KEY ix_preparation_updated(update_time,id), ALGORITHM=INPLACE, LOCK=NONE;
ALTER TABLE databasecommons_publication ADD KEY ix_preparation_updated(update_time,id), ALGORITHM=INPLACE, LOCK=NONE;
ALTER TABLE ewas_publication ADD KEY ix_preparation_updated(update_time,id), ALGORITHM=INPLACE, LOCK=NONE;
ALTER TABLE gen_publication ADD KEY ix_preparation_updated(update_time,id), ALGORITHM=INPLACE, LOCK=NONE;
ALTER TABLE genbase_publication ADD KEY ix_preparation_updated(update_time,id), ALGORITHM=INPLACE, LOCK=NONE;
ALTER TABLE gsa_publication ADD KEY ix_preparation_updated(update_time,id), ALGORITHM=INPLACE, LOCK=NONE;
ALTER TABLE gvm_publication ADD KEY ix_preparation_updated(update_time,id), ALGORITHM=INPLACE, LOCK=NONE;
ALTER TABLE gwh_publication ADD KEY ix_preparation_updated(update_time,id), ALGORITHM=INPLACE, LOCK=NONE;
ALTER TABLE omix_publication ADD KEY ix_preparation_updated(update_time,id), ALGORITHM=INPLACE, LOCK=NONE;
ALTER TABLE research_stat_publication ADD KEY ix_preparation_resource_updated(record_kind,source_codes,update_time,id), ALGORITHM=INPLACE, LOCK=NONE;

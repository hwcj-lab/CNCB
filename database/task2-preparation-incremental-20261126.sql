-- Existing preparation installations: run once. Additive DDL only, no history backfill.
SET SESSION lock_wait_timeout=5;
ALTER TABLE research_preparation_job ADD COLUMN scan_mode VARCHAR(20) NOT NULL DEFAULT 'FULL' COMMENT '扫描方式：FULL全量扫描，INCREMENTAL增量扫描；强制重解析另看rebuild', ALGORITHM=INSTANT;
ALTER TABLE research_preparation_job ADD COLUMN after_update_time DATETIME(6) NULL COMMENT '当前来源已提交批次的更新时间游标；全量模式为空', ALGORITHM=INSTANT;
ALTER TABLE research_prepared_document ADD COLUMN source_row_id BIGINT NULL COMMENT '来源适配器提供的扫描行ID；可能编码资源/文献类型，不一定等于原表物理ID', ALGORITHM=INSTANT;
ALTER TABLE research_prepared_document ADD KEY ix_prepared_source_row(source_code,source_row_id,valid_until), ALGORITHM=INPLACE, LOCK=NONE;
CREATE TABLE IF NOT EXISTS research_preparation_source_cursor (
 job_id BIGINT NOT NULL COMMENT '所属整理任务ID，逻辑引用research_preparation_job.id',
 source_code VARCHAR(40) NOT NULL COMMENT '本条游标对应的数据库编码',
 scan_mode VARCHAR(20) NOT NULL COMMENT '该来源实际扫描方式：FULL或INCREMENTAL；无增量基线时可回退FULL',
 status VARCHAR(20) NOT NULL COMMENT '该来源处理状态：PENDING待处理、RUNNING处理中、COMPLETE已完成',
 from_time DATETIME(6) NULL COMMENT '增量扫描包含的时间下界，已包含重叠窗口；全量模式为空',
 until_time DATETIME(6) NOT NULL COMMENT '本轮固定时间上界，不包含此时间；供增量扫描和后续基线衔接使用',
 after_update_time DATETIME(6) NULL COMMENT '最近已提交批次的更新时间游标；与after_id构成增量有序游标',
 after_id BIGINT NOT NULL DEFAULT 0 COMMENT '最近已提交批次的来源扫描ID；同更新时间下用于稳定分页',
 PRIMARY KEY(job_id,source_code),
 KEY ix_preparation_source_baseline(source_code,job_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='逐来源的整理扫描计划和持久化游标；支持增量、失败重试及断点续传';

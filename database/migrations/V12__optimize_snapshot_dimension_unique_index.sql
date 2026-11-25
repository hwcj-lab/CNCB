USE bio_portal;
SET NAMES utf8mb4;

-- 长文本联合唯一索引在数万作者/机构维度批量写入时维护成本过高。
-- 原始键继续完整保存，只使用固定32字节SHA-256维护版本内唯一性。
ALTER TABLE research_stat_dimension
    DROP INDEX uq_research_snapshot_dimension,
    ADD COLUMN dimension_hash BINARY(32) GENERATED ALWAYS AS
        (UNHEX(SHA2(CONCAT(dimension_type, '#', dimension_key, '#', stat_year), 256))) STORED
        COMMENT '维度唯一键SHA-256，避免为长文本建立大型唯一索引'
        AFTER dimension_key,
    ADD UNIQUE KEY uq_research_snapshot_dimension (snapshot_id, dimension_hash);

-- 服务被测试进程终止时，将未完成版本显式标记失败；成功版本不会受影响。
UPDATE research_stat_snapshot
SET status='FAILED', error_message='测试进程停止：维度唯一索引已优化，请重新生成快照', completed_time=NOW()
WHERE status='BUILDING';

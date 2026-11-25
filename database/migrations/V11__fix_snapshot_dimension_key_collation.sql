USE bio_portal;
SET NAMES utf8mb4;

-- MySQL默认utf8mb4_0900_ai_ci会把César与Cesar等键判定为相同，
-- 但应用层保留重音差异。改为二进制排序，确保批量聚合与唯一索引口径一致。
ALTER TABLE research_stat_dimension
    MODIFY COLUMN dimension_key VARCHAR(700)
        CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL
        COMMENT '归一化后的维度键；二进制排序确保唯一性与Java键一致';

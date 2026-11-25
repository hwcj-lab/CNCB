USE bio_portal;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS=0;

-- 快照删除时同步清理文献和维度；历史版本之间互不影响。
ALTER TABLE research_stat_publication
    DROP FOREIGN KEY fk_research_stat_publication_snapshot;
ALTER TABLE research_stat_publication
    ADD CONSTRAINT fk_research_stat_publication_snapshot
        FOREIGN KEY (snapshot_id) REFERENCES research_stat_snapshot(id) ON DELETE CASCADE,
    ALGORITHM=INPLACE, LOCK=NONE;

ALTER TABLE research_stat_dimension
    DROP FOREIGN KEY fk_research_stat_dimension_snapshot;
ALTER TABLE research_stat_dimension
    ADD CONSTRAINT fk_research_stat_dimension_snapshot
        FOREIGN KEY (snapshot_id) REFERENCES research_stat_snapshot(id) ON DELETE CASCADE,
    ALGORITHM=INPLACE, LOCK=NONE;

SET FOREIGN_KEY_CHECKS=1;

CREATE TABLE IF NOT EXISTS research_stat_dimension_publication (
    dimension_id BIGINT UNSIGNED NOT NULL COMMENT '统计维度ID',
    publication_id BIGINT UNSIGNED NOT NULL COMMENT '快照文献ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (dimension_id, publication_id),
    KEY idx_research_dimension_publication_publication (publication_id),
    CONSTRAINT fk_research_dimension_publication_dimension
        FOREIGN KEY (dimension_id) REFERENCES research_stat_dimension(id) ON DELETE CASCADE,
    CONSTRAINT fk_research_dimension_publication_publication
        FOREIGN KEY (publication_id) REFERENCES research_stat_publication(id) ON DELETE CASCADE
) COMMENT='统计维度与快照文献的穿透关系';


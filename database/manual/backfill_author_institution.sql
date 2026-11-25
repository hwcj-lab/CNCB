-- 可选历史回填脚本（MySQL 8）。
-- 新采集与新快照不需要执行本脚本；仅用于给既有来源记录和既有快照补作者/机构/基金。
-- 每500个来源主键提交一次，支持中断后重新执行；已填充的affiliation_info不会覆盖。
USE bio_portal;
SET NAMES utf8mb4;
SET SESSION group_concat_max_len = 1048576;

DROP TABLE IF EXISTS author_affiliation_backfill_stage;
CREATE TABLE author_affiliation_backfill_stage AS
SELECT 'OPENLB' source_code, p.id source_id,
       GROUP_CONCAT(DISTINCT NULLIF(TRIM(a.affiliation), '') ORDER BY a.affiliation SEPARATOR '; ') affiliations
FROM openlb_publication p
JOIN JSON_TABLE(p.authors_json, '$[*]' COLUMNS(affiliation TEXT PATH '$.affiliation')) a
GROUP BY p.id
UNION ALL
SELECT 'GENBASE', p.id,
       GROUP_CONCAT(DISTINCT NULLIF(TRIM(a.affiliation), '') ORDER BY a.affiliation SEPARATOR '; ')
FROM genbase_publication p
JOIN JSON_TABLE(p.authors_json, '$[*]' COLUMNS(affiliation TEXT PATH '$.affiliation')) a
GROUP BY p.id;
ALTER TABLE author_affiliation_backfill_stage ADD PRIMARY KEY(source_code,source_id);

DROP PROCEDURE IF EXISTS backfill_author_institution;
DELIMITER //
CREATE PROCEDURE backfill_author_institution()
BEGIN
    DECLARE cursor_id BIGINT DEFAULT 0;
    DECLARE max_id BIGINT DEFAULT 0;

    SELECT COALESCE(MAX(id),0) INTO max_id FROM openlb_publication;
    WHILE cursor_id <= max_id DO
        UPDATE openlb_publication p
        JOIN author_affiliation_backfill_stage s ON s.source_code='OPENLB' AND s.source_id=p.id
        SET p.affiliation_info=s.affiliations
        WHERE p.id>=cursor_id AND p.id<cursor_id+500
          AND (p.affiliation_info IS NULL OR p.affiliation_info='') AND s.affiliations IS NOT NULL;
        COMMIT;
        SET cursor_id=cursor_id+500;
    END WHILE;

    SET cursor_id=0;
    SELECT COALESCE(MAX(id),0) INTO max_id FROM genbase_publication;
    WHILE cursor_id <= max_id DO
        UPDATE genbase_publication p
        JOIN author_affiliation_backfill_stage s ON s.source_code='GENBASE' AND s.source_id=p.id
        SET p.affiliation_info=s.affiliations
        WHERE p.id>=cursor_id AND p.id<cursor_id+500
          AND (p.affiliation_info IS NULL OR p.affiliation_info='') AND s.affiliations IS NOT NULL;
        COMMIT;
        SET cursor_id=cursor_id+500;
    END WHILE;

    SET cursor_id=0;
    SELECT COALESCE(MAX(id),0) INTO max_id FROM research_stat_publication;
    WHILE cursor_id <= max_id DO
        UPDATE research_stat_publication rp
        LEFT JOIN openlb_publication o ON o.pmid=rp.pmid
        LEFT JOIN genbase_publication g ON g.pmid=rp.pmid
        SET rp.authors_json=COALESCE(o.authors_json,g.authors_json),
            rp.affiliation_info=COALESCE(NULLIF(o.affiliation_info,''),NULLIF(g.affiliation_info,'')),
            rp.grants_json=COALESCE(o.grants_json,g.grants_json)
        WHERE rp.id>=cursor_id AND rp.id<cursor_id+500 AND rp.pmid IS NOT NULL;
        COMMIT;
        SET cursor_id=cursor_id+500;
    END WHILE;
END//
DELIMITER ;

CALL backfill_author_institution();
DROP PROCEDURE backfill_author_institution;
DROP TABLE author_affiliation_backfill_stage;

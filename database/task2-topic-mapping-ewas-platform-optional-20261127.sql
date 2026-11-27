-- 可选：仅在EWAS来源代表整个EWAS Open Platform时执行。
-- 平台包括Atlas知识库、DataHub数据集、Toolkit工具。若仅采集DataHub，不执行此脚本。
-- 与主要映射脚本配合后，EWAS具有DATABASE、OMICS_DATASET、TOOL三个分类。
-- 这会让该来源全部文献在三个分类中分别计数；并非逐篇文献的功能标签识别。
SET SESSION lock_wait_timeout=5;

INSERT INTO research_database_topic
    (database_code, topic_id, evidence, verification_status)
SELECT 'EWAS', categories.id,
       CASE categories.topic_code
           WHEN 'DATABASE' THEN 'EWAS Open Platform包含人工整理表观基因组关联与因果知识的EWAS Atlas；按完整平台范围增加知识资源分类。https://ngdc.cncb.ac.cn/ewas/document'
           WHEN 'TOOL' THEN 'EWAS Open Platform包含提供上游批次校正及下游富集分析的EWAS Toolkit；按完整平台范围增加工具分类。https://ngdc.cncb.ac.cn/ewas/document'
       END,
       'CONFIRMED'
FROM research_topic_category AS categories
WHERE categories.status = 'ENABLED' AND categories.topic_code IN ('DATABASE','TOOL')
ON DUPLICATE KEY UPDATE
    evidence = VALUES(evidence),
    verification_status = VALUES(verification_status),
    update_time = CURRENT_TIMESTAMP;

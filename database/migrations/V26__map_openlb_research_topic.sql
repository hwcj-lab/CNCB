-- OpenLB 是跨资源的文献库，不属于项目、样本、工具等具体资源类型；
-- 在按采集来源统计研究主题时暂归“其他”。后续可维护 research_database_topic 调整。
INSERT INTO research_database_topic(database_code,topic_id,evidence,verification_status)
SELECT 'OPENLB',id,'OpenLB 为跨数据库生命科学文献库；按采集来源统计时归入其他。','CONFIRMED'
FROM research_topic_category WHERE topic_code='OTHER'
ON DUPLICATE KEY UPDATE evidence=VALUES(evidence),verification_status=VALUES(verification_status),update_time=CURRENT_TIMESTAMP;

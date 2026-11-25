-- MySQL：在 bio_portal 中执行；只建表/配置路由，不采集、不迁移、不删除数据。
USE bio_portal;
SET NAMES utf8mb4;

-- 与当前分流及公共关联适配器一致：source_code 小写 + _publication。
-- LIKE 复制字段、字段注释、主键及唯一索引，不复制已有文献数据。
CREATE TABLE IF NOT EXISTS gwh_publication LIKE openlb_publication;
CREATE TABLE IF NOT EXISTS databasecommons_publication LIKE openlb_publication;
CREATE TABLE IF NOT EXISTS gsa_publication LIKE openlb_publication;
CREATE TABLE IF NOT EXISTS bioproject_publication LIKE openlb_publication;
CREATE TABLE IF NOT EXISTS ascanceratlas_publication LIKE openlb_publication;
CREATE TABLE IF NOT EXISTS biocode_publication LIKE openlb_publication;

ALTER TABLE gwh_publication COMMENT='GWH所属文献；OpenLB补充来源，PMID唯一';
ALTER TABLE databasecommons_publication COMMENT='Database Commons所属文献；OpenLB补充来源，PMID唯一';
ALTER TABLE gsa_publication COMMENT='GSA所属文献；OpenLB补充来源，PMID唯一';
ALTER TABLE bioproject_publication COMMENT='BioProject所属文献；OpenLB补充来源，PMID唯一';
ALTER TABLE ascanceratlas_publication COMMENT='ASCancerAtlas所属文献；OpenLB补充来源，PMID唯一';
ALTER TABLE biocode_publication COMMENT='BioCode所属文献；OpenLB补充来源，PMID唯一';

-- 建好全部目标表后再启用路由，防止采集/统计访问尚未存在的表。
INSERT INTO openlb_bank_route(bank_db,source_code,source_name,enabled) VALUES
('GWH','GWH','GWH',1),
('Database Commons','DATABASECOMMONS','Database Commons',1),
('GSA','GSA','GSA',1),
('BioProject','BIOPROJECT','BioProject',1),
('ASCancerAtlas','ASCANCERATLAS','ASCancerAtlas',1),
('BioCode','BIOCODE','BioCode',1)
ON DUPLICATE KEY UPDATE source_code=VALUES(source_code),source_name=VALUES(source_name),enabled=VALUES(enabled);

-- 保留既有GenBase路由、数据和专用字段适配，不需要重新创建genbase_publication。
SELECT bank_db,source_code,CONCAT(LOWER(source_code),'_publication') target_table,enabled
FROM openlb_bank_route ORDER BY source_code;

-- 检查所有目标表均存在（missing_table 应为0）。
SELECT r.bank_db,r.source_code,(t.table_name IS NULL) missing_table
FROM openlb_bank_route r
LEFT JOIN information_schema.tables t ON t.table_schema=DATABASE()
 AND t.table_name=CONCAT(LOWER(r.source_code),'_publication')
WHERE r.enabled=1;

-- 检查模板及目标表的PMID唯一索引；LIKE不会修复已经存在且结构不同的表。
SELECT table_name,index_name,column_name,non_unique
FROM information_schema.statistics
WHERE table_schema=DATABASE() AND table_name IN
('openlb_publication','gwh_publication','databasecommons_publication','gsa_publication',
 'bioproject_publication','ascanceratlas_publication','biocode_publication')
AND column_name='pmid' AND non_unique=0;

-- 执行后：
-- 1. 已保存OpenLB数据：POST /api/collection/openlb/banks/redistribute-existing?afterId=0
--    每次最多100条，使用nextAfterId继续至processed=0；不请求上游。
-- 2. 尚未采集的PMID：POST /api/collection/openlb/jobs/start，走新路由。
-- 3. 历史整合表：分流完成后按需调用全量重建，再生成新快照。
-- 4. 总主题图还需要research_database_topic映射，不能仅靠本路由推断研究主题。

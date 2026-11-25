-- 先执行本文件，再启动新后端。不回填历史记录，不执行采集。
CREATE TABLE IF NOT EXISTS openlb_bank_inventory_job (
 id INT PRIMARY KEY COMMENT '固定1',cursor_pmid VARCHAR(32) NOT NULL DEFAULT '' COMMENT '已完成的PMID字典序游标',
 message VARCHAR(1000) COMMENT '最后进度或停止原因',update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='OpenLB来源盘点持久化断点';
INSERT IGNORE INTO openlb_bank_inventory_job(id) VALUES(1);
CREATE TABLE IF NOT EXISTS openlb_bank_inventory (
 pmid VARCHAR(32) NOT NULL COMMENT '查询PMID',bank_index INT NOT NULL COMMENT '数组下标',
 name VARCHAR(1000) COMMENT '资源名称',accession VARCHAR(255) COMMENT '资源编号',link TEXT COMMENT '资源详情链接',
 db VARCHAR(255) COMMENT 'cncbBanks数据库名称',bank_pmid VARCHAR(32) COMMENT '银行对象自己的PMID',
 PRIMARY KEY(pmid,bank_index),KEY idx_bank_db(db)
) COMMENT='仅保存cncbBanks字段的来源盘点结果';
CREATE TABLE IF NOT EXISTS openlb_bank_route (
 bank_db VARCHAR(255) PRIMARY KEY COMMENT 'cncbBanks.db原始名称',source_code VARCHAR(40) NOT NULL COMMENT '大写数据库代码；表名为小写代码_publication',
 source_name VARCHAR(200) NOT NULL COMMENT '页面名称',enabled TINYINT NOT NULL DEFAULT 1 COMMENT '启用分流及统计'
) COMMENT='OpenLB实际数据库路由配置；目标表必须先创建';
-- 已有GenBase走专用字段适配；其他库采用OpenLB同结构模板。
INSERT IGNORE INTO openlb_bank_route VALUES('GenBase','GENBASE','GenBase',1);
-- 示例：盘点后确认需要Database Commons时执行以下两句（不预建未知库）：
-- CREATE TABLE databasecommons_publication LIKE openlb_publication;
-- INSERT INTO openlb_bank_route VALUES('Database Commons','DATABASECOMMONS','Database Commons',1);
-- 给新source_code配置research_database_topic，才能归入总主题标签。
-- 旧OpenLB关联与文献保留；历史快照保持历史口径。
-- 全局整合表清理请调用已有全量重建接口；重建仅读取当前注册的真实数据库。

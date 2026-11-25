/*
 Navicat Premium Dump SQL

 Source Server         : 生物数据资源推荐与科研产出分析平台
 Source Server Type    : MySQL
 Source Server Version : 80018 (8.0.18)
 Source Host           : localhost:3306
 Source Schema         : bio_portal

 Target Server Type    : MySQL
 Target Server Version : 80018 (8.0.18)
 File Encoding         : 65001

 Date: 20/11/2026 16:21:08
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for research_stat_publication
-- ----------------------------
DROP TABLE IF EXISTS `research_stat_publication`;
CREATE TABLE `research_stat_publication`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '全局文献主键',
  `publication_key` varchar(320) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '全局去重键：PMID优先、DOI其次、最后来源记录键',
  `content_hash` char(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '合并内容SHA-256；内容未变化时不更新',
  `pmid` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'PubMed ID',
  `doi` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '规范化DOI',
  `title` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '合并标题',
  `pub_year` smallint(6) NULL DEFAULT NULL COMMENT '发表年份',
  `journal_title` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '期刊',
  `source_codes` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '提供该文献的来源代码',
  `source_record_keys` json NOT NULL COMMENT '各来源原始记录标识',
  `authors_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '作者及作者署名机构JSON',
  `affiliation_info` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '文章机构汇总',
  `fund_level1_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '鏁村悎鏂囩尞涓?骇鍩洪噾鏈烘瀯/璧勫姪鏂瑰幓閲岼SON鏁扮粍',
  `fund_level2_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '鏁村悎鏂囩尞浜岀骇鍩洪噾椤圭洰鎴栧熀閲戠紪鍙峰幓閲岼SON鏁扮粍',
  `keyword_list` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '整合关键词，用于快照主题统计',
  `mesh_headings_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '整合MeSH主题，用于快照主题统计',
  `cncb_banks_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '整合CNCB关联数据库与资源编号JSON，用于研究主题分类',
  `has_primary_source` tinyint(1) NOT NULL DEFAULT 1 COMMENT '至少一个来源将该记录标记为原始文献',
  `is_active` tinyint(1) NOT NULL DEFAULT 1 COMMENT '当前来源仍存在；逻辑重建时保留历史快照引用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次合并时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '内容最近变化时间',
  `grants_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '原始基金JSON；快照阶段解析',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uq_research_global_publication`(`publication_key` ASC) USING BTREE,
  INDEX `idx_research_global_publication_year`(`pub_year` ASC) USING BTREE,
  INDEX `idx_research_global_publication_pmid`(`pmid` ASC) USING BTREE,
  INDEX `idx_research_global_publication_doi`(`doi` ASC) USING BTREE,
  INDEX `idx_research_global_active`(`is_active` ASC, `has_primary_source` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 15448 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '跨来源全局去重文献；一篇文献只保存一次' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;

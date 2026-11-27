-- 补齐当前接入来源缺失的主要资源类型映射；不修改采集路由、不重跑整理、不删除旧别名。
-- 分类建议基于官网资源对象，不表示每篇文献都同时涉及该库的全部功能。
-- 当前EWAS暂采用DataHub的组学数据集口径；若采集覆盖整个Open Platform，再执行可选脚本。
-- 通过topic_code查ID，不依赖固定数值ID；唯一键(database_code,topic_id)保证重复执行不新增重复映射。
SET SESSION lock_wait_timeout=5;

INSERT INTO research_database_topic
    (database_code, topic_id, evidence, verification_status)
SELECT mappings.database_code, categories.id, mappings.evidence, 'CONFIRMED'
FROM (
    SELECT 'ASCANCERATLAS' AS database_code, 'DATABASE' AS topic_code,
           'ASCancer Atlas的核心对象是癌症可变剪接事件及其人工整理的功能知识；按知识资源归入数据库，不直接等同于基因组变异。https://ngdc.cncb.ac.cn/ascancer/help' AS evidence
    UNION ALL
    SELECT 'GEN', 'OMICS_DATASET',
           'GEN整合标准化bulk、单细胞及空间转录组表达谱和数据集元信息，按核心表达数据对象归入组学数据集。https://ngdc.cncb.ac.cn/gen/documentation'
    UNION ALL
    SELECT 'GWH', 'SEQUENCE',
           'GWH归档基因组组装序列、注释及关联元数据，核心资源对象为组装后的基因组序列。https://ngdc.cncb.ac.cn/gwh/documents'
    UNION ALL
    SELECT 'EWAS', 'OMICS_DATASET',
           'EWAS DataHub整合标准化DNA甲基化数据集及样本元数据；本映射采用数据集范围，不把表观甲基化关联自动归为基因组变异。https://ngdc.cncb.ac.cn/ewas/document'
) AS mappings
JOIN research_topic_category AS categories ON categories.topic_code = mappings.topic_code
WHERE categories.status = 'ENABLED'
ON DUPLICATE KEY UPDATE
    evidence = VALUES(evidence),
    verification_status = VALUES(verification_status),
    update_time = CURRENT_TIMESTAMP;

-- 核对本次四个来源的全部映射。没有其他业务数据写入。
SELECT mappings.database_code, categories.topic_code, categories.topic_name,
       mappings.verification_status, mappings.evidence
FROM research_database_topic AS mappings
JOIN research_topic_category AS categories ON categories.id = mappings.topic_id
WHERE mappings.database_code IN ('ASCANCERATLAS','GEN','GWH','EWAS')
ORDER BY mappings.database_code, categories.sort_order, categories.topic_code;

-- 映射在生成新快照时冻结。无需重新采集或整理；已有/正在生成的快照不会自动变更。

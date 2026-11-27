package com.example.bioplatform.research.statistics.prepared.model;

import lombok.Data;

import java.util.List;

/**
 * 快照查询的修订、来源、关联/资源开关及维度成员过滤条件。
 */
@Data
public class PreparedScope {
    /**
     * revision：整理修订号，用于固定读取的数据版本。
     */
    private long revision;
    /**
     * sources：已注册的来源适配器或本次选择的来源集合。
     */
    private List<String> sources;
    /**
     * scope：统计范围；TOTAL表示总统计，其余为指定来源编码。
     * type：当前分类、维度或唯一值类型，按调用上下文解释。
     * memberKey：规范化维度成员摘要；同一文献同一成员据此去重。
     */
    private String scope = "TOTAL", type, memberKey;
    /**
     * sourceFilter：额外限定的来源列表；为空与空列表由查询契约分别解释。
     */
    private List<String> sourceFilter;
    /**
     * includeRelated：是否将非本库的关联记录纳入当前处理范围。
     * includeResources：是否计入无文献资源；与是否包含关联记录独立。
     */
    private boolean includeRelated, includeResources;
}

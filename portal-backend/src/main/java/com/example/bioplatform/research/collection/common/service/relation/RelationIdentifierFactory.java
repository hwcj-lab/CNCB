package com.example.bioplatform.research.collection.common.service.relation;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 按来源选择唯一值解释器；专用解释器优先，公共关联队列不硬编码各库业务主键。
 */
@Component
public class RelationIdentifierFactory {
    /**
     * resolvers：可供选择的资源或唯一值解析器实现集合。
     */
    private final List<RelationIdentifierResolver> resolvers;

    /**
     * 构造RelationIdentifierFactory，注入本组件所需依赖并建立初始状态。
     * @param resolvers 可供选择的资源或唯一值解析器实现集合
     */
    public RelationIdentifierFactory(List<RelationIdentifierResolver> resolvers) {
        this.resolvers = resolvers.stream().sorted(Comparator.comparingInt(RelationIdentifierResolver::priority)).toList();
    }

    /**
     * 把原始标识解析成规范的类型与值，或建立当前上下文的身份对应关系。
     * @param database 本次处理的数据库来源编码
     * @param raw 上游原始载荷或未规范化参数
     * @return 按上述规则计算或读取的RelationIdentifierResolver.Identifier结果
     */
    public RelationIdentifierResolver.Identifier resolve(String database, Map<String, String> raw) {
        Map<String, String> clean = new HashMap<>();
        raw.forEach((k, v) -> {
            if (k != null && v != null && !v.isBlank()) clean.put(k.trim().toLowerCase(Locale.ROOT), v.trim());
        });
        if (clean.containsKey("sourcekey") && clean.get("sourcekey").startsWith("RESOURCE:"))
            return new RelationIdentifierResolver.Identifier("SOURCE_KEY", clean.get("sourcekey"));
        return resolvers.stream().filter(r -> r.supports(database)).findFirst().orElseThrow(() -> new IllegalArgumentException("未配置来源唯一值解析器：" + database)).resolve(database, clean);
    }
}

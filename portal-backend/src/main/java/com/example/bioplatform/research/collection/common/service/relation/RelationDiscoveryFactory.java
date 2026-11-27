package com.example.bioplatform.research.collection.common.service.relation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 只负责按元数据选发现策略；工厂不包含任何具体数据库解析分支。
 */
@Component
public class RelationDiscoveryFactory {
    /**
     * strategies：按优先级排列的关联发现策略集合。
     */
    private final List<RelationDiscoveryStrategy> strategies;
    /**
     * json：JSON解析与序列化组件。
     */
    private final ObjectMapper json;

    /**
     * 构造RelationDiscoveryFactory，注入本组件所需依赖并建立初始状态。
     * @param strategies 按优先级排列的关联发现策略集合
     * @param json JSON解析与序列化组件
     */
    public RelationDiscoveryFactory(List<RelationDiscoveryStrategy> strategies, ObjectMapper json) {
        this.strategies = strategies.stream().sorted(Comparator.comparingInt(RelationDiscoveryStrategy::priority).thenComparing(RelationDiscoveryStrategy::code)).toList();
        this.json = json;
    }

    /**
     * 依次尝试可处理当前元数据的策略；工厂只选择路径，不在内部写来源表或递归请求。
     */
    public RelationDiscoveryResult discover(RelationDiscoveryInput input, RelationDiscoveryStrategy.BeforeRequest control) throws Exception {
        for (var strategy : strategies) if (strategy.supports(input)) return strategy.discover(input, control);
        return new RelationDiscoveryResult("NONE", Map.of(), List.of(), json.createArrayNode());
    }
}

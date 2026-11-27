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
    private final List<RelationDiscoveryStrategy> strategies;
    private final ObjectMapper json;

    public RelationDiscoveryFactory(List<RelationDiscoveryStrategy> strategies, ObjectMapper json) {
        this.strategies = strategies.stream().sorted(Comparator.comparingInt(RelationDiscoveryStrategy::priority).thenComparing(RelationDiscoveryStrategy::code)).toList();
        this.json = json;
    }

    public RelationDiscoveryResult discover(RelationDiscoveryInput input, RelationDiscoveryStrategy.BeforeRequest control) throws Exception {
        for (var strategy : strategies) if (strategy.supports(input)) return strategy.discover(input, control);
        return new RelationDiscoveryResult("NONE", Map.of(), List.of(), json.createArrayNode());
    }
}

package com.example.bioplatform.research.integration.service.impl;

import com.example.bioplatform.research.collection.common.service.ResearchPublicationSource;
import com.example.bioplatform.research.collection.common.vo.ResearchPublicationSourceOption;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * 自动收集并稳定排序所有文献来源实现，是统计工厂的来源注册中心。
 */
@Component
public class ResearchPublicationSourceRegistry {
    private final List<ResearchPublicationSource> sources;
    private final com.example.bioplatform.research.collection.openlb.mapper.OpenLbRoutingMapper routes;
    private final com.example.bioplatform.research.collection.common.mapper.RoutedPublicationMapper routed;
    private final com.example.bioplatform.research.integration.mapper.ResearchResourceMapper resources;

    public ResearchPublicationSourceRegistry(List<ResearchPublicationSource> sources, com.example.bioplatform.research.collection.openlb.mapper.OpenLbRoutingMapper routes, com.example.bioplatform.research.collection.common.mapper.RoutedPublicationMapper routed) {
        this(sources, routes, routed, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public ResearchPublicationSourceRegistry(List<ResearchPublicationSource> sources, com.example.bioplatform.research.collection.openlb.mapper.OpenLbRoutingMapper routes, com.example.bioplatform.research.collection.common.mapper.RoutedPublicationMapper routed, com.example.bioplatform.research.integration.mapper.ResearchResourceMapper resources) {
        this.resources = resources;
        this.routes = routes;
        this.routed = routed;
        this.sources = sources.stream()
                .sorted(Comparator.comparing(ResearchPublicationSource::sourceCode)).toList();
    }

    /**
     * 返回Spring自动发现的全部来源；返回值不可修改。
     */
    public List<ResearchPublicationSource> all() {
        return all(true);
    }

    public List<ResearchPublicationSource> all(boolean includeResources) {
        java.util.Map<String, ResearchPublicationSource> result = new java.util.TreeMap<>();
        sources.stream().filter(ResearchPublicationSource::enabled).forEach(source -> result.put(source.sourceCode(), source));
        for (var row : routes.routes()) {
            String code = row.get("source_code").toString();
            result.putIfAbsent(code, new RoutedResearchPublicationSource(code, row.get("source_name").toString(), routed));
        }
        return result.values().stream().map(source -> includeResources && resources != null ? (ResearchPublicationSource) new ResourceAwareResearchPublicationSource(source, resources) : source).toList();
    }

    /**
     * 按来源代码获取适配器；穿透查询不会拼接动态表名。
     */
    public ResearchPublicationSource require(String sourceCode) {
        String normalized = sourceCode == null ? "" : sourceCode.trim().toUpperCase(Locale.ROOT);
        return all().stream().filter(source -> source.sourceCode().equalsIgnoreCase(normalized))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("未注册的文献来源: " + normalized));
    }

    /**
     * 返回页面可选来源，默认名称不推测外部数据库，只标识已注册采集源。
     */
    public List<ResearchPublicationSourceOption> options() {
        return all().stream().map(source -> new ResearchPublicationSourceOption(
                source.sourceCode(), source.sourceName())).toList();
    }
}

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
    /**
     * sources：已注册的来源适配器或本次选择的来源集合。
     */
    private final List<ResearchPublicationSource> sources;
    /**
     * routes：来源路由持久层或已加载的路由配置。
     */
    private final com.example.bioplatform.research.collection.openlb.mapper.OpenLbRoutingMapper routes;
    /**
     * routed：配置驱动的通用来源或来源持久层。
     */
    private final com.example.bioplatform.research.collection.common.mapper.RoutedPublicationMapper routed;
    /**
     * resources：资源结果、资源来源或资源缓存，类型决定具体用途。
     */
    private final com.example.bioplatform.research.integration.mapper.ResearchResourceMapper resources;

    /**
     * 构造ResearchPublicationSourceRegistry，注入本组件所需依赖并建立初始状态。
     * @param sources 已注册的来源适配器或本次选择的来源集合
     * @param routes 来源路由持久层或已加载的路由配置
     * @param routed 配置驱动的通用来源或来源持久层
     */
    public ResearchPublicationSourceRegistry(List<ResearchPublicationSource> sources, com.example.bioplatform.research.collection.openlb.mapper.OpenLbRoutingMapper routes, com.example.bioplatform.research.collection.common.mapper.RoutedPublicationMapper routed) {
        this(sources, routes, routed, null);
    }

    /**
     * 构造ResearchPublicationSourceRegistry，注入本组件所需依赖并建立初始状态。
     * @param sources 已注册的来源适配器或本次选择的来源集合
     * @param routes 来源路由持久层或已加载的路由配置
     * @param routed 配置驱动的通用来源或来源持久层
     * @param resources 资源结果、资源来源或资源缓存，类型决定具体用途
     */
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

    /**
     * 取得已注册来源集合；按参数决定是否过滤停用来源及是否包装无文献资源适配器。
     * @param includeResources 是否计入无文献资源；与是否包含关联记录独立
     * @return 按上述规则计算或读取的List<ResearchPublicationSource>结果
     */
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

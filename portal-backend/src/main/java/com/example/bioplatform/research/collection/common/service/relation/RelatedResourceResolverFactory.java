package com.example.bioplatform.research.collection.common.service.relation;

import com.example.bioplatform.research.collection.common.service.RelatedResourceResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * cncbBanks.db到资源页面解析器的独立工厂；新资源类型只新增Resolver组件。
 */
@Component
@RequiredArgsConstructor
public class RelatedResourceResolverFactory {
    /**
     * resolvers：可供选择的资源或唯一值解析器实现集合。
     */
    private final List<RelatedResourceResolver> resolvers;

    /**
     * 从已注册实现中查找能处理输入数据库名称的解析器；找不到时交给调用方决定回退路径。
     * @param name 当前对象或资源的展示名称
     * @return 按上述规则计算或读取的RelatedResourceResolver结果
     */
    public RelatedResourceResolver find(String name) {
        if (name == null) return null;
        return resolvers.stream().filter(r -> r.supportedDatabaseNames().stream().anyMatch(alias -> alias.equalsIgnoreCase(name.trim()))).findFirst().orElse(null);
    }
}

package com.example.bioplatform.research.collection.common.service.relation;
import com.example.bioplatform.research.collection.common.service.RelatedResourceResolver;
import com.example.bioplatform.research.collection.common.service.RelatedResourceResolver;import lombok.RequiredArgsConstructor;import org.springframework.stereotype.Component;import java.util.*;
/** cncbBanks.db到资源页面解析器的独立工厂；新资源类型只新增Resolver组件。 */
@Component @RequiredArgsConstructor public class RelatedResourceResolverFactory {
 private final List<RelatedResourceResolver> resolvers;
 public RelatedResourceResolver find(String name){if(name==null)return null;return resolvers.stream().filter(r->r.supportedDatabaseNames().stream().anyMatch(alias->alias.equalsIgnoreCase(name.trim()))).findFirst().orElse(null);}
}

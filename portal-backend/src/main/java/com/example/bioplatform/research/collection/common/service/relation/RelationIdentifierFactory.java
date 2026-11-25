package com.example.bioplatform.research.collection.common.service.relation;

import org.springframework.stereotype.Component;import java.util.*;
@Component public class RelationIdentifierFactory {
 private final List<RelationIdentifierResolver> resolvers;
 public RelationIdentifierFactory(List<RelationIdentifierResolver> resolvers){this.resolvers=resolvers.stream().sorted(Comparator.comparingInt(RelationIdentifierResolver::priority)).toList();}
 public RelationIdentifierResolver.Identifier resolve(String database,Map<String,String> raw){Map<String,String> clean=new HashMap<>();raw.forEach((k,v)->{if(k!=null&&v!=null&&!v.isBlank())clean.put(k.trim().toLowerCase(Locale.ROOT),v.trim());});
  if(clean.containsKey("sourcekey")&&clean.get("sourcekey").startsWith("RESOURCE:"))return new RelationIdentifierResolver.Identifier("SOURCE_KEY",clean.get("sourcekey"));
  return resolvers.stream().filter(r->r.supports(database)).findFirst().orElseThrow(()->new IllegalArgumentException("未配置来源唯一值解析器："+database)).resolve(database,clean);
 }
}

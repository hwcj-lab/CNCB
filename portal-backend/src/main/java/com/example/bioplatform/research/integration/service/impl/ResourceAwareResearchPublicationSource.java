package com.example.bioplatform.research.integration.service.impl;
import com.example.bioplatform.research.collection.common.dto.SourcePublicationRecord;
import com.example.bioplatform.research.integration.mapper.ResearchResourceMapper;
import com.example.bioplatform.research.collection.common.service.ResearchPublicationSource;
import com.example.bioplatform.research.collection.common.service.ResearchPublicationSource;import com.example.bioplatform.research.integration.mapper.ResearchResourceMapper;import com.example.bioplatform.research.collection.common.dto.SourcePublicationRecord;import java.util.*;
/** 同库文献/无文献资源合并为一条有序分页流；偶数游标为原文献，奇数为资源。 */
final class ResourceAwareResearchPublicationSource implements ResearchPublicationSource {
 private final ResearchPublicationSource papers;private final ResearchResourceMapper resources;
 ResourceAwareResearchPublicationSource(ResearchPublicationSource papers,ResearchResourceMapper resources){this.papers=papers;this.resources=resources;}
 public String sourceCode(){return papers.sourceCode();}public String sourceName(){return papers.sourceName();}public boolean enabled(){return papers.enabled();}
 public List<SourcePublicationRecord> loadRecordsPage(boolean related,long after,int size){
  List<SourcePublicationRecord> result=new ArrayList<>();long base=after/2;
  for(var original:papers.loadRecordsPage(related,base,size)){var row=new SourcePublicationRecord();org.springframework.beans.BeanUtils.copyProperties(original,row);row.setSourceRowId(Math.multiplyExact(row.getSourceRowId(),2));if(row.getSourceRowId()>after)result.add(row);}
  for(var original:resources.page(sourceCode(),related,Math.max(0,base-(after%2==0?1:0)),size)){var row=new SourcePublicationRecord();org.springframework.beans.BeanUtils.copyProperties(original,row);row.setSourceRowId(Math.addExact(Math.multiplyExact(row.getSourceRowId(),2),1));if(row.getSourceRowId()>after)result.add(row);}
  return result.stream().sorted(Comparator.comparing(SourcePublicationRecord::getSourceRowId)).limit(size).toList();
 }
 public List<SourcePublicationRecord> loadRecordsByIdentifiers(List<String> pmids,List<String> dois){return papers.loadRecordsByIdentifiers(pmids,dois);}
 public long countRecords(boolean related){return papers.countRecords(related)+resources.count(sourceCode(),related);}
 public List<Integer> distinctYears(boolean related){Set<Integer> years=new TreeSet<>(papers.distinctYears(related));years.addAll(resources.years(sourceCode(),related));return List.copyOf(years);}
 public List<SourcePublicationRecord> loadRecordsBySourceKeys(List<String> keys){
  var resourceIds=keys.stream().filter(key->key.startsWith("RESOURCE:")).map(key->key.substring(9)).distinct().toList();var paperKeys=keys.stream().filter(key->!key.startsWith("RESOURCE:")).toList();List<SourcePublicationRecord> result=new ArrayList<>();if(!paperKeys.isEmpty())result.addAll(papers.loadRecordsBySourceKeys(paperKeys));if(!resourceIds.isEmpty())result.addAll(resources.keys(sourceCode(),resourceIds));return result;
 }
}

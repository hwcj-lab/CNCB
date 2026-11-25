package com.example.bioplatform.research.integration.service.impl;
import com.example.bioplatform.research.collection.common.dto.IntegratedPublicationRecord;
import com.example.bioplatform.research.collection.common.dto.SourcePublicationRecord;
import com.example.bioplatform.research.collection.gsa.service.impl.GsaPublicationParser;
import java.util.*;
/** 只解析当前有变化批次的标识；新增PMID时保留原整合键和历史穿透引用。 */
final class IntegrationIdentityResolver {
 record Group(String key,List<SourcePublicationRecord> rows,List<IntegratedPublicationRecord> previous){}
 static String doi(String value){return value==null?"":com.example.bioplatform.research.common.PublicationValues.normalizeDoi(value);}
 static String natural(SourcePublicationRecord row){if("RESOURCE".equals(row.getRecordKind()))return row.getSourceCode()+":"+row.getSourceRecordKey();return present(row.getPmid())?"PMID:"+row.getPmid():present(row.getDoi())?"DOI:"+doi(row.getDoi()):present(row.getSourceRecordKey())?row.getSourceCode()+":"+row.getSourceRecordKey():row.getSourceCode()+":UNKEYED:"+com.example.bioplatform.research.common.PublicationValues.sha(row.getTitle()==null?"":row.getTitle());}
 static Set<String> keys(List<SourcePublicationRecord> rows){Set<String> result=new LinkedHashSet<>();for(var row:rows){result.add(natural(row));if(present(row.getSourceRecordKey()))result.add(row.getSourceCode()+":"+row.getSourceRecordKey());if(present(row.getDoi()))result.add("DOI:"+doi(row.getDoi()));}return result;}
 static List<Group> resolve(List<SourcePublicationRecord> rows,List<IntegratedPublicationRecord> existing){
  Map<String,String> doiPmids=new HashMap<>();Set<String> ambiguous=new HashSet<>();
  for(var row:rows)if(present(row.getDoi())&&present(row.getPmid())){String key=doi(row.getDoi()),old=doiPmids.putIfAbsent(key,row.getPmid());if(old!=null&&!old.equals(row.getPmid()))ambiguous.add(key);}
  Map<String,List<SourcePublicationRecord>> groups=new LinkedHashMap<>();
  for(var row:rows){String key=natural(row);if(!present(row.getPmid())&&present(row.getDoi())&&!ambiguous.contains(doi(row.getDoi()))&&doiPmids.containsKey(doi(row.getDoi())))key="PMID:"+doiPmids.get(doi(row.getDoi()));groups.computeIfAbsent(key,k->new ArrayList<>()).add(row);}
  List<Group> result=new ArrayList<>();Set<String> claimed=new HashSet<>();
  for(var group:groups.entrySet()){
   var sourceRows=group.getValue();Set<String> aliases=keys(sourceRows);Set<String> ids=new HashSet<>(),dois=new HashSet<>();sourceRows.forEach(row->{if(present(row.getPmid()))ids.add(row.getPmid());if(present(row.getDoi()))dois.add(doi(row.getDoi()));});
   var matches=existing.stream().filter(old->{
    if(claimed.contains(old.getPublicationKey()))return false;
    if(present(old.getPmid())&&!ids.isEmpty()&&!ids.contains(old.getPmid()))return false;
    return aliases.contains(old.getPublicationKey()) || present(old.getPmid())&&ids.contains(old.getPmid()) || !ambiguous.contains(doi(old.getDoi()))&&present(old.getDoi())&&dois.contains(doi(old.getDoi()));
   }).sorted(Comparator.comparing(IntegratedPublicationRecord::getId)).toList();
   String key=matches.isEmpty()?group.getKey():matches.get(0).getPublicationKey();claimed.add(key);matches.forEach(old->claimed.add(old.getPublicationKey()));result.add(new Group(key,List.copyOf(sourceRows),matches));
  }
  return result;
 }
 private static boolean present(String value){return value!=null&&!value.isBlank();}
}

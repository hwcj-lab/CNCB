package com.example.bioplatform.research.integration.service.impl;

import com.example.bioplatform.research.collection.common.dto.SourcePublicationRecord;
import com.example.bioplatform.research.collection.common.mapper.*;
import com.example.bioplatform.research.collection.common.service.ResearchPublicationSource;
import com.example.bioplatform.research.collection.genbase.service.impl.GenBaseResearchPublicationSource;
import com.example.bioplatform.research.collection.gsa.mapper.GsaSourceMapper;
import com.example.bioplatform.research.collection.gsa.service.impl.GsaResearchPublicationSource;
import com.example.bioplatform.research.collection.bioproject.mapper.BioProjectSourceMapper;
import com.example.bioplatform.research.collection.bioproject.service.impl.BioProjectResearchPublicationSource;
import com.example.bioplatform.research.integration.mapper.ResearchResourceMapper;
import com.example.bioplatform.research.integration.dto.ResearchResourceInput;
import com.example.bioplatform.research.statistics.mapper.ResearchSnapshotMapper;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.*;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class IncrementalSourceReadersTest {
 @Test void everySourceUsesTimeAndIdAndResourcesMergeIntoTheSameOrderedCursor() throws Exception {
  var ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:delta_"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
  try(var connection=ds.getConnection();var statement=connection.createStatement()){
   for(String table:List.of("genbase_publication","gsa_publication","bioproject_publication","biocode_publication")){
    statement.execute("CREATE TABLE "+table+"(id BIGINT PRIMARY KEY,update_time TIMESTAMP(6),pmid VARCHAR(32),doi VARCHAR(255),source_url VARCHAR(1500),title TEXT,pub_year INT,journal_title VARCHAR(500),authors_json TEXT,affiliation_info TEXT,grants_json TEXT,keyword_list TEXT,mesh_headings_json TEXT,cncb_banks_json TEXT,is_primary INT,gsa_record_key VARCHAR(400),bioproject_record_key VARCHAR(400),gsa_accessions TEXT,bioproject_accession TEXT,paper_id BIGINT,paper_title TEXT,publish_year INT,journal_name VARCHAR(500))");
    statement.execute("CREATE INDEX ix_"+table+" ON "+table+"(update_time,id)");
    for(int id=1;id<=4;id++)statement.execute("INSERT INTO "+table+"(id,update_time,pmid,source_url,is_primary,gsa_record_key,bioproject_record_key,paper_id) VALUES("+id+",'2020-01-0"+Math.max(1,id-1)+" 00:00:00','1000000"+id+"','https://example.org/"+id+"',1,'GSA:"+id+"','PROJECT:"+id+"',"+(10000+id)+")");
   }
   statement.execute("CREATE TABLE research_stat_publication(id BIGINT AUTO_INCREMENT PRIMARY KEY,publication_key VARCHAR(320) UNIQUE,record_kind VARCHAR(20),source_codes VARCHAR(500),source_record_keys TEXT,resource_accession VARCHAR(320),resource_url VARCHAR(1500),is_active INT,pmid VARCHAR(32),doi VARCHAR(255),title TEXT,pub_year INT,authors_json TEXT,affiliation_info TEXT,grants_json TEXT,cncb_banks_json TEXT,keyword_list TEXT,raw_payload TEXT,content_hash VARCHAR(64),has_primary_source INT,relation_status VARCHAR(20),update_time TIMESTAMP(6))");
   statement.execute("INSERT INTO research_stat_publication(id,publication_key,record_kind,source_codes,resource_accession,resource_url,is_active,has_primary_source,content_hash,update_time) VALUES(1,'GENBASE:RESOURCE:R1','RESOURCE','GENBASE','R1','https://example.org/R1',1,1,'same','2020-01-01 00:00:00'),(2,'GENBASE:RESOURCE:R2','RESOURCE','GENBASE','R2','https://example.org/R2',0,1,'same','2020-01-01 00:00:00')");
   statement.execute("CREATE TABLE research_relation_crawl_queue(id BIGINT AUTO_INCREMENT PRIMARY KEY,job_id BIGINT,identifier_type VARCHAR(20),identifier_value VARCHAR(400),depth INT,status VARCHAR(20),attempt_count INT DEFAULT 0,last_error TEXT,update_time TIMESTAMP,UNIQUE(job_id,identifier_type,identifier_value))");
  }
  var cfg=new Configuration();cfg.setMapUnderscoreToCamelCase(true);cfg.setArgNameBasedConstructorAutoMapping(true);cfg.setEnvironment(new Environment("test",new JdbcTransactionFactory(),ds));
  for(Class<?> type:List.of(GsaSourceMapper.class,BioProjectSourceMapper.class,RoutedPublicationMapper.class,ResearchSnapshotMapper.class,ResearchResourceMapper.class,PublicationRelationCollectionMapper.class))cfg.addMapper(type);
  for(String name:List.of("GsaSourceMapper","BioProjectSourceMapper","RoutedPublicationMapper","ResearchSnapshotMapper","ResearchResourceMapper","PublicationRelationCollectionMapper")){
   String xml;try(var input=getClass().getResourceAsStream("/mapper/research/"+name+".xml")){xml=new String(Objects.requireNonNull(input).readAllBytes(),StandardCharsets.UTF_8);}
   xml=xml.replace("&lt;=&gt;"," IS NOT DISTINCT FROM ");
   new XMLMapperBuilder(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)),cfg,name,cfg.getSqlFragments()).parse();
  }
  try(var session=new SqlSessionFactoryBuilder().build(cfg).openSession(true)){
   var papers=new GenBaseResearchPublicationSource(session.getMapper(ResearchSnapshotMapper.class));
   List<ResearchPublicationSource> sources=List.of(papers,new GsaResearchPublicationSource(session.getMapper(GsaSourceMapper.class),null),new BioProjectResearchPublicationSource(session.getMapper(BioProjectSourceMapper.class),null),new RoutedResearchPublicationSource("BIOCODE","BioCode",session.getMapper(RoutedPublicationMapper.class)));
   var time=LocalDateTime.of(2020,1,1,0,0);var until=time.plusDays(2);
   for(var source:sources){
    assertTrue(source.supportsIncremental());
    var page=source.loadChangedRecordsPage(true,time,1,until,10);
    assertEquals(List.of(2L,3L),page.stream().map(SourcePublicationRecord::getSourceRowId).toList(),source.sourceCode());
    assertEquals(time,page.get(0).getSourceUpdatedTime());
    assertTrue(source.loadChangedRecordsPage(true,page.get(1).getSourceUpdatedTime(),3,until,10).isEmpty());
   }
   var resources=session.getMapper(ResearchResourceMapper.class);
   var combined=new ResourceAwareResearchPublicationSource(papers,resources);
   List<Long> ids=new ArrayList<>();List<Boolean> active=new ArrayList<>();long after=0;var afterTime=time;
   while(true){var rows=combined.loadChangedRecordsPage(true,afterTime,after,until,1);if(rows.isEmpty())break;var row=rows.get(0);ids.add(row.getSourceRowId());active.add(row.getRecordActive());after=row.getSourceRowId();afterTime=row.getSourceUpdatedTime();}
   assertEquals(List.of(2L,3L,4L,5L,6L),ids);assertFalse(active.get(3));
   // Reactivating unchanged resource content must advance update_time as well as is_active.
   var input=new ResearchResourceInput();input.setDatabaseCode("GENBASE");input.setAccession("R2");input.setSourceUrl("https://example.org/R2");
   resources.upsert(input,"same","{}");
   try(var statement=session.getConnection().createStatement();var result=statement.executeQuery("SELECT is_active,update_time FROM research_stat_publication WHERE id=2")){assertTrue(result.next());assertEquals(1,result.getInt(1));assertTrue(result.getTimestamp(2).toLocalDateTime().isAfter(until));}
   // A discovered leaf is visited but not scheduled. Old recursive queue nodes are retired on resume.
   var queue=session.getMapper(PublicationRelationCollectionMapper.class);queue.insertRootQueue(9L,"PMID","10000001");queue.insertTerminalQueue(9L,"PMID","10000002",1);
   assertEquals(1,queue.countPendingQueue(9L));assertEquals("10000001",queue.findNextPending(9L).identifierValue());
   queue.insertQueueIgnore(9L,"PMID","10000003",2);assertEquals(1,queue.finishDescendantQueue(9L));assertEquals(1,queue.countPendingQueue(9L));
   var root=queue.findNextPending(9L);queue.failQueue(root.id(),"fixture");assertEquals(1,queue.retryFailedQueue(9L));assertEquals(root.id(),queue.findNextPending(9L).id());
  }
 }
}

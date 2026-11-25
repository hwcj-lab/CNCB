package com.example.bioplatform.research.statistics.service.impl;
import com.example.bioplatform.research.collection.genbase.service.impl.GenBaseEnrichmentPersistenceTest;
import com.example.bioplatform.research.integration.dto.ResearchResourceInput;
import com.example.bioplatform.research.statistics.dto.ResearchSnapshotBuildRequest;
import com.example.bioplatform.research.collection.common.dto.SourcePublicationRecord;
import com.example.bioplatform.research.collection.openlb.mapper.OpenLbRoutingMapper;
import com.example.bioplatform.research.integration.mapper.ResearchResourceMapper;
import com.example.bioplatform.research.statistics.mapper.ResearchSnapshotMapper;
import com.example.bioplatform.research.collection.common.service.ResearchPublicationSource;
import com.example.bioplatform.research.statistics.vo.ResearchSnapshotDetail;
import com.example.bioplatform.research.integration.service.impl.ResearchPublicationIntegrationServiceImpl;
import com.example.bioplatform.research.integration.service.impl.ResearchPublicationSourceRegistry;
import com.example.bioplatform.research.integration.service.impl.ResearchResourceService;
import com.example.bioplatform.research.statistics.service.impl.ResearchSnapshotServiceImpl;
import com.example.bioplatform.research.collection.common.service.ResearchPublicationSource;import com.fasterxml.jackson.databind.ObjectMapper;import org.apache.ibatis.builder.xml.XMLMapperBuilder;import org.apache.ibatis.mapping.Environment;import org.apache.ibatis.session.*;import org.mybatis.spring.transaction.SpringManagedTransactionFactory;import org.h2.jdbcx.JdbcDataSource;import org.junit.jupiter.api.Test;import org.mybatis.spring.SqlSessionTemplate;import org.springframework.jdbc.datasource.DataSourceTransactionManager;import org.springframework.transaction.support.TransactionTemplate;import java.io.*;import java.nio.charset.StandardCharsets;import java.util.*;import static org.junit.jupiter.api.Assertions.*;
class ResourceMixedStatisticsTest {
 static class Source implements ResearchPublicationSource {
  final String code;final List<SourcePublicationRecord> rows=new ArrayList<>();Source(String code){this.code=code;}
  public String sourceCode(){return code;}public List<SourcePublicationRecord> loadRecordsPage(boolean related,long after,int size){return rows.stream().filter(r->r.getSourceRowId()>after&&(related||Boolean.TRUE.equals(r.getPrimaryFlag()))).limit(size).toList();}
  public List<SourcePublicationRecord> loadRecordsByIdentifiers(List<String> pmids,List<String> dois){return rows.stream().filter(r->pmids.contains(r.getPmid())||dois.contains(r.getDoi())).toList();}
  public long countRecords(boolean related){return rows.stream().filter(r->related||Boolean.TRUE.equals(r.getPrimaryFlag())).count();}
  public List<Integer> distinctYears(boolean related){return rows.stream().filter(r->related||Boolean.TRUE.equals(r.getPrimaryFlag())).map(SourcePublicationRecord::getPubYear).filter(Objects::nonNull).distinct().toList();}
  public List<SourcePublicationRecord> loadRecordsBySourceKeys(List<String> keys){return rows.stream().filter(r->keys.contains(r.getSourceRecordKey())).toList();}
  void paper(long id,String pmid,boolean primary){var r=new SourcePublicationRecord();r.setSourceRowId(id);r.setSourceCode(code);r.setSourceRecordKey("PMID:"+pmid);r.setPmid(pmid);r.setTitle("Paper "+pmid);r.setPubYear(2026);r.setPrimaryFlag(primary);rows.add(r);}
 }
 static class Fixture implements AutoCloseable {
  final JdbcDataSource ds=new JdbcDataSource();final ObjectMapper json=new ObjectMapper();final ResearchSnapshotMapper snapshots;final ResearchResourceMapper resources;final ResearchPublicationSourceRegistry registry;final ResearchPublicationIntegrationServiceImpl integration;final ResearchResourceService service;final ResearchSnapshotServiceImpl builder;final TransactionTemplate tx;
  Fixture(List<Source> sources)throws Exception{
   ds.setURL("jdbc:h2:mem:mixed_"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");String schema=new String(getClass().getResourceAsStream("/db/schema.sql").readAllBytes(),StandardCharsets.UTF_8);
   try(var c=ds.getConnection();var st=c.createStatement()){
    for(String table:List.of("research_fund_dictionary","research_topic_category","research_database_topic","research_stat_snapshot","research_stat_publication","research_stat_dimension","research_stat_dimension_publication","research_stat_dimension_candidate")){
     int start=schema.indexOf("CREATE TABLE IF NOT EXISTS "+table+" (");assertTrue(start>=0,table);String ddl=schema.substring(start,schema.indexOf(';',start)).replace("dimension_key(191)","dimension_key").replace(" CHARACTER SET utf8mb4 COLLATE utf8mb4_bin","").replace("source_codes(40)","source_codes");st.execute(ddl);
    }
   }
   var cfg=new Configuration();cfg.setMapUnderscoreToCamelCase(true);cfg.setEnvironment(new Environment("test",new SpringManagedTransactionFactory(),ds));cfg.addMapper(ResearchSnapshotMapper.class);cfg.addMapper(ResearchResourceMapper.class);
   for(String name:List.of("ResearchSnapshotMapper","ResearchResourceMapper")){
    String xml=new String(getClass().getResourceAsStream("/mapper/research/"+name+".xml").readAllBytes(),StandardCharsets.UTF_8);xml=GenBaseEnrichmentPersistenceTest.mysqlIfToCase(xml.replace("&lt;=&gt;"," IS NOT DISTINCT FROM ")).replace("SET d.publication_count=","SET publication_count=");
    // 同一维度的键本身已排序；H2无MySQL FIELD函数，测试按等价键序读取。
    xml=xml.replaceAll("(?s)ORDER BY FIELD\\(publication_key,\\s*<foreach.*?</foreach>\\s*\\)","ORDER BY publication_key");
    new XMLMapperBuilder(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)),cfg,"mapper/"+name+".xml",cfg.getSqlFragments()).parse();
   }
   var template=new SqlSessionTemplate(new SqlSessionFactoryBuilder().build(cfg));snapshots=template.getMapper(ResearchSnapshotMapper.class);resources=template.getMapper(ResearchResourceMapper.class);
   OpenLbRoutingMapper routes=(OpenLbRoutingMapper)java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{OpenLbRoutingMapper.class},(p,m,a)->List.of());registry=new ResearchPublicationSourceRegistry(new ArrayList<>(sources),routes,null,resources);
   tx=new TransactionTemplate(new DataSourceTransactionManager(ds));integration=new ResearchPublicationIntegrationServiceImpl(snapshots,registry,json,tx);service=new ResearchResourceService(resources,registry,json);builder=new ResearchSnapshotServiceImpl(registry,snapshots,integration,json);
  }
  void observe(ResearchResourceInput row){tx.executeWithoutResult(t->service.observe(List.of(row)));}
  ResearchSnapshotDetail snapshot(boolean includeResources,boolean includeRelated)throws Exception{
   var input=new ResearchSnapshotBuildRequest();input.setName("test");input.setIncludeResources(includeResources);input.setIncludeRelated(includeRelated);long id=builder.startBuild(input).current().getId();long until=System.currentTimeMillis()+15000;
   ResearchSnapshotDetail detail;do{detail=snapshots.selectDetail(id);if(!"BUILDING".equals(detail.getStatus()))break;Thread.sleep(20);}while(System.currentTimeMillis()<until);
   assertEquals("SUCCESS",detail.getStatus(),detail.getErrorMessage());return detail;
  }
  public void close(){builder.shutdown();}
 }
 private ResearchResourceInput resource(String source,String accession,boolean primary){var r=new ResearchResourceInput();r.setDatabaseCode(source);r.setAccession(accession);r.setHasPublication(false);r.setPrimaryRecord(primary);r.setSourceUrl("https://example.org/resources/"+accession);r.setReleaseYear(2025);r.setRawPayload("{\"publications\":[]}");return r;}
 @Test void directResourcesKeepIdentityRelationStateAndSurvivePaperRebuild()throws Exception {
  var source=new Source("GENBASE");source.paper(1,"11111111",true);
  try(var f=new Fixture(List.of(source))){
   f.integration.syncSourceKeys("GENBASE",List.of("PMID:11111111"));f.observe(resource("GENBASE","R1",true));
   long id=f.resources.keys("GENBASE",List.of("R1")).get(0).getSourceRowId();
   assertEquals(1,f.resources.pending("GENBASE",false,20).size());
   f.resources.relationStatus("GENBASE","R1","COMPLETED",null);f.observe(resource("GENBASE","R1",true));assertTrue(f.resources.pending("GENBASE",false,20).isEmpty());
   assertEquals("COMPLETED",f.resources.relationSource("GENBASE","R1").relationStatus());
   org.springframework.test.util.ReflectionTestUtils.invokeMethod(f.integration,"rebuildAll");
   assertEquals(1,f.resources.count("GENBASE",false));assertEquals(id,f.resources.keys("GENBASE",List.of("R1")).get(0).getSourceRowId());
   assertEquals(2,f.snapshot(true,false).getRecordCount());
   try(var c=f.ds.getConnection();var st=c.createStatement();var r=st.executeQuery("SELECT COUNT(*) FROM information_schema.tables WHERE table_name='research_resource_record'")){r.next();assertEquals(0,r.getInt(1));}
  }
 }
 @Test void allFourModesRespectNativeAndExternalRelatedFlagsAndTypedDrilldown()throws Exception {
  var source=new Source("GENBASE");source.paper(1,"11111111",true);source.paper(2,"22222222",false);source.paper(3,"33333333",true);
  try(var f=new Fixture(List.of(source))){f.integration.syncSourceKeys("GENBASE",List.of("PMID:11111111","PMID:22222222","PMID:33333333"));f.observe(resource("GENBASE","R1",true));f.observe(resource("GENBASE","R2",false));f.observe(resource("GENBASE","R1",true));
   assertEquals(2,f.registry.all(false).get(0).countRecords(false));assertEquals(3,f.registry.all(true).get(0).countRecords(false));
   assertEquals(2,f.snapshot(false,false).getRecordCount());assertEquals(3,f.snapshot(false,true).getRecordCount());var mixed=f.snapshot(true,false);assertEquals(3,mixed.getRecordCount());assertEquals(1,mixed.getResourceCount());assertEquals(2,mixed.getLiteratureCount());assertEquals(5,f.snapshot(true,true).getRecordCount());
   var dimension=f.snapshots.selectDimensions(mixed.getId(),"GENBASE","YEAR",50).stream().filter(d->d.getStatYear()==2025).findFirst().orElseThrow();var page=f.builder.dimensionPublications(mixed.getId(),dimension.getId(),1,10);assertEquals(1,page.total());assertEquals("RESOURCE",page.items().get(0).getRecordKind());assertEquals("R1",page.items().get(0).getResourceAccession());assertEquals("https://example.org/resources/R1",page.items().get(0).getSourceUrl());
   List<Long> cursors=new ArrayList<>();List<String> keys=new ArrayList<>();long after=0;var wrapped=f.registry.require("GENBASE");for(int i=0;i<10;i++){var batch=wrapped.loadRecordsPage(true,after,1);if(batch.isEmpty())break;assertTrue(batch.get(0).getSourceRowId()>after);after=batch.get(0).getSourceRowId();cursors.add(after);keys.add(batch.get(0).getSourceRecordKey());}assertEquals(5,keys.size());assertEquals(5,new HashSet<>(keys).size());
   source.paper(4,"44444444",true);f.integration.syncSourceKeys("GENBASE",List.of("PMID:44444444"));var promoted=resource("GENBASE","R1",true);promoted.setHasPublication(true);f.observe(promoted);assertEquals(0,f.resources.count("GENBASE",false));assertEquals(3,f.snapshot(true,false).getRecordCount());assertEquals(1,f.resources.keys("GENBASE",List.of("R1")).size());assertFalse(f.resources.keys("GENBASE",List.of("R1")).get(0).getRecordActive());
  }
 }
 @Test void sameNativeNumberInDifferentDatabasesIsIndependentAndHasArticleNeverCreatesResource()throws Exception {
  try(var f=new Fixture(List.of(new Source("GENBASE"),new Source("GSA")))){f.observe(resource("GENBASE","SAME",true));f.observe(resource("GSA","SAME",true));assertEquals(2,f.snapshots.countIntegratedRecordsForMode(false,true));assertEquals(0,f.snapshots.countIntegratedRecordsForMode(false,false));var withPaper=resource("GSA","HAS_PAPER",true);withPaper.setHasPublication(true);f.observe(withPaper);assertTrue(f.resources.keys("GSA",List.of("HAS_PAPER")).isEmpty());}
 }
}

package com.example.bioplatform.research.integration.service.impl;
import com.example.bioplatform.research.collection.genbase.service.impl.GenBaseEnrichmentPersistenceTest;
import com.example.bioplatform.research.collection.openlb.mapper.OpenLbRoutingMapper;
import com.example.bioplatform.research.statistics.mapper.ResearchSnapshotMapper;
import com.example.bioplatform.research.collection.genbase.service.impl.GenBaseResearchPublicationSource;
import com.example.bioplatform.research.integration.service.impl.ResearchPublicationIntegrationServiceImpl;
import com.example.bioplatform.research.integration.service.impl.ResearchPublicationSourceRegistry;
import org.apache.ibatis.session.*;import org.apache.ibatis.mapping.Environment;import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;import org.apache.ibatis.builder.xml.XMLMapperBuilder;import org.h2.jdbcx.JdbcDataSource;import org.junit.jupiter.api.Test;import org.springframework.transaction.support.TransactionTemplate;import org.springframework.jdbc.datasource.DataSourceTransactionManager;import com.fasterxml.jackson.databind.ObjectMapper;import java.io.*;import java.nio.charset.StandardCharsets;import java.util.*;import static org.junit.jupiter.api.Assertions.*;
class IntegrationIdentityPersistenceTest {
 @Test void resolvedPmidUpdatesSameIntegratedRowAndKeepsHistoricalDuplicateKeysReadable()throws Exception {
  var ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:integration_"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
  String schema=new String(getClass().getResourceAsStream("/db/schema.sql").readAllBytes(),StandardCharsets.UTF_8);
  try(var c=ds.getConnection();var st=c.createStatement()){
   for(String name:List.of("genbase_publication","research_stat_publication")){int a=schema.indexOf("CREATE TABLE IF NOT EXISTS "+name+" (");st.execute(schema.substring(a,schema.indexOf(';',a)));}
   st.execute("INSERT INTO genbase_publication(paper_id,paper_title,source_url,raw_payload,is_primary,source_hash) VALUES(11,'Current paper','https://ngdc.cncb.ac.cn/genbase/statistics/supportpaper','{}',1,'initial')");
  }
  var cfg=new Configuration();cfg.setMapUnderscoreToCamelCase(true);cfg.setEnvironment(new Environment("test",new JdbcTransactionFactory(),ds));cfg.addMapper(ResearchSnapshotMapper.class);
  String xml=new String(getClass().getResourceAsStream("/mapper/research/ResearchSnapshotMapper.xml").readAllBytes(),StandardCharsets.UTF_8);xml=GenBaseEnrichmentPersistenceTest.mysqlIfToCase(xml.replace("&lt;=&gt;"," IS NOT DISTINCT FROM "));
  new XMLMapperBuilder(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)),cfg,"mapper/ResearchSnapshotMapper.xml",cfg.getSqlFragments()).parse();
  try(var session=new SqlSessionFactoryBuilder().build(cfg).openSession(true)){
   var mapper=session.getMapper(ResearchSnapshotMapper.class);
   OpenLbRoutingMapper routes=(OpenLbRoutingMapper)java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{OpenLbRoutingMapper.class},(proxy,method,args)->List.of());
   var registry=new ResearchPublicationSourceRegistry(List.of(new GenBaseResearchPublicationSource(mapper)),routes,null);
   var integration=new ResearchPublicationIntegrationServiceImpl(mapper,registry,new ObjectMapper(),new TransactionTemplate(new DataSourceTransactionManager(ds)));
   integration.syncGenBasePaperIds(List.of(11L));
   Long original;try(var st=session.getConnection().createStatement();var rs=st.executeQuery("SELECT id,publication_key FROM research_stat_publication")){assertTrue(rs.next());original=rs.getLong(1);assertEquals("GENBASE:PAPER:11",rs.getString(2));assertFalse(rs.next());}
   try(var st=session.getConnection().createStatement()){st.execute("UPDATE genbase_publication SET pmid='41769996',doi='10.1234/current',authors_json='[]' WHERE paper_id=11");}
   integration.syncGenBasePaperIds(List.of(11L));integration.syncGenBasePaperIds(List.of(11L));
   try(var st=session.getConnection().createStatement();var rs=st.executeQuery("SELECT id,publication_key,pmid FROM research_stat_publication WHERE is_active=1")){assertTrue(rs.next());assertEquals(original.longValue(),rs.getLong(1));assertEquals("GENBASE:PAPER:11",rs.getString(2));assertEquals("41769996",rs.getString(3));assertFalse(rs.next());}
   try(var st=session.getConnection().createStatement()){st.execute("INSERT INTO research_stat_publication(publication_key,content_hash,pmid,doi,source_codes,source_record_keys,is_active) VALUES('PMID:41769996','legacy','41769996','10.1234/current','GENBASE','{}',1)");}
   integration.syncGenBasePaperIds(List.of(11L));
   try(var st=session.getConnection().createStatement();var rs=st.executeQuery("SELECT COUNT(*),SUM(is_active) FROM research_stat_publication")){assertTrue(rs.next());assertEquals(2,rs.getLong(1));assertEquals(1,rs.getLong(2));}
   assertEquals(1,mapper.selectIntegratedIdentityMatches(List.of("PMID:41769996"),List.of(),List.of()).size());
  }
 }
}

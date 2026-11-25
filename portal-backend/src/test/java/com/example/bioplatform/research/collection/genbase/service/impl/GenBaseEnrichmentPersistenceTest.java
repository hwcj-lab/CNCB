package com.example.bioplatform.research.collection.genbase.service.impl;
import com.example.bioplatform.research.collection.genbase.dto.GenBaseOpenLbWriteRow;
import com.example.bioplatform.research.collection.genbase.dto.GenBasePublicationWriteRow;
import com.example.bioplatform.research.collection.genbase.mapper.GenBaseCollectionMapper;
import com.example.bioplatform.research.collection.genbase.mapper.GenBaseCollectionMapper;import org.apache.ibatis.session.*;import org.apache.ibatis.mapping.Environment;import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;import org.apache.ibatis.builder.xml.XMLMapperBuilder;import org.h2.jdbcx.JdbcDataSource;import org.junit.jupiter.api.Test;import java.nio.charset.StandardCharsets;import java.io.*;import java.util.*;import static org.junit.jupiter.api.Assertions.*;
public class GenBaseEnrichmentPersistenceTest {
 @Test void nativeNoPmidRemainsMissingWhileMetadataAndSourceProvidedIdentifiersArePreserved()throws Exception {
  var ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:genbase_"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
  String schema=new String(getClass().getResourceAsStream("/db/schema.sql").readAllBytes(),StandardCharsets.UTF_8);int start=schema.indexOf("CREATE TABLE IF NOT EXISTS genbase_publication");String ddl=schema.substring(start,schema.indexOf(';',start));
  try(var c=ds.getConnection();var st=c.createStatement()){st.execute(ddl);}
  var cfg=new Configuration();cfg.setMapUnderscoreToCamelCase(true);cfg.setEnvironment(new Environment("test",new JdbcTransactionFactory(),ds));cfg.addMapper(GenBaseCollectionMapper.class);
  String xml=new String(getClass().getResourceAsStream("/mapper/research/GenBaseCollectionMapper.xml").readAllBytes(),StandardCharsets.UTF_8);xml=mysqlIfToCase(xml.replace("&lt;=&gt;"," IS NOT DISTINCT FROM "));
  new XMLMapperBuilder(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)),cfg,"mapper/GenBaseCollectionMapper.xml",cfg.getSqlFragments()).parse();
  try(var session=new SqlSessionFactoryBuilder().build(cfg).openSession(true)){
   var mapper=session.getMapper(GenBaseCollectionMapper.class);
   var seed=new GenBasePublicationWriteRow(11L,null,null,"PRJCA039050","Journal","Current paper",2026,"C_AA001.1","https://ngdc.cncb.ac.cn/genbase/statistics/supportpaper","{\"pubmedId\":null}","a".repeat(64));
   mapper.insertPublicationsIgnore(List.of(seed));var tasks=mapper.findPendingEnrichment(100);assertEquals(1,tasks.size());assertNull(tasks.get(0).getPmid());assertEquals("PRJCA039050",tasks.get(0).getBioprojectAccession());
   var write=new GenBaseOpenLbWriteRow();write.setPaperId(11L);write.setPmid("41769996");write.setDoi("10.1234/current");write.setAuthorsJson("[{\"firstName\":\"Wei\",\"lastName\":\"Li\",\"affiliation\":\"University A\"}]");write.setGrantsJson("[{\"agency\":\"Agency A\"}]");write.setCncbBanksJson("[]");write.setStatus("NO_PMID");write.setRawPayload("{\"identifierResolution\":{\"method\":\"NO_SOURCE_PMID\"}}");mapper.updateEnrichmentSuccess(List.of(write));
   assertTrue(mapper.findPendingEnrichment(100).isEmpty());var stored=mapper.lockEnrichment(List.of(11L)).get(0);assertNull(stored.getPmid());assertNull(stored.getDoi());assertTrue(stored.getAuthorsJson().contains("University A"));
   try(var st=session.getConnection().createStatement()){st.execute("UPDATE genbase_publication SET relation_status='COMPLETED' WHERE paper_id=11");}
   mapper.updateEnrichmentSuccess(List.of(write));try(var st=session.getConnection().createStatement();var rs=st.executeQuery("SELECT relation_status FROM genbase_publication WHERE paper_id=11")){assertTrue(rs.next());assertEquals("COMPLETED",rs.getString(1));}
   mapper.insertPublicationsIgnore(List.of(seed));assertTrue(mapper.findPendingEnrichment(100).isEmpty());assertNull(mapper.lockEnrichment(List.of(11L)).get(0).getPmid());
   var changed=new GenBasePublicationWriteRow(11L,"41769996","10.1234/current","PRJCA039050","Journal","Updated title",2026,"C_AA001.1",seed.sourceUrl(),seed.rawPayload(),"b".repeat(64));mapper.insertPublicationsIgnore(List.of(changed));assertEquals(1,mapper.findPendingEnrichment(100).size());assertEquals("41769996",mapper.findPendingEnrichment(100).get(0).getPmid());
   write.setPaperId(11L);write.setStatus("NO_PMID");mapper.updateEnrichmentSuccess(List.of(write));assertTrue(mapper.findPendingEnrichment(100).isEmpty());assertEquals(1,mapper.resetPmidEnrichment());assertEquals(1,mapper.findPendingEnrichment(100).size());
   mapper.markEnrichmentFailedOrPending(List.of(new GenBaseCollectionMapper.EnrichmentFailure(11L,"temporary")),3);assertEquals(1,mapper.findPendingEnrichment(100).size());
   mapper.markEnrichmentFailedOrPending(List.of(new GenBaseCollectionMapper.EnrichmentFailure(11L,"temporary")),3);assertEquals(1,mapper.findPendingEnrichment(100).size());
   mapper.markEnrichmentFailedOrPending(List.of(new GenBaseCollectionMapper.EnrichmentFailure(11L,"temporary")),3);assertTrue(mapper.findPendingEnrichment(100).isEmpty());
  }
 }
 /** MySQL IF与标准CASE语义等价；仅测试H2需要转换，不修改生产Mapper。 */
 public static String mysqlIfToCase(String input){
  int at=input.indexOf("IF(");if(at<0)return input;int depth=1;boolean quoted=false;List<String> parts=new ArrayList<>();int part=at+3,end=part;
  for(;end<input.length();end++){char ch=input.charAt(end);if(ch=='\''){if(quoted&&end+1<input.length()&&input.charAt(end+1)=='\''){end++;continue;}quoted=!quoted;}if(quoted)continue;if(ch=='(')depth++;if(ch==')')depth--;if(ch==','&&depth==1){parts.add(input.substring(part,end));part=end+1;}if(depth==0){parts.add(input.substring(part,end));break;}}
  if(parts.size()!=3)throw new IllegalArgumentException("Cannot convert IF");return input.substring(0,at)+"(CASE WHEN "+mysqlIfToCase(parts.get(0))+" THEN "+mysqlIfToCase(parts.get(1))+" ELSE "+mysqlIfToCase(parts.get(2))+" END)"+mysqlIfToCase(input.substring(end+1));
 }
}

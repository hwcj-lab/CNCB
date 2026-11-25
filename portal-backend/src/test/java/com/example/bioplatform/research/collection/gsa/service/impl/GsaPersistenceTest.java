package com.example.bioplatform.research.collection.gsa.service.impl;
import com.example.bioplatform.research.collection.gsa.mapper.GsaCollectionMapper;
import com.example.bioplatform.research.collection.gsa.mapper.GsaSourceMapper;
import com.example.bioplatform.research.collection.gsa.service.impl.GsaMetadataClient;
import com.example.bioplatform.research.collection.gsa.service.impl.GsaPublicationParser;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.session.*;import org.apache.ibatis.mapping.Environment;import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.h2.jdbcx.JdbcDataSource;import org.junit.jupiter.api.Test;
import java.io.*;import java.nio.charset.StandardCharsets;import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** 内存库验证实际批量Mapper的UPSERT、补全状态和稳定业务键；不修改生产表。 */
class GsaPersistenceTest {
 @Test void unchangedSeedsPreserveMetadataAndDoiPapersCanBeDrilledThrough()throws Exception {
  JdbcDataSource ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:gsa_"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
  try(var c=ds.getConnection();var st=c.createStatement()){
   st.execute("CREATE TABLE gsa_publication(id BIGINT AUTO_INCREMENT PRIMARY KEY,gsa_record_key VARCHAR(400) UNIQUE,pmid VARCHAR(32) UNIQUE,doi VARCHAR(255),title TEXT,pub_year INT,journal_title VARCHAR(500),source_url VARCHAR(1500),grants_json TEXT,cncb_banks_json TEXT,raw_payload TEXT,gsa_raw_payload TEXT,gsa_source_hash CHAR(64),source_hash CHAR(64),gsa_accessions TEXT,bioproject_accession TEXT,is_primary INT DEFAULT 1,is_related INT DEFAULT 0,relation_status VARCHAR(20) DEFAULT 'PENDING',gsa_enrichment_status VARCHAR(20) DEFAULT 'PENDING',gsa_enrichment_error VARCHAR(1800),gsa_enrichment_attempts INT DEFAULT 0,gsa_metadata_checked_at TIMESTAMP,authors_json TEXT,affiliation_info TEXT,mesh_headings_json TEXT,keyword_list TEXT,abstract_text TEXT)");
  }
  Configuration cfg=new Configuration();cfg.setMapUnderscoreToCamelCase(true);cfg.setEnvironment(new Environment("test",new JdbcTransactionFactory(),ds));
  cfg.addMapper(GsaCollectionMapper.class);cfg.addMapper(GsaSourceMapper.class);
  String xml=new String(Objects.requireNonNull(getClass().getResourceAsStream("/mapper/research/GsaCollectionMapper.xml")).readAllBytes(),StandardCharsets.UTF_8);
  // H2使用SQL标准的等价空值比较；生产Mapper仍保留MySQL <=>。
  xml=xml.replace("&lt;=&gt;"," IS NOT DISTINCT FROM ");
  new XMLMapperBuilder(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)),cfg,"mapper/GsaCollectionMapper.xml",cfg.getSqlFragments()).parse();
  try(var in=getClass().getResourceAsStream("/mapper/research/GsaSourceMapper.xml")){new XMLMapperBuilder(in,cfg,"mapper/GsaSourceMapper.xml",cfg.getSqlFragments()).parse();}
  try(SqlSession session=new SqlSessionFactoryBuilder().build(cfg).openSession(true)){
   var mapper=session.getMapper(GsaCollectionMapper.class);var source=session.getMapper(GsaSourceMapper.class);var json=new ObjectMapper();
   var seed=GsaPublicationParser.parse(json.readTree("{\"articleTitle\":\"A paper\",\"doi\":\"10.1234/abc\",\"craAccession\":\"HRA005096\"}"),json);
   mapper.upsertSeeds(List.of(seed));mapper.upsertSeeds(List.of(seed));assertEquals(1,source.count(false));
   var task=mapper.pending(100).get(0);assertEquals("DOI:10.1234/abc",task.getRecordKey());
   var write=GsaMetadataClient.fromTask(task);write.setAuthorsJson("[{\"firstName\":\"Wei\",\"lastName\":\"Li\",\"affiliation\":\"University A\"}]");write.setGrantsJson("[{\"agency\":\"NSFC\"}]");write.setStatus("SUCCESS");write.setSourceHash("c".repeat(64));
   mapper.updateMetadata(List.of(write));assertTrue(mapper.pending(100).isEmpty());
   mapper.upsertSeeds(List.of(seed));assertTrue(mapper.pending(100).isEmpty());
   var paper=source.keys(List.of("DOI:10.1234/abc")).get(0);assertTrue(paper.getAuthorsJson().contains("University A"));assertTrue(paper.getGrantsJson().contains("NSFC"));
   var changed=GsaPublicationParser.parse(json.readTree("{\"articleTitle\":\"Updated title\",\"doi\":\"10.1234/abc\"}"),json);
   mapper.upsertSeeds(List.of(changed));assertEquals(1,mapper.pending(100).size());assertEquals(1,source.count(false));
   assertEquals("Updated title",source.keys(List.of("DOI:10.1234/abc")).get(0).getTitle());
  }
 }
}

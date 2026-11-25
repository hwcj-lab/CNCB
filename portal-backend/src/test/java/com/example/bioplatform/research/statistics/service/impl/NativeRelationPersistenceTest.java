package com.example.bioplatform.research.statistics.service.impl;
import com.example.bioplatform.research.collection.common.mapper.PublicationRelationCollectionMapper;
import com.example.bioplatform.research.collection.common.mapper.PublicationRelationCollectionMapper;import org.apache.ibatis.session.*;import org.apache.ibatis.mapping.Environment;import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;import org.apache.ibatis.builder.xml.XMLMapperBuilder;import org.h2.jdbcx.JdbcDataSource;import org.junit.jupiter.api.Test;import java.util.*;import static org.junit.jupiter.api.Assertions.*;
/** 验证实际公共关联Mapper按DOI/来源稳定键查原表，并支持无PMID论文的双向穿透。 */
class NativeRelationPersistenceTest {
 @Test void sourceKeysAndDoiDrillThroughIntoCorrectNativeTables()throws Exception {
  var ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:relations_"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
  try(var c=ds.getConnection();var st=c.createStatement()){
   for(String source:List.of("gsa","bioproject")){
    st.execute("CREATE TABLE "+source+"_publication(id BIGINT PRIMARY KEY,"+source+"_record_key VARCHAR(400),pmid VARCHAR(32),doi VARCHAR(255),title TEXT,pub_year INT,source_url TEXT,gsa_accessions TEXT,bioproject_accession TEXT,grants_json TEXT,cncb_banks_json TEXT,source_hash CHAR(64),relation_status VARCHAR(20))");
    st.execute("INSERT INTO "+source+"_publication(id,"+source+"_record_key,doi,title,pub_year,relation_status) VALUES(1,'TITLE:abc',NULL,'Title-only paper',2026,'PENDING'),(2,'DOI:10.1234/b','10.1234/b','DOI-only paper',2025,'PENDING')");
   }
   st.execute("UPDATE gsa_publication SET gsa_accessions='CRA042678',bioproject_accession='PRJCA063830' WHERE id=1");
   st.execute("UPDATE bioproject_publication SET bioproject_accession='PRJCA063830' WHERE id=1");
   st.execute("CREATE TABLE research_publication_relation(id BIGINT AUTO_INCREMENT PRIMARY KEY,database_code VARCHAR(40),left_identifier_type VARCHAR(20),left_identifier VARCHAR(400),right_identifier_type VARCHAR(20),right_identifier VARCHAR(400),relation_type VARCHAR(40),evidence_url TEXT,discovery_depth INT,active INT,last_seen_run_id BIGINT,last_seen_time TIMESTAMP,UNIQUE(database_code,left_identifier_type,left_identifier,right_identifier_type,right_identifier))");
  }
  Configuration cfg=new Configuration();cfg.setMapUnderscoreToCamelCase(true);cfg.setArgNameBasedConstructorAutoMapping(true);cfg.setEnvironment(new Environment("test",new JdbcTransactionFactory(),ds));cfg.addMapper(PublicationRelationCollectionMapper.class);
  try(var in=getClass().getResourceAsStream("/mapper/research/PublicationRelationCollectionMapper.xml")){new XMLMapperBuilder(in,cfg,"mapper/PublicationRelationCollectionMapper.xml",cfg.getSqlFragments()).parse();}
  try(var session=new SqlSessionFactoryBuilder().build(cfg).openSession(true)){
   var mapper=session.getMapper(PublicationRelationCollectionMapper.class);
   for(String source:List.of("GSA","BIOPROJECT")){
    assertEquals(2,mapper.selectPendingRoots(source,false,10).size());assertNotNull(mapper.selectSourceRecord(source,"SOURCE_KEY","TITLE:abc"));assertNotNull(mapper.selectSourceRecord(source,"DOI","10.1234/b"));
    mapper.upsertRelation(source,"DOI","10.1234/b","SOURCE_KEY","TITLE:abc","https://example.org/project",1,1);mapper.upsertRelation(source,"DOI","10.1234/b","SOURCE_KEY","TITLE:abc","https://example.org/project",1,1);
    assertEquals(1,mapper.countRelations(source,"DOI","10.1234/b"));assertEquals("Title-only paper",mapper.selectRelations(source,"DOI","10.1234/b",0,10).get(0).title());
    assertEquals("PRJCA063830",mapper.selectRelations(source,"DOI","10.1234/b",0,10).get(0).bioprojectAccession());
    if(source.equals("GSA"))assertEquals("CRA042678",mapper.selectRelations(source,"DOI","10.1234/b",0,10).get(0).gsaAccessions());
    assertEquals("DOI-only paper",mapper.selectRelations(source,"SOURCE_KEY","TITLE:abc",0,10).get(0).title());
   }
  }
 }
}


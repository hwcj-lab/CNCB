package com.example.bioplatform.research.collection.gsa.service.impl;
import com.example.bioplatform.research.collection.gsa.service.impl.GsaPublicationParser;
import com.example.bioplatform.research.collection.gsa.service.impl.GsaStatisticsReader;
import com.fasterxml.jackson.databind.*;import org.junit.jupiter.api.Test;import org.junit.jupiter.api.BeforeEach;import org.junit.jupiter.api.AfterEach;
import java.nio.file.*;import java.util.*;import static org.junit.jupiter.api.Assertions.*;
/** 全部使用本地小样例，不请求上游或触碰生产数据库。 */
class GsaParserTest {
 private final ObjectMapper json=new ObjectMapper();Path folder=Path.of("target","gsa-parser-tests",UUID.randomUUID().toString());
 @BeforeEach void prepare()throws Exception{Files.createDirectories(folder);}
 @AfterEach void cleanup()throws Exception{Files.deleteIfExists(folder.resolve("source.json"));Files.deleteIfExists(folder.resolve("wrong.json"));Files.deleteIfExists(folder);}
 @Test void importsAllResourceTypesAndHtmlProjectLinks()throws Exception {
  var row=json.readTree("{\"pubmedId\":\"42341979\",\"articleTitle\":\"A paper\",\"pDate\":\"2027-02\",\"craAccession\":\"<a>CRA004775</a><br><a>CRA014497</a>, HRA005096, OMIX015927\",\"proAccession\":\"<a href='https://ngdc.cncb.ac.cn/bioproject/browse/PRJCA006211'>PRJCA006211</a><br>PRJCA019528\"}");
  var result=GsaPublicationParser.parse(row,json);
  assertEquals("PMID:42341979",result.getRecordKey());assertEquals(2027,result.getPubYear());
  assertEquals("CRA004775,CRA014497,HRA005096,OMIX015927",result.getAccessions());
  assertEquals("PRJCA006211,PRJCA019528",result.getBioprojectAccession());assertEquals(4,json.readTree(result.getCncbBanksJson()).size());
 }
 @Test void doiOrTitleCanIdentifyPaperWithoutPmid()throws Exception {
  var doi=GsaPublicationParser.parse(json.readTree("{\"pubmedId\":\"null\",\"doi\":\"https://doi.org/10.1234/AbC\",\"articleTitle\":\"Paper\"}"),json);
  assertNull(doi.getPmid());assertEquals("DOI:10.1234/abc",doi.getRecordKey());
  var title=GsaPublicationParser.parse(json.readTree("{\"articleTitle\":\"Paper without identifier\"}"),json);assertTrue(title.getRecordKey().startsWith("TITLE:"));
  assertThrows(IllegalArgumentException.class,()->GsaPublicationParser.parse(json.readTree("{}"),json));
 }
 @Test void streamsBatchesAndResumesFixedSnapshot()throws Exception {
  Path file=folder.resolve("source.json");Files.writeString(file,"{\"agency\":{\"publications\":[{}]},\"article\":{\"publicationCount\":4,\"publications\":[{\"pubmedId\":\"11111111\"},{\"pubmedId\":\"22222222\"},{\"pubmedId\":\"33333333\"},{\"pubmedId\":\"44444444\"}]}}");
  List<Long> cursors=new ArrayList<>();assertFalse(GsaStatisticsReader.read(file,0,2,json,(rows,next)->{cursors.add(next);return false;}));assertEquals(List.of(2L),cursors);
  List<String> pmids=new ArrayList<>();assertTrue(GsaStatisticsReader.read(file,2,1,json,(rows,next)->{pmids.add(rows.get(0).path("pubmedId").asText());return true;}));assertEquals(List.of("33333333","44444444"),pmids);
 }
 @Test void missingPublicationsIsFailureInsteadOfSuccessfulEmptyImport()throws Exception {
  Path file=folder.resolve("wrong.json");Files.writeString(file,"{\"article\":{\"journals\":[]}}");
  assertThrows(Exception.class,()->GsaStatisticsReader.read(file,0,10,json,(rows,next)->true));
 }
}

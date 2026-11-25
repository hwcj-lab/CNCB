package com.example.bioplatform.research.collection.bioproject.service.impl;
import com.example.bioplatform.research.collection.bioproject.service.impl.BioProjectDocumentParser;
import com.example.bioplatform.research.collection.bioproject.service.impl.BioProjectPublicationParser;
import com.example.bioplatform.research.collection.bioproject.service.impl.BioProjectStatisticsReader;
import com.example.bioplatform.research.collection.common.service.impl.CollectionHttpTransport;
import com.fasterxml.jackson.databind.*;import org.junit.jupiter.api.Test;import java.nio.file.*;import java.util.*;import static org.junit.jupiter.api.Assertions.*;
class BioProjectParserTest {
 private final ObjectMapper json=new ObjectMapper();
 private String html(){return "<table><tr><th>PubMed ID</th><th>Title</th></tr><tr><td><a href='https://pubmed.ncbi.nlm.nih.gov/41769996'>41769996</a></td><td>Paper A</td><td>Microbiology Spectrum</td><td><a href='https://doi.org/10.1128/spectrum.02676-25'>10.1128/spectrum.02676-25</a></td><td>2026</td></tr><tr><td></td><td>Paper B</td><td>Journal B</td><td>10.1234/b</td><td>2025</td></tr></table><table><tr><th>授权项目ID</th></tr><tr><td>Zhaoqing Municipal Bureau of Science and Technology</td><td></td><td>2023040316002</td><td>Project title</td></tr></table><th>提交单位</th><td>Guangdong Provincial Institute of Public Health</td><table><tr><td>Unrelated resource data</td></tr></table>";}
 @Test void parsesOnlyPublicationTableAndKeepsProjectOrganizationSeparate(){
  var document=BioProjectDocumentParser.html(html(),json);assertEquals(2,document.path("listPublication").size());assertEquals("41769996",document.path("listPublication").get(0).path("pubmedId").asText());assertEquals("Zhaoqing Municipal Bureau of Science and Technology",document.path("listGrants").get(0).path("agency").asText());assertEquals("Guangdong Provincial Institute of Public Health",document.path("submitter").path("organization").asText());
 }
 @Test void validApiWithoutPmidDoesNotFetchHtmlOrFillPmid()throws Exception {
  List<String> calls=new ArrayList<>();var doc=BioProjectDocumentParser.fetch("PRJCA039050",json,url->{calls.add(url);return url.contains("/api/")?"{\"message\":\"SUCCESS\",\"listPublication\":[{\"articleTitle\":\"Paper A\",\"doi\":\"10.1128/spectrum.02676-25\"}],\"listGrants\":[]}":html();});
  assertEquals(1,calls.size());assertEquals(1,doc.path("listPublication").size());assertEquals("",doc.path("listPublication").get(0).path("pubmedId").asText());
 }
 @Test void completeApiPmidDoesNotFetchHtml()throws Exception{
  List<String> calls=new ArrayList<>();BioProjectDocumentParser.fetch("PRJCA039050",json,url->{calls.add(url);return "{\"message\":\"SUCCESS\",\"listPublication\":[{\"pubmedId\":\"41769996\"}]}";});assertEquals(1,calls.size());
 }
 @Test void streamsEveryPaperAndEmptyProjectWithDurableProjectCursor()throws Exception {
  Path dir=Path.of("target/bioproject-parser-tests");Files.createDirectories(dir);Path file=dir.resolve(UUID.randomUUID()+".json");
  try{Files.writeString(file,"[{\"accession\":\"PRJCA039050\"},{\"accession\":\"PRJCA000091\"}]");CollectionHttpTransport.validateJsonPrefix(file);List<Long> cursors=new ArrayList<>();List<JsonNode> papers=new ArrayList<>();
   assertTrue(BioProjectStatisticsReader.read(file,0,1,json,row->row.path("accession").asText().equals("PRJCA039050")?BioProjectDocumentParser.html(html(),json):json.readTree("{\"listPublication\":[]}"),(batch,next)->{papers.addAll(batch);cursors.add(next);return true;}));
   assertEquals(2,papers.size());assertEquals(List.of(0L,0L,1L,2L),cursors);
   var seed=BioProjectPublicationParser.parse(papers.get(1),json);assertEquals("DOI:10.1234/b",seed.getRecordKey());assertEquals("PRJCA039050",seed.getBioprojectAccession());assertTrue(seed.getGrantsJson().contains("Zhaoqing"));assertTrue(seed.getRawPayload().contains("Guangdong"));
   List<String> loaded=new ArrayList<>();BioProjectStatisticsReader.read(file,1,10,json,row->{loaded.add(row.path("accession").asText());return json.readTree("{\"listPublication\":[]}");},(batch,next)->true);assertEquals(List.of("PRJCA000091"),loaded);
  }finally{Files.deleteIfExists(file);}
 }
 @Test void rejectsVerificationPageAndInvalidAccession(){assertThrows(IllegalStateException.class,()->BioProjectDocumentParser.html("<html>SecurityCheck</html>",json));assertThrows(IllegalArgumentException.class,()->BioProjectDocumentParser.fetch("../bad",json,url->""));}
}

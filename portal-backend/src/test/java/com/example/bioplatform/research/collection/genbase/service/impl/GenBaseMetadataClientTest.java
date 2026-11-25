package com.example.bioplatform.research.collection.genbase.service.impl;
import com.example.bioplatform.research.collection.genbase.dto.GenBasePmidTask;
import com.example.bioplatform.research.collection.genbase.service.impl.GenBaseMetadataClient;
import com.example.bioplatform.research.collection.common.service.impl.CollectionHttpTransport;
import com.fasterxml.jackson.databind.*;import org.junit.jupiter.api.Test;import java.util.*;import java.util.function.*;import static org.junit.jupiter.api.Assertions.*;
class GenBaseMetadataClientTest {
 private final ObjectMapper json=new ObjectMapper();
 static class FakeHttp extends CollectionHttpTransport {
  final Map<String,String> responses=new LinkedHashMap<>();final List<String> calls=new ArrayList<>();
  public String text(String url,int attempts,BooleanSupplier stop,Consumer<String> progress){calls.add(url);if(!responses.containsKey(url))throw new IllegalStateException("不应请求："+url);return responses.get(url);}
 }
 private GenBasePmidTask task(){var t=new GenBasePmidTask();t.setId(1L);t.setPaperId(11L);t.setTitle("Current paper");return t;}
 private String record(String pmid,String doi){return "{\"code\":\"200\",\"result\":{\"data\":[{\"pmid\":\""+pmid+"\",\"doi\":\""+doi+"\",\"authors\":[{\"firstName\":\"Wei\",\"lastName\":\"Li\",\"affiliation\":\"University A\"},{\"firstName\":\"Yan\",\"lastName\":\"Wang\",\"affiliation\":\"University B\"}],\"grants\":[{\"grantId\":\"A1\",\"agency\":\"Agency A\"}],\"cncbBanks\":[]}]}}";}
 private String openlb(String id){return "https://ngdc.cncb.ac.cn/openlb/api/pmid?pmid="+id;}
 private String doiSearch(String doi){return "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=pubmed&retmode=json&retmax=2&term="+java.net.URLEncoder.encode(doi+"[doi]",java.nio.charset.StandardCharsets.UTF_8);}
 private String project(){return "https://ngdc.cncb.ac.cn/gwh/api/public/bioProject/PRJCA039050";}
 @Test void missingPmidDoesNotTriggerDoiSearchOrWriteAnyPmid()throws Exception {
  var http=new FakeHttp();var t=task();t.setDoi("10.1234/current");
  var result=new GenBaseMetadataClient(http,json).enrich(t,800,()->false,v->{});assertNull(result.getPmid());assertEquals("NO_PMID",result.getStatus());assertTrue(http.calls.isEmpty());assertEquals(t.getDoi(),result.getDoi());assertNull(t.getPmid());
 }
 @Test void projectMatchesCurrentArticleAmongMultiplePublicationsWithoutBorrowingSubmitterAffiliation()throws Exception {
  var http=new FakeHttp();var t=task();t.setBioprojectAccession("PRJCA039050");http.responses.put(project(),"{\"message\":\"SUCCESS\",\"listPublication\":[{\"articleTitle\":\"Other paper\",\"pubmedId\":\"11111111\"},{\"articleTitle\":\"Current Paper.\",\"pubmedId\":\"41769996\",\"doi\":\"10.1234/current\"}],\"listGrants\":[{\"agency\":\"Zhaoqing bureau\",\"grantID\":\"P1\"}],\"submitter\":{\"organization\":\"Project submitter organization\"}}");http.responses.put(openlb("41769996"),record("41769996","10.1234/current"));
  var out=new GenBaseMetadataClient(http,json).enrich(t,800,()->false,v->{});assertNull(out.getPmid());assertTrue(out.getGrantsJson().contains("Zhaoqing bureau"));assertTrue(out.getAuthorsJson()==null || !out.getAuthorsJson().contains("Project submitter organization"));assertTrue(out.getRawPayload().contains("Project submitter organization"));assertEquals("BIOPROJECT_METADATA",json.readTree(out.getRawPayload()).path("identifierResolution").path("method").asText());assertEquals(1,http.calls.size());
 }
 @Test void completeExistingPmidDoesNotQueryBioProject()throws Exception {
  var http=new FakeHttp();var t=task();t.setPmid("41769996");t.setBioprojectAccession("PRJCA039050");http.responses.put(openlb(t.getPmid()),record(t.getPmid(),"10.1234/current"));var out=new GenBaseMetadataClient(http,json).enrich(t,800,()->false,v->{});assertEquals("SUCCESS",out.getStatus());assertEquals(List.of(openlb(t.getPmid())),http.calls);
 }
 @Test void unrelatedSoleProjectPublicationDoesNotSupplyPmidOrFunding()throws Exception {
  var http=new FakeHttp();var t=task();t.setBioprojectAccession("PRJCA039050");http.responses.put(project(),"{\"message\":\"SUCCESS\",\"listPublication\":[{\"articleTitle\":\"Other paper\",\"pubmedId\":\"11111111\"}],\"listGrants\":[{\"agency\":\"Wrong agency\"}]}");var out=new GenBaseMetadataClient(http,json).enrich(t,800,()->false,v->{});assertNull(out.getPmid());assertEquals("NO_PMID",out.getStatus());assertEquals("[]",out.getGrantsJson());assertEquals(1,http.calls.size());
 }
 @Test void ambiguousTitleAndConflictingDoiAreRejected()throws Exception {
  var t=task();JsonNode papers=json.readTree("[{\"articleTitle\":\"Current paper\",\"pubmedId\":\"11111111\"},{\"articleTitle\":\"Current paper\",\"pubmedId\":\"22222222\"}]");assertNull(GenBaseMetadataClient.matchPublication(papers,t));t.setDoi("10.1234/current");assertNull(GenBaseMetadataClient.matchPublication(json.readTree("[{\"articleTitle\":\"Current paper\",\"doi\":\"10.1234/other\",\"pubmedId\":\"11111111\"}]"),t));
 }
 @Test void emptyOpenlbFallsBackToPubmedXml()throws Exception {
  var http=new FakeHttp();var t=task();t.setPmid("41769996");http.responses.put(openlb(t.getPmid()),"{\"code\":\"200\",\"result\":{\"data\":[]}}");http.responses.put("https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&retmode=xml&id=41769996","<PubmedArticleSet><PubmedArticle><MedlineCitation><PMID>41769996</PMID><Article><ArticleTitle>Current paper</ArticleTitle><AuthorList><Author><ForeName>Wei</ForeName><LastName>Li</LastName><AffiliationInfo><Affiliation>University A</Affiliation></AffiliationInfo></Author></AuthorList><GrantList><Grant><Agency>Agency A</Agency><GrantID>A1</GrantID></Grant></GrantList></Article></MedlineCitation></PubmedArticle></PubmedArticleSet>");var out=new GenBaseMetadataClient(http,json).enrich(t,800,()->false,v->{});assertEquals("SUCCESS",out.getStatus());assertTrue(out.getAuthorsJson().contains("University A"));assertTrue(out.getGrantsJson().contains("Agency A"));assertEquals(2,http.calls.size());
 }
 @Test void metadataIdentifierMismatchDoesNotRewriteOriginal()throws Exception {
  var http=new FakeHttp();var t=task();t.setPmid("41769996");t.setDoi("10.1234/current");http.responses.put(doiSearch(t.getDoi()),"{\"esearchresult\":{\"idlist\":[\"41769996\"]}}");http.responses.put(openlb("41769996"),record("41769996","10.1234/other"));assertThrows(IllegalStateException.class,()->new GenBaseMetadataClient(http,json).enrich(t,800,()->false,v->{}));assertEquals("10.1234/current",t.getDoi());assertEquals("41769996",t.getPmid());
 }
 @Test void missingIdentifiersPreserveExistingAuthorAndFundingWithoutAnyNetworkRequest()throws Exception {
  var http=new FakeHttp();var t=task();t.setAuthorsJson("[{\"firstName\":\"Wei\",\"lastName\":\"Li\",\"affiliation\":\"University A\"}]");t.setGrantsJson("[{\"agency\":\"Existing agency\"}]");var out=new GenBaseMetadataClient(http,json).enrich(t,800,()->false,v->{});assertEquals("NO_PMID",out.getStatus());assertEquals(t.getAuthorsJson(),out.getAuthorsJson());assertEquals(t.getGrantsJson(),out.getGrantsJson());assertTrue(http.calls.isEmpty());
 }
}

package com.example.bioplatform.research.collection.gsa.service.impl;
import com.example.bioplatform.research.collection.gsa.dto.GsaCollectionStartRequest;
import com.example.bioplatform.research.collection.gsa.dto.GsaEnrichmentTask;
import com.example.bioplatform.research.collection.common.service.impl.CollectionHttpTransport;
import com.example.bioplatform.research.collection.gsa.service.impl.GsaMetadataClient;
import com.fasterxml.jackson.databind.*;import org.junit.jupiter.api.Test;import java.util.*;import java.util.function.*;import static org.junit.jupiter.api.Assertions.*;
class GsaMetadataClientTest {
 static class FakeHttp extends CollectionHttpTransport {
  final Map<String,String> data=new HashMap<>();final List<String> calls=new ArrayList<>();
  @Override public String text(String url,int attempts,BooleanSupplier stopped,Consumer<String> progress){calls.add(url);if(!data.containsKey(url))throw new IllegalStateException("不应请求："+url);return data.get(url);}
 }
 @Test void completePmidMetadataDoesNotQueryProjectsAndPreservesExistingAgency()throws Exception {
  var json=new ObjectMapper();var http=new FakeHttp();http.data.put("https://ngdc.cncb.ac.cn/openlb/api/pmid?pmid=41028458","{\"code\":\"200\",\"result\":{\"data\":[{\"pmid\":\"41028458\",\"authors\":[{\"firstName\":\"Wei\",\"lastName\":\"Li\",\"affiliation\":\"University A\"}],\"grants\":[{\"grantId\":\"G1\",\"agency\":\"new name\"}],\"cncbBanks\":[]}]}}");
  var task=new GsaEnrichmentTask();task.setId(1L);task.setPmid("41028458");task.setBioprojectAccession("PRJCA000091");task.setGrantsJson("[{\"grantId\":\"G1\",\"agency\":\"original name\"}]");task.setCncbBanksJson("[{\"db\":\"BioProject\",\"accession\":\"PRJCA000091\",\"link\":\"https://ngdc.cncb.ac.cn/bioproject/browse/PRJCA000091\"}]");
  var out=new GsaMetadataClient(http,json,null).enrich(task,new GsaCollectionStartRequest(null,null,null),()->false,v->{});
  assertEquals("SUCCESS",out.getStatus());assertEquals("original name",json.readTree(out.getGrantsJson()).get(0).path("agency").asText());assertEquals(1,json.readTree(out.getCncbBanksJson()).size());assertEquals(1,http.calls.size());
 }
 @Test void doiOnlyRecordDoesNotQueryOrFillPmid()throws Exception {
  var http=new FakeHttp();http.data.put("https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=pubmed&retmode=json&retmax=2&term=10.1234%2Fabc%5Bdoi%5D","{\"esearchresult\":{\"idlist\":[\"11111111\",\"22222222\"]}}");
  var task=new GsaEnrichmentTask();task.setId(1L);task.setDoi("10.1234/abc");task.setTitle("A paper");
  var result=new GsaMetadataClient(http,new ObjectMapper(),null).enrich(task,new GsaCollectionStartRequest(null,null,null),()->false,v->{});
  assertTrue(http.calls.isEmpty());assertNull(result.getPmid());assertEquals("NO_PMID",result.getStatus());assertEquals("A paper",result.getTitle());
 }
}

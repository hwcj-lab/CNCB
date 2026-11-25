package com.example.bioplatform.research.statistics.service.impl;
import com.example.bioplatform.research.collection.common.service.RelatedResourceData;
import com.example.bioplatform.research.collection.common.service.RelatedResourceResolver;
import com.example.bioplatform.research.collection.bioproject.service.impl.BioProjectMetadataClient;
import com.example.bioplatform.research.collection.bioproject.service.relation.BioProjectDiscoveryStrategy;
import com.example.bioplatform.research.collection.common.service.relation.CncbBanksDiscoveryStrategy;
import com.example.bioplatform.research.collection.genbase.service.relation.GenBaseRelationSupport;
import com.example.bioplatform.research.collection.common.service.relation.RelatedResourceResolverFactory;
import com.example.bioplatform.research.collection.common.service.relation.RelationDiscoveryFactory;
import com.example.bioplatform.research.collection.common.service.relation.RelationDiscoveryInput;
import com.example.bioplatform.research.collection.common.service.relation.RelationDiscoveryResult;
import com.example.bioplatform.research.collection.common.service.relation.RelationDiscoveryStrategy;
import com.example.bioplatform.research.collection.common.service.relation.RelationIdentifierFactory;
import com.example.bioplatform.research.collection.common.service.relation.RelationIdentifierResolver;
import com.example.bioplatform.research.collection.common.service.relation.RelationPageClient;
import com.example.bioplatform.research.collection.common.service.relation.RoutedRelationSupport;
import com.example.bioplatform.research.collection.common.service.relation.StableSourceRelationIdentifiers;
import com.example.bioplatform.research.collection.bioproject.service.impl.BioProjectMetadataClient;import com.fasterxml.jackson.databind.*;import org.junit.jupiter.api.Test;import java.util.*;import static org.junit.jupiter.api.Assertions.*;
class RelationFactoriesTest {
 private final ObjectMapper json=new ObjectMapper();
 static class Projects extends BioProjectMetadataClient {int calls;JsonNode response;Projects(ObjectMapper json){super(json);}public JsonNode fetch(String code){calls++;return response;}}
 @Test void cncbBanksStrategyHasPriorityAndPreservesPmidOrderFundsAndStableReferences()throws Exception {
  var projects=new Projects(json);var resolver=new RelatedResourceResolver(){public List<String> supportedDatabaseNames(){return List.of("Example");}public RelatedResourceData findRelatedPublications(String accession,String url)throws Exception{return new RelatedResourceData(List.of("11111111","22222222","22222222"),json.readTree("[{\"agency\":\"Agency A\"}]"),List.of(json.readTree("{\"articleTitle\":\"Other paper\",\"proAccession\":\"PRJCA039050\"}")));}};
  var cncb=new CncbBanksDiscoveryStrategy(new RelatedResourceResolverFactory(List.of(resolver)),new RelationPageClient(),json);var factory=new RelationDiscoveryFactory(List.of(new BioProjectDiscoveryStrategy(projects,json),cncb),json);
  var input=new RelationDiscoveryInput("GSA","PMID","11111111","11111111","[{\"db\":\"Example\",\"accession\":\"E1\",\"link\":\"https://example.org/E1\"}]","PRJCA039050",true);int[] requests={0};var out=factory.discover(input,()->requests[0]++);
  assertEquals("CNCB_BANKS",out.strategy());assertEquals(List.of("22222222"),new ArrayList<>(out.pmids().keySet()));assertEquals("https://example.org/E1",out.pmids().get("22222222"));assertEquals(1,out.publicationRecords().size());assertEquals(1,out.grants().size());assertEquals(0,projects.calls);assertEquals(1,requests[0]);
 }
 @Test void projectFallbackRemainsIndependentAndDoesNotAddNewFundOrReferenceSemantics()throws Exception{
  var projects=new Projects(json);projects.response=json.readTree("{\"listPublication\":[{\"pubmedId\":\"11111111\"},{\"pubmedId\":\"22222222\"}],\"listGrants\":[{\"agency\":\"Agency A\"}]}");
  var factory=new RelationDiscoveryFactory(List.of(new BioProjectDiscoveryStrategy(projects,json),new CncbBanksDiscoveryStrategy(new RelatedResourceResolverFactory(List.of()),new RelationPageClient(),json)),json);
  var out=factory.discover(new RelationDiscoveryInput("GENBASE","PAPER_ID","11","11111111","[]","PRJCA039050",false),()->fail("原项目回退不额外调用cncbBanks页面"));assertEquals("BIOPROJECT",out.strategy());assertEquals(Set.of("22222222"),out.pmids().keySet());assertTrue(out.grants().isEmpty());assertTrue(out.publicationRecords().isEmpty());assertEquals(1,projects.calls);
 }
 @Test void newStrategyCanBeRegisteredWithoutChangingExistingStrategyOrQueue()throws Exception {
  RelationDiscoveryStrategy custom=new RelationDiscoveryStrategy(){public String code(){return "CUSTOM";}public int priority(){return 50;}public boolean supports(RelationDiscoveryInput i){return "CUSTOM".equals(i.databaseCode());}public RelationDiscoveryResult discover(RelationDiscoveryInput i,BeforeRequest c){return new RelationDiscoveryResult(code(),Map.of("33333333","https://example.org"),List.of(),json.createArrayNode());}};
  var factory=new RelationDiscoveryFactory(List.of(custom),json);assertEquals("CUSTOM",factory.discover(new RelationDiscoveryInput("CUSTOM","SOURCE_KEY","1",null,null,null,false),()->{}).strategy());assertEquals("NONE",factory.discover(new RelationDiscoveryInput("OTHER","SOURCE_KEY","1",null,null,null,false),()->{}).strategy());
 }
 @Test void identifierFactoryKeepsNativeKeysAndDefaultSourceRules(){
  var factory=new RelationIdentifierFactory(List.of(new RoutedRelationSupport(null),new StableSourceRelationIdentifiers(),new GenBaseRelationSupport(null)));
  assertEquals(new RelationIdentifierResolver.Identifier("PAPER_ID","11"),factory.resolve("GENBASE",Map.of("paperId","11","pmid","11111111")));
  assertEquals(new RelationIdentifierResolver.Identifier("DOI","10.1234/a"),factory.resolve("GENBASE",Map.of("doi","https://doi.org/10.1234/A")));
  assertEquals(new RelationIdentifierResolver.Identifier("SOURCE_KEY","RESOURCE:PRJCA039050"),factory.resolve("GENBASE",Map.of("sourceKey","RESOURCE:PRJCA039050")));
  assertEquals(new RelationIdentifierResolver.Identifier("SOURCE_KEY","TITLE:abc"),factory.resolve("GSA",Map.of("sourceKey","TITLE:abc")));assertThrows(RuntimeException.class,()->factory.resolve("OTHER",Map.of("doi","10.1234/a")));
 }
}

package com.example.bioplatform.research.collection.openlb.service.impl;
import com.example.bioplatform.research.collection.openlb.mapper.OpenLbCollectionMapper;
import com.example.bioplatform.research.collection.openlb.mapper.OpenLbRoutingMapper;
import com.example.bioplatform.research.collection.openlb.dto.OpenLbCollectionStartRequest;
import com.example.bioplatform.research.collection.openlb.dto.OpenLbPublicationWriteRow;
import com.example.bioplatform.research.integration.service.ResearchPublicationIntegrationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class OpenLbPipelineTest {
 @Test void multipleBanksRouteOncePerDatabaseAndUnknownBanksAreArchived() {
  var routes=mock(OpenLbRoutingMapper.class);var fallback=mock(OpenLbCollectionMapper.class);
  when(routes.routes()).thenReturn(List.of(Map.of("bank_db","GenBase","source_code","GENBASE"),Map.of("bank_db","GSA","source_code","GSA")));
  var service=new OpenLbRoutingService(routes,fallback,new ObjectMapper());
  var row=OpenLbPublicationWriteRow.builder().pmid("12345678").cncbBanksJson("[{\"db\":\"GenBase\"},{\"db\":\"GenBase\"},{\"db\":\"GSA\"},{\"db\":\"Unknown\"}]").build();
  service.route(List.of(row));verify(routes).writeGenBase(List.of(row));verify(routes).write("gsa_publication",List.of(row));verify(fallback).insertPublicationsIgnore(List.of(row));
 }
 @Test void successfulBatchRoutesThenIntegratesThenAcknowledgesWithoutCallingOtherCollectors() throws Exception {
  var mapper=mock(OpenLbCollectionMapper.class);var routing=mock(OpenLbRoutingService.class);var integration=mock(ResearchPublicationIntegrationService.class);var tx=mock(org.springframework.transaction.support.TransactionTemplate.class);
  doAnswer(invocation->{((java.util.function.Consumer<org.springframework.transaction.TransactionStatus>)invocation.getArgument(0)).accept(mock(org.springframework.transaction.TransactionStatus.class));return null;}).when(tx).executeWithoutResult(any());
  var json=new ObjectMapper();var service=new OpenLbCollectionServiceImpl(mapper,routing,integration,json,tx);
  Class<?> type=Arrays.stream(OpenLbCollectionServiceImpl.class.getDeclaredClasses()).filter(c->c.getSimpleName().equals("CollectionResult")).findFirst().orElseThrow();
  var success=type.getDeclaredMethod("success",String.class,com.fasterxml.jackson.databind.JsonNode.class,String.class);success.setAccessible(true);
  String payload="{\"pmid\":\"12345678\",\"title\":\"Fixture\",\"cncbBanks\":[{\"db\":\"GSA\"}],\"authors\":[],\"grants\":[]}";
  var result=success.invoke(null,"12345678",json.readTree(payload),payload);
  ReflectionTestUtils.invokeMethod(service,"persistBatch",List.of(result),new OpenLbCollectionStartRequest(1,1,100L,1,1));
  var order=inOrder(routing,integration,mapper);order.verify(routing).route(anyList());order.verify(integration).syncIdentifiers(List.of("12345678"),List.of());order.verify(mapper).markSuccess(List.of("12345678"));service.shutdown();
 }
 @Test void failedRecoveryCannotLeaveJobPermanentlyRunning() {
  var mapper=mock(OpenLbCollectionMapper.class);var service=new OpenLbCollectionServiceImpl(mapper,mock(OpenLbRoutingService.class),mock(ResearchPublicationIntegrationService.class),new ObjectMapper(),mock(org.springframework.transaction.support.TransactionTemplate.class));
  when(mapper.findPendingPmids(anyInt())).thenReturn(List.of());doThrow(new IllegalStateException("database unavailable")).when(mapper).recoverRunningTasks("Recovered after stopped job");
  var flag=(java.util.concurrent.atomic.AtomicBoolean)ReflectionTestUtils.getField(service,"running");flag.set(true);
  ReflectionTestUtils.invokeMethod(service,"runJob",new OpenLbCollectionStartRequest(1,1,100L,1,1));
  assertFalse(flag.get());assertNotNull(ReflectionTestUtils.getField(service,"finishedAt"));service.shutdown();
 }
}

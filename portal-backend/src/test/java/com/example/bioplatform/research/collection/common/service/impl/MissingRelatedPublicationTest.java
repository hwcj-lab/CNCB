package com.example.bioplatform.research.collection.common.service.impl;

import com.example.bioplatform.research.collection.common.dto.*;
import com.example.bioplatform.research.collection.common.mapper.PublicationRelationCollectionMapper;
import com.example.bioplatform.research.collection.common.service.relation.*;
import com.example.bioplatform.research.collection.common.vo.PublicationRelationJobStatus;
import com.example.bioplatform.research.collection.openlb.service.relation.OpenLbPublicationDetailsClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.io.IOException;
import java.util.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MissingRelatedPublicationTest {
    private final ObjectMapper json = new ObjectMapper();
    private final PublicationRelationCollectionMapper mapper = mock(PublicationRelationCollectionMapper.class);
    private final RelationDiscoveryFactory discovery = mock(RelationDiscoveryFactory.class);
    private final RelatedPublicationStore store = mock(RelatedPublicationStore.class);
    private final OpenLbPublicationDetailsClient details = mock(OpenLbPublicationDetailsClient.class);
    private final NativeRelatedPublicationWriter nativeWriter = mock(NativeRelatedPublicationWriter.class);

    private PublicationRelationCollectionServiceImpl service() {
        var first = new RelationQueueNode(1L,8L,"PMID","41359036",0);
        var second = new RelationQueueNode(2L,8L,"PMID","39530327",0);
        when(mapper.findNextPending(8L)).thenReturn(first,second,null);
        when(mapper.selectJob(8L)).thenReturn(new PublicationRelationJobStatus(false,false,8L,"GENBASE","BATCH","test","RUNNING",1000,0L,0,0,0,2L,null,null,null));
        for(String id:List.of("41359036","39530327"))
            when(mapper.selectSourceRecord("GENBASE","PMID",id)).thenReturn(new RelationSourceRecord("PMID",id,id,null,"[]","[]","hash","PENDING"));
        return new PublicationRelationCollectionServiceImpl(null,mapper,null,nativeWriter,null,discovery,store,null,details,new RelatedPublicationAssembler(json),json);
    }

    @Test void missingChildIsDiscardedButAnotherChildAndTheNextRootAreProcessed() throws Exception {
        var found = new LinkedHashMap<String,String>();
        found.put("25431773","https://example.org/missing");
        found.put("38018256","https://example.org/found");
        when(discovery.discover(any(),any())).thenReturn(new RelationDiscoveryResult("CNCB_BANKS",found,List.of(),json.createArrayNode()));
        when(details.read("25431773")).thenThrow(new PublicationNotFoundException("25431773"));
        when(details.read("38018256")).thenReturn(json.readTree("{\"pmid\":\"38018256\",\"title\":\"Valid\"}"));
        var service=service();
        try {
            ReflectionTestUtils.invokeMethod(service,"runJob",8L,false,true,1000,0L);
            verify(details,times(1)).read("25431773"); // Known misses are bounded and reused for this run.
            verify(store,times(1)).write(eq("GENBASE"),argThat(rows->rows.size()==1&&"38018256".equals(rows.get(0).pmid())));
            verify(mapper,times(2)).upsertRelation(anyString(),anyString(),anyString(),anyString(),anyString(),eq("https://example.org/found"),eq(1),eq(8L));
            verify(mapper,never()).insertTerminalQueue(eq(8L),eq("PMID"),eq("25431773"),anyInt());
            verify(mapper).finishQueue(1L,"SUCCESS");
            verify(mapper).finishQueue(2L,"SUCCESS");
            verify(mapper).finishJob(8L,"SUCCESS",null);
            verify(mapper,never()).failQueue(anyLong(),anyString());
        } finally {service.shutdown();}
    }

    @Test void networkErrorStillStopsBeforeTheNextRootAndKeepsTheFailedNodeRetryable() throws Exception {
        when(discovery.discover(any(),any())).thenReturn(new RelationDiscoveryResult("CNCB_BANKS",Map.of("25431773","https://example.org"),List.of(),json.createArrayNode()));
        when(details.read("25431773")).thenThrow(new IOException("HTTP 429"));
        var service=service();
        try {
            ReflectionTestUtils.invokeMethod(service,"runJob",8L,false,true,1000,0L);
            verify(mapper).failQueue(1L,"HTTP 429");
            verify(mapper).finishJob(8L,"FAILED","HTTP 429");
            verify(mapper,never()).markQueueRunning(2L);
            verifyNoInteractions(store);
        } finally {service.shutdown();}
    }

    @Test void missingRootRefreshIsSkippedWithoutDeletingStoredDataOrBlockingTheNextRoot() throws Exception {
        when(details.read("41359036")).thenThrow(new PublicationNotFoundException("41359036"));
        when(details.read("39530327")).thenReturn(json.readTree("{\"pmid\":\"39530327\",\"title\":\"Valid\"}"));
        var service=service();
        try {
            ReflectionTestUtils.invokeMethod(service,"runJob",8L,true,false,1000,0L);
            verify(mapper).skipQueue(eq(1L),contains("41359036"));
            verify(mapper).finishQueue(2L,"SUCCESS");
            verify(mapper).finishJob(8L,"SUCCESS",null);
            verify(mapper,never()).failQueue(anyLong(),anyString());
        } finally {service.shutdown();}
    }
}

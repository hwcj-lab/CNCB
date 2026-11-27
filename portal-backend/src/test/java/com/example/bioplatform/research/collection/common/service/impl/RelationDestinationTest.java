package com.example.bioplatform.research.collection.common.service.impl;

import com.example.bioplatform.research.collection.common.dto.*;
import com.example.bioplatform.research.collection.common.mapper.PublicationRelationCollectionMapper;
import com.example.bioplatform.research.collection.common.service.relation.*;
import com.example.bioplatform.research.collection.common.vo.PublicationRelationJobStatus;
import com.example.bioplatform.research.collection.openlb.service.relation.OpenLbPublicationDetailsClient;
import com.example.bioplatform.research.integration.service.ResearchPublicationIntegrationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RelationDestinationTest {
    @Test void exhaustingTheRootQueueAtTheExactBudgetCompletesInsteadOfPausing() {
        var mapper=mock(PublicationRelationCollectionMapper.class);
        var node=new RelationQueueNode(1L,9L,"PMID","41359036",0);
        when(mapper.findNextPending(9L)).thenReturn(node,null);
        when(mapper.selectJob(9L)).thenReturn(new PublicationRelationJobStatus(false,false,9L,"GENBASE","PMID","41359036","RUNNING",1,100L,0,0,0,1L,null,null,null));
        when(mapper.selectSourceRecord("GENBASE","PMID","41359036")).thenReturn(new RelationSourceRecord("PMID","41359036","41359036",null,"[]","[]","hash","PENDING"));
        var service=new PublicationRelationCollectionServiceImpl(null,mapper,null,null,null,null,null,null,null,null,new ObjectMapper());
        try {
            ReflectionTestUtils.invokeMethod(service,"runJob",9L,false,false,1,0L);
            verify(mapper).finishDescendantQueue(9L);
            verify(mapper).finishJob(9L,"SUCCESS",null);
            verify(mapper,never()).finishJob(eq(9L),eq("PAUSED"),any());
        } finally { service.shutdown(); }
    }
    @Test void discoveredPapersStayInTheJobsDatabaseEvenWhenTheirBanksPointElsewhere() throws Exception {
        var json = new ObjectMapper();
        var mapper = mock(PublicationRelationCollectionMapper.class);
        var nativeWriter = mock(NativeRelatedPublicationWriter.class);
        var discovery = mock(RelationDiscoveryFactory.class);
        var store = mock(RelatedPublicationStore.class);
        var details = mock(OpenLbPublicationDetailsClient.class);
        var service = new PublicationRelationCollectionServiceImpl(null, mapper, null, nativeWriter,
                mock(ResearchPublicationIntegrationService.class), discovery, store, null, details,
                new RelatedPublicationAssembler(json), json);
        try {
            when(mapper.selectJob(8L)).thenReturn(new PublicationRelationJobStatus(false, false, 8L, "GENBASE", "PMID", "41359036", "RUNNING", 1000, 800L, 0, 0, 0, 1L, null, null, null));
            when(mapper.selectSourceRecord("GENBASE", "PMID", "41359036")).thenReturn(new RelationSourceRecord("PMID", "41359036", "41359036", null, "[]", "[]", "hash", "PENDING"));
            when(discovery.discover(any(), any())).thenReturn(new RelationDiscoveryResult("CNCB_BANKS", Map.of("39530327", "https://ngdc.cncb.ac.cn/databasecommons/database/id/1802"), List.of(), json.createArrayNode()));
            when(details.read("39530327")).thenReturn(json.readTree("{\"pmid\":\"39530327\",\"title\":\"Fixture\",\"authors\":[],\"grants\":[],\"cncbBanks\":[{\"db\":\"GWH\"},{\"db\":\"BioCode\"}]}"));
            ReflectionTestUtils.invokeMethod(service, "processNode", 8L, new RelationQueueNode(1L, 8L, "PMID", "41359036", 0), false, true, 0L);
            verify(store).write(eq("GENBASE"), argThat(rows -> rows.size() == 1 && "39530327".equals(rows.get(0).pmid())));
            verifyNoMoreInteractions(store);
            verify(mapper).insertTerminalQueue(8L,"PMID","39530327",1);
            verify(mapper,never()).insertQueueIgnore(anyLong(),anyString(),anyString(),anyInt());
            verify(mapper).upsertRelation(eq("GENBASE"), eq("PMID"), eq("39530327"), eq("PMID"), eq("41359036"), anyString(), eq(1), eq(8L));
        } finally {
            service.shutdown();
        }
    }
}

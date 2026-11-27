package com.example.bioplatform.research.preparation;

import com.example.bioplatform.research.statistics.prepared.mapper.PreparedStatisticsMapper;
import com.example.bioplatform.research.statistics.prepared.model.PreparedSnapshotConfig;
import com.example.bioplatform.research.statistics.prepared.service.PreparedSnapshotService;
import com.example.bioplatform.research.statistics.mapper.ResearchSnapshotMapper;
import com.example.bioplatform.research.statistics.vo.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class PreparedTopicCategoriesTest {
    @Test void totalIncludesFrozenEmptyCategoriesAndPreservesExistingCountsAndDrilldownIds() {
        var prepared=mock(PreparedStatisticsMapper.class);
        var mapper=mock(ResearchSnapshotMapper.class);
        var config=new PreparedSnapshotConfig();
        config.setSourceNamesJson("{\"GSA\":\"GSA\"}");
        config.setTopicScopesJson("{\"PROJECT|项目\":[],\"SEQUENCE|序列\":[\"GSA\"],\"VARIATION|变异\":[]}");
        when(prepared.config(1L)).thenReturn(config);
        var snapshot=new ResearchSnapshotDetail();
        snapshot.setStatus("SUCCESS");
        when(mapper.selectDetail(1L)).thenReturn(snapshot);
        when(mapper.selectDimensions(eq(1L),anyString(),anyString(),anyInt())).thenReturn(List.of());
        var sequence=new ResearchSnapshotDimension();
        sequence.setId(7L);sequence.setDimensionKey("SEQUENCE");sequence.setDisplayName("序列");sequence.setPublicationCount(12);
        when(mapper.selectDimensions(1L,"TOTAL","TOPIC",1000)).thenReturn(List.of(sequence));
        var service=new PreparedSnapshotService(null,prepared,mapper,null,null,new ObjectMapper());
        try {
            var topics=service.result(1L,1).dimensions().get("TOPIC");
            assertEquals(List.of("PROJECT","SEQUENCE","VARIATION"),topics.stream().map(ResearchSnapshotDimension::getDimensionKey).toList());
            assertEquals(List.of(0,12,0),topics.stream().map(ResearchSnapshotDimension::getPublicationCount).toList());
            assertEquals(7L,topics.get(1).getId());
            assertNull(topics.get(0).getId());
            assertTrue(service.databaseResult(1L,"GSA",1).dimensions().get("TOPIC").isEmpty());
            verify(mapper,never()).selectEnabledTopicCategories(); // History uses the frozen taxonomy.
        } finally { service.shutdown(); }
    }
}

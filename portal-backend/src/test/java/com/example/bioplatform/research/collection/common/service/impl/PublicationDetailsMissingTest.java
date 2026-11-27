package com.example.bioplatform.research.collection.common.service.impl;

import com.example.bioplatform.research.collection.common.service.relation.*;
import com.example.bioplatform.research.collection.openlb.service.relation.OpenLbPublicationDetailsClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class PublicationDetailsMissingTest {
    private final RelationPageClient pages=mock(RelationPageClient.class);
    private OpenLbPublicationDetailsClient client() {
        var client=new OpenLbPublicationDetailsClient(pages,new ObjectMapper());
        ReflectionTestUtils.setField(client,"endpoint","https://fixture/");
        return client;
    }
    private void emptyOpenLb() throws Exception {
        when(pages.get("https://fixture/25431773")).thenReturn("{\"code\":200,\"result\":{\"data\":[]}}");
    }
    @Test void onlyValidEmptyResponsesProduceTheSkippableException() throws Exception {
        emptyOpenLb();
        when(pages.get(contains("efetch.fcgi"))).thenReturn("<PubmedArticleSet/>");
        assertThrows(PublicationNotFoundException.class,()->client().read("25431773"));
    }
    @Test void malformedOpenLbSchemaIsNotMistakenForNoData() throws Exception {
        when(pages.get(anyString())).thenReturn("{\"code\":200,\"result\":{\"data\":null}}");
        var error=assertThrows(IOException.class,()->client().read("25431773"));
        assertFalse(error instanceof PublicationNotFoundException);
        verify(pages,times(1)).get(anyString());
    }
    @Test void networkAndRateLimitFailuresDoNotBecomeSkips() throws Exception {
        when(pages.get(anyString())).thenThrow(new IOException("HTTP 429"));
        var error=assertThrows(IOException.class,()->client().read("25431773"));
        assertFalse(error instanceof PublicationNotFoundException);
        verify(pages,times(1)).get(anyString());
    }
    @Test void pubmedErrorAndHtmlResponsesDoNotBecomeSkips() throws Exception {
        emptyOpenLb();
        when(pages.get(contains("efetch.fcgi"))).thenReturn("<PubmedArticleSet><ERROR>API rate limit exceeded</ERROR></PubmedArticleSet>");
        assertFalse(assertThrows(IOException.class,()->client().read("25431773")) instanceof PublicationNotFoundException);
        when(pages.get(contains("efetch.fcgi"))).thenReturn("<html><body>Service unavailable</body></html>");
        assertFalse(assertThrows(IOException.class,()->client().read("25431773")) instanceof PublicationNotFoundException);
    }
    @Test void aPubmedMatchStillReturnsNormalMetadata() throws Exception {
        emptyOpenLb();
        when(pages.get(contains("efetch.fcgi"))).thenReturn("<PubmedArticleSet><PubmedArticle><MedlineCitation><PMID>25431773</PMID><Article><ArticleTitle>Found</ArticleTitle></Article></MedlineCitation></PubmedArticle></PubmedArticleSet>");
        assertEquals("Found",client().read("25431773").path("title").asText());
    }
}

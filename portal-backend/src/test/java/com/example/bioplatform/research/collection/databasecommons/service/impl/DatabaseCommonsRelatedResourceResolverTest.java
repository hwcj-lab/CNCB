package com.example.bioplatform.research.collection.databasecommons.service.impl;

import com.example.bioplatform.research.collection.common.service.relation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class DatabaseCommonsRelatedResourceResolverTest {
    private final ObjectMapper json = new ObjectMapper();
    private static final String URL = "https://ngdc.cncb.ac.cn/databasecommons/database/id/1802";
    static class Pages extends RelationPageClient {
        String payload, requested;
        String page = "<table id='pubTable'><tr><td><a href='https://pubmed.ncbi.nlm.nih.gov/21071426/'>PMID</a></td></tr></table>";
        final List<String> requests = new ArrayList<>();
        Pages(String payload) { this.payload = payload; }
        @Override public String get(String url) { requested = url; requests.add(url); return url.contains("/database/id/") ? page : payload; }
    }
    @Test void onlyMatchingIdIsIncludedPaddedAccessionsNormalizeAndNestedCitationsAreExcluded() throws Exception {
        try (var input = getClass().getResourceAsStream("/research/databasecommons-1802.json")) {
            var pages = new Pages(new String(Objects.requireNonNull(input).readAllBytes(), StandardCharsets.UTF_8));
            var resolver = new DatabaseCommonsRelatedResourceResolver(json, pages);
            var result = resolver.findRelatedPublications("DBC001802", URL);
            assertEquals("https://ngdc.cncb.ac.cn/databasecommons/api/biodb/cited?dbId=1802", pages.requested);
            assertEquals(List.of("27899658", "29036542", "30365034", "31702008", "33175170", "34718731", "30716410", "36420893", "38018256", "39530327", "41359036"), result.pmids());
            assertEquals("example-grant", result.grants().get(0).asText());
            assertFalse(result.pmids().contains("88888888"));
            assertFalse(result.pmids().contains("99999999"));
            var strategy = new CncbBanksDiscoveryStrategy(new RelatedResourceResolverFactory(List.of(resolver)), pages, json);
            var found = strategy.discover(new RelationDiscoveryInput("DATABASECOMMONS", "PMID", "41359036", "41359036", "[{\"db\":\"Database Commons\",\"accession\":\"DBC001802\",\"link\":\"" + URL + "\"}]", null, false), () -> {});
            assertEquals(10, found.pmids().size());
            assertFalse(found.pmids().containsKey("41359036"));
            assertTrue(found.pmids().values().stream().allMatch(URL::equals));
        }
    }
    @Test void missingSelfIsValidButUnexpectedPublicationsStillFails() throws Exception {
        var pages = new Pages("[{\"dbId\":42,\"publicationList\":[{\"id\":1802,\"pmid\":\"99999999\"}]}]");
        var resolver = new DatabaseCommonsRelatedResourceResolver(json, pages);
        assertEquals(List.of("21071426"),resolver.findRelatedPublications("DBC001802", URL).pmids());
        pages.payload = "{\"dbId\":1802}";
        assertThrows(IllegalStateException.class, () -> resolver.findRelatedPublications("DBC001802", URL));
    }
    @Test void wrapperLegacyProfileEmptyListsAndIdentifierValidation() throws Exception {
        var pages = new Pages("{\"data\":[{\"id\":1802,\"publicationList\":[]}]}");
        var resolver = new DatabaseCommonsRelatedResourceResolver(json, pages);
        assertTrue(resolver.findRelatedPublications(null, URL).pmids().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> resolver.findRelatedPublications("DBC000309", URL));
        assertThrows(IllegalArgumentException.class, () -> resolver.findRelatedPublications("invalid", "https://example.org/1802"));
        pages.payload = "{\"dbId\":1802,\"publicationList\":[{\"pmid\":null},{\"pmid\":\"null\"},{\"pmid\":\"0\"},{\"pmid\":\"39530327\"},{\"pubmedId\":\"39530327\"}]}";
        assertEquals(List.of("39530327"), resolver.findRelatedPublications("1802", URL).pmids());
    }

    @Test void absent614FallsBackToItsOwnPageInsteadOfOtherDatabases() throws Exception {
        var pages = new Pages("[]");
        try (var input = getClass().getResourceAsStream("/research/databasecommons-614-cited.json")) {
            pages.payload = new String(Objects.requireNonNull(input).readAllBytes(), StandardCharsets.UTF_8);
        }
        var result = new DatabaseCommonsRelatedResourceResolver(json, pages).findRelatedPublications("DBC000614", "https://ngdc.cncb.ac.cn/databasecommons/database/id/614");
        assertEquals(List.of("21071426"),result.pmids());
        assertTrue(result.grants().isEmpty());
        assertEquals(List.of("https://ngdc.cncb.ac.cn/databasecommons/api/biodb/cited?dbId=614", "https://ngdc.cncb.ac.cn/databasecommons/database/id/614"), pages.requests);
    }

    @Test void validEmptyListsCompleteButMalformedOrErrorResponsesDoNot() throws Exception {
        var pages = new Pages("[]");
        pages.page = "<table id='pubTable'></table>";
        var resolver = new DatabaseCommonsRelatedResourceResolver(json, pages);
        assertTrue(resolver.findRelatedPublications("614", null).pmids().isEmpty());
        pages.payload = "[{\"dbId\":349,\"publicationList\":[]}]";
        assertTrue(resolver.findRelatedPublications("614", null).pmids().isEmpty());
        pages.payload = "{\"message\":\"ERROR\",\"data\":[]}";
        assertThrows(IllegalStateException.class, () -> resolver.findRelatedPublications("614", null));
        pages.payload = "[{\"dbId\":614,\"publicationList\":null}]";
        assertThrows(IllegalStateException.class, () -> resolver.findRelatedPublications("614", null));
    }

    @Test void aResponseWarmsTheIdIndexWithoutMixingLists() throws Exception {
        var pages = new Pages("[{\"dbId\":1802,\"publicationList\":[{\"pmid\":\"41359036\"}]},{\"dbId\":614,\"publicationList\":[{\"pmid\":\"21071426\"}]}]");
        var resolver = new DatabaseCommonsRelatedResourceResolver(json, pages);
        try (var cache = RelationRunCache.open()) {
            assertEquals(List.of("41359036"), resolver.findRelatedPublications("1802", null).pmids());
            assertEquals(List.of("21071426"), resolver.findRelatedPublications("614", null).pmids());
            assertEquals(1, pages.requests.size());
        }
    }
}

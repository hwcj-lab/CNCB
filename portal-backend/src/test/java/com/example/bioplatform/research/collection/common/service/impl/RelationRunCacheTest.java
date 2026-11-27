package com.example.bioplatform.research.collection.common.service.impl;

import com.example.bioplatform.research.collection.common.service.RelatedResourceData;
import com.example.bioplatform.research.collection.common.service.relation.RelationRunCache;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class RelationRunCacheTest {
    @Test void cachesUnfilteredResultsAndCopiesMutablePayloads() throws Exception {
        var calls = new AtomicInteger();
        var json = new ObjectMapper();
        RelationRunCache.Loader loader = () -> {
            calls.incrementAndGet();
            return new RelatedResourceData(List.of("41359036", "39530327"), json.createArrayNode().add("grant"));
        };
        try (var cache = RelationRunCache.open()) {
            var first = RelationRunCache.resource("bank1802", loader);
            ((com.fasterxml.jackson.databind.node.ArrayNode) first.grants()).removeAll();
            var second = RelationRunCache.resource("bank1802", loader);
            assertEquals(1, calls.get());
            assertEquals(1, second.grants().size());
            assertEquals(List.of("39530327"), second.pmids().stream().filter(p -> !p.equals("41359036")).toList());
            assertEquals(List.of("41359036"), second.pmids().stream().filter(p -> !p.equals("39530327")).toList());
            RelationRunCache.written("GSA", "41359036");
            assertTrue(RelationRunCache.wasWritten("GSA", "41359036"));
            assertFalse(RelationRunCache.wasWritten("GENBASE", "41359036"));
        }
        try (var cache = RelationRunCache.open()) {
            RelationRunCache.resource("bank1802", loader);
            assertFalse(RelationRunCache.wasWritten("GSA", "41359036"));
        }
        assertEquals(2, calls.get());
    }

    @Test void failuresAreRetriedAndOldEntriesAreEvicted() throws Exception {
        try (var cache = RelationRunCache.open()) {
            assertThrows(IllegalStateException.class, () -> RelationRunCache.resource("failed", () -> { throw new IllegalStateException(); }));
            assertEquals(List.of("1"), RelationRunCache.resource("failed", () -> new RelatedResourceData(List.of("1"), null)).pmids());
            for (int i = 0; i < 128; i++)
                RelationRunCache.resource("key" + i, () -> new RelatedResourceData(List.of("2"), null));
            assertEquals(List.of("3"), RelationRunCache.resource("failed", () -> new RelatedResourceData(List.of("3"), null)).pmids());
        }
    }
}

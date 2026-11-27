package com.example.bioplatform.research.collection.common.service.relation;

import com.example.bioplatform.research.collection.common.service.RelatedResourceData;

import java.util.*;

/**
 * Bounded worker-local reuse; discarded on completion, stop or failure.
 */
public final class RelationRunCache implements AutoCloseable {
    private static final ThreadLocal<RelationRunCache> CURRENT = new ThreadLocal<>();
    private final Map<String, RelatedResourceData> resources = new LinkedHashMap<>(16, .75f, true);
    private final Set<String> written = new LinkedHashSet<>();
    private int characters;

    private RelationRunCache() {
    }

    public static RelationRunCache open() {
        if (CURRENT.get() != null) throw new IllegalStateException("Relation cache scope already active");
        var cache = new RelationRunCache();
        CURRENT.set(cache);
        return cache;
    }

    @FunctionalInterface
    public interface Loader {
        RelatedResourceData load() throws Exception;
    }

    public static RelatedResourceData resource(String key, Loader loader) throws Exception {
        var cache = CURRENT.get();
        if (cache == null) return loader.load();
        var found = cache.resources.get(key);
        if (found != null) return copy(found);
        var result = loader.load(); // Never cache failures.
        remember(key, result);
        return result;
    }

    public static void remember(String key, RelatedResourceData result) {
        var cache = CURRENT.get();
        if (cache == null || cache.resources.containsKey(key)) return;
        int size = size(result);
        if (size <= 500_000) {
            while (!cache.resources.isEmpty() && (cache.resources.size() >= 128 || cache.characters + size > 2_000_000)) {
                var first = cache.resources.entrySet().iterator();
                cache.characters -= size(first.next().getValue());
                first.remove();
            }
            cache.resources.put(key, copy(result));
            cache.characters += size;
        }
    }

    public static boolean wasWritten(String scope, String pmid) {
        var cache = CURRENT.get();
        return cache != null && cache.written.contains(scope + ":" + pmid);
    }

    public static void written(String scope, String pmid) {
        var cache = CURRENT.get();
        if (cache == null) return;
        if (cache.written.size() >= 10_000) cache.written.remove(cache.written.iterator().next());
        cache.written.add(scope + ":" + pmid);
    }

    private static RelatedResourceData copy(RelatedResourceData value) {
        return new RelatedResourceData(List.copyOf(value.pmids()), value.grants() == null ? null : value.grants().deepCopy(),
                value.publicationRecords().stream().map(node -> (com.fasterxml.jackson.databind.JsonNode) node.deepCopy()).toList());
    }

    private static int size(RelatedResourceData value) {
        long length = value.pmids().stream().mapToLong(s -> s.length() + 32L).sum();
        if (value.grants() != null) length += value.grants().toString().length();
        for (var node : value.publicationRecords()) length += node.toString().length();
        return (int) Math.min(Integer.MAX_VALUE, length);
    }

    @Override
    public void close() {
        CURRENT.remove();
    }
}

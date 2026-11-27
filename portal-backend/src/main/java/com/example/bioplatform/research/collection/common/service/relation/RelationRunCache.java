package com.example.bioplatform.research.collection.common.service.relation;

import com.example.bioplatform.research.collection.common.service.RelatedResourceData;

import java.util.*;

/**
 * 单轮工作线程的有界缓存：复用资源发现、成功写入和明确缺失记录；结束、停止或失败后释放。
 */
public final class RelationRunCache implements AutoCloseable {
    private static final ThreadLocal<RelationRunCache> CURRENT = new ThreadLocal<>();
    private final Map<String, RelatedResourceData> resources = new LinkedHashMap<>(16, .75f, true);
    private final Set<String> written = new LinkedHashSet<>();
    private final Set<String> missing = new LinkedHashSet<>();
    private int characters;

    private RelationRunCache() {
    }

    /**
     * 建立当前工作线程的一轮缓存作用域；作业结束或失败必须关闭，避免跨轮使用陈旧结果。
     */
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

    /**
     * 缓存未排除根文献的资源发现结果；返回副本，防止某根文献过滤自身时污染其他根的结果。
     */
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

    /**
     * 记录当前来源已成功写入的PMID；其他根仍保存自己的关联边，但不重复补正文。
     */
    public static void written(String scope, String pmid) {
        var cache = CURRENT.get();
        if (cache == null) return;
        if (cache.written.size() >= 10_000) cache.written.remove(cache.written.iterator().next());
        cache.written.add(scope + ":" + pmid);
    }
    public static boolean isMissing(String pmid) {
        var cache = CURRENT.get();
        return cache != null && cache.missing.contains(pmid);
    }
    /**
     * 本轮有界记录已确认无数据的PMID；避免多个根反复请求同一缺失目标。
     */
    public static void missing(String pmid) {
        var cache = CURRENT.get();
        if (cache == null) return;
        if (cache.missing.size() >= 10_000) cache.missing.remove(cache.missing.iterator().next());
        cache.missing.add(pmid);
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

    /**
     * 释放线程本地缓存；不会删除数据库中的任务、文献或关联关系。
     */
    @Override
    public void close() {
        CURRENT.remove();
    }
}

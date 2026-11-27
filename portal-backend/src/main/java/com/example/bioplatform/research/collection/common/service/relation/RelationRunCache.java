package com.example.bioplatform.research.collection.common.service.relation;

import com.example.bioplatform.research.collection.common.service.RelatedResourceData;

import java.util.*;

/**
 * 单轮工作线程的有界缓存：复用资源发现、成功写入和明确缺失记录；结束、停止或失败后释放。
 */
public final class RelationRunCache implements AutoCloseable {
    /**
     * CURRENT：当前工作线程的单轮缓存上下文，不跨作业永久共享。
     */
    private static final ThreadLocal<RelationRunCache> CURRENT = new ThreadLocal<>();
    /**
     * resources：资源结果、资源来源或资源缓存，类型决定具体用途。
     */
    private final Map<String, RelatedResourceData> resources = new LinkedHashMap<>(16, .75f, true);
    /**
     * written：本轮已成功写入的来源文献标识集合。
     */
    private final Set<String> written = new LinkedHashSet<>();
    /**
     * missing：本轮确认无匹配数据的标识集合，或无结果错误构造用途。
     */
    private final Set<String> missing = new LinkedHashSet<>();
    /**
     * characters：缓存中已计入的字符容量估计值。
     */
    private int characters;

    /**
     * 限制本工具或内部状态对象的实例化入口，具体创建方式由所属类控制。
     */
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

    /**
     * 成功结果可被本轮缓存复用的资源加载回调。
     */
    @FunctionalInterface
    public interface Loader {
        /**
         * 执行约定的数据加载回调，具体读取来源由注入实现决定。
         * @return 按上述规则计算或读取的RelatedResourceData结果
         */
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

    /**
     * 在当前工作线程的有界缓存中保存成功的资源解析结果，容量超限时淘汰旧项。
     * @param key 当前对象的业务匹配键
     * @param result 当前查询或处理结果
     */
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

    /**
     * 检查当前来源的目标PMID在本轮是否已成功写入，避免重复拉取同一正文。
     * @param scope 统计范围；TOTAL表示总统计，其余为指定来源编码
     * @param pmid 已有PubMed标识；为空时不通过标题猜补
     * @return true表示满足上述条件，false表示不满足
     */
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
    /**
     * 检查本轮是否已确认该PMID无数据；明确缺失可复用，网络失败不会被记录为缺失。
     * @param pmid 已有PubMed标识；为空时不通过标题猜补
     * @return true表示满足上述条件，false表示不满足
     */
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

    /**
     * 复制缓存中的可变JSON载荷，防止调用方修改影响其他根文献。
     * @param value 待校验、转换或合并的值
     * @return 按上述规则计算或读取的RelatedResourceData结果
     */
    private static RelatedResourceData copy(RelatedResourceData value) {
        return new RelatedResourceData(List.copyOf(value.pmids()), value.grants() == null ? null : value.grants().deepCopy(),
                value.publicationRecords().stream().map(node -> (com.fasterxml.jackson.databind.JsonNode) node.deepCopy()).toList());
    }

    /**
     * 估计缓存内容的字符容量，用于控制单轮内存上限。
     * @param value 待校验、转换或合并的值
     * @return 按上述规则计算或读取的int结果
     */
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

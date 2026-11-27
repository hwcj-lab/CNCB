package com.example.bioplatform.research.statistics.prepared.service;

import com.example.bioplatform.common.BusinessException;
import com.example.bioplatform.research.collection.common.vo.ResearchPublicationSourceOption;
import com.example.bioplatform.research.integration.service.impl.ResearchPublicationSourceRegistry;
import com.example.bioplatform.research.preparation.service.PreparationService;
import com.example.bioplatform.research.preparation.service.PreparedMetadataParser;
import com.example.bioplatform.research.statistics.dto.ResearchSnapshotBuildRequest;
import com.example.bioplatform.research.statistics.dto.ResearchSnapshotCreate;
import com.example.bioplatform.research.statistics.mapper.ResearchSnapshotMapper;
import com.example.bioplatform.research.statistics.prepared.mapper.PreparedStatisticsMapper;
import com.example.bioplatform.research.statistics.prepared.model.PreparedScope;
import com.example.bioplatform.research.statistics.prepared.model.PreparedSnapshotConfig;
import com.example.bioplatform.research.statistics.service.ResearchSnapshotService;
import com.example.bioplatform.research.statistics.vo.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * 默认快照实现：读取固定修订的整理表，不重新解析原始JSON。总量与分库共用算法，只改变来源范围。
 */
@Service
@RequiredArgsConstructor
public class PreparedSnapshotService implements ResearchSnapshotService {
    /**
     * preparation：独立数据整理服务。
     */
    private final PreparationService preparation;
    /**
     * prepared：新整理模式的统计持久层。
     */
    private final PreparedStatisticsMapper prepared;
    /**
     * mapper：本组件使用的持久层接口，SQL由对应XML维护。
     */
    private final ResearchSnapshotMapper mapper;
    /**
     * registry：可用文献来源的注册中心。
     */
    private final ResearchPublicationSourceRegistry registry;
    /**
     * tx：事务执行模板，保证本批数据与进度一起提交或回滚。
     */
    private final TransactionTemplate tx;
    /**
     * json：JSON解析与序列化组件。
     */
    private final ObjectMapper json;
    /**
     * running：当前进程是否正在执行对应后台任务。
     * stop：停止请求标记或用于检查停止状态的回调。
     */
    private final AtomicBoolean running = new AtomicBoolean(), stop = new AtomicBoolean();
    /**
     * executor：本组件后台任务执行器。
     */
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        var t = new Thread(r, "prepared-snapshot");
        t.setDaemon(true);
        return t;
    });
    /**
     * currentId：当前进程执行的快照ID。
     */
    private volatile Long currentId;
    /**
     * message：响应说明、当前阶段说明或错误提示。
     */
    private volatile String message = "先采集，再整理，最后生成快照";
    /**
     * TYPES：本组件支持的维度或类型白名单。
     */
    private static final List<String> TYPES = List.of("YEAR", "AUTHOR", "INSTITUTION", "FUND", "TOPIC", "DATABASE");

    /**
     * 先取得可发布整理修订，再冻结来源名称、分类映射和统计选项，创建BUILDING快照并异步聚合。
     */
    @Override
    public synchronized ResearchSnapshotStatus startBuild(ResearchSnapshotBuildRequest request) {
        long revision = preparation.requireReady();
        if (!running.compareAndSet(false, true)) throw conflict("已有快照正在生成");
        try {
            ResearchSnapshotCreate create = tx.execute(s -> {
                if (prepared.lock() != 1) throw conflict("其他进程正在创建快照");
                try {
                    if (mapper.countBuildingSnapshots() > 0) throw conflict("存在未结束快照，请确认旧进程停止后恢复");
                    var sources = registry.options();
                    var row = new ResearchSnapshotCreate();
                    row.setVersionNo(mapper.selectNextVersionNo());
                    row.setSnapshotName(request != null && request.getName() != null && !request.getName().isBlank() ? request.getName().trim() : "科研统计快照 v" + row.getVersionNo());
                    row.setIncludeRelated(request != null && Boolean.TRUE.equals(request.getIncludeRelated()));
                    row.setIncludeResources(request != null && Boolean.TRUE.equals(request.getIncludeResources()));
                    row.setSourceCount(sources.size());
                    row.setSourceCodes(sources.stream().map(ResearchPublicationSourceOption::code).collect(Collectors.joining(",")));
                    row.setStartedTime(LocalDateTime.now());
                    mapper.insertSnapshot(row);
                    Map<String, String> names = new LinkedHashMap<>();
                    sources.forEach(o -> names.put(o.code(), o.name()));
                    var cfg = new PreparedSnapshotConfig();
                    cfg.setSnapshotId(row.getId());
                    cfg.setPreparationRevision(revision);
                    cfg.setSourceNamesJson(encode(names));
                    cfg.setTopicScopesJson(encode(topicScopes(names)));
                    prepared.saveConfig(cfg);
                    return row;
                } finally {
                    prepared.unlock();
                }
            });
            stop.set(false);
            currentId = create.getId();
            message = "正在从整理表聚合";
            executor.submit(() -> build(create.getId()));
            return status();
        } catch (RuntimeException ex) {
            running.set(false);
            throw ex;
        }
    }

    /**
     * 按已启用分类和已确认数据库映射构造分类范围；无匹配来源的分类也保留，后续显示0。
     */
    private Map<String, List<String>> topicScopes(Map<String, String> names) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        var mappings = mapper.selectConfirmedDatabaseTopics();
        for (var topic : mapper.selectEnabledTopicCategories()) {
            var codes = names.keySet().stream().filter(code -> mappings.stream().anyMatch(m -> topic.getTopicCode().equals(m.getTopicCode()) && (normalize(code).equals(normalize(m.getDatabaseCode())) || normalize(names.get(code)).equals(normalize(m.getDatabaseCode()))))).toList();
            result.put(topic.getTopicCode() + "|" + topic.getTopicName(), codes);
        }
        return result;
    }

    /**
     * 先计算全局覆盖量，再依次计算TOTAL及各来源的五类维度；全部成功后才回写汇总量并发布快照。
     */
    private void build(long id) {
        try {
            var snapshot = detail(id);
            var cfg = config(id);
            var q = scope(snapshot, cfg, "TOTAL");
            int total = exact(prepared.count(q));
            long resources = prepared.resources(q);
            int year = exact(prepared.coverage(q, PreparedMetadataParser.table("YEAR"))), author = exact(prepared.coverage(q, PreparedMetadataParser.table("AUTHOR"))), institution = exact(prepared.coverage(q, PreparedMetadataParser.table("INSTITUTION"))), fund = exact(prepared.coverage(q, PreparedMetadataParser.table("FUND")));
            List<String> scopes = new ArrayList<>();
            scopes.add("TOTAL");
            scopes.addAll(q.getSources());
            for (String code : scopes) {
                checkStopped();
                q = scope(snapshot, cfg, code);
                message = "聚合 " + code;
                for (String type : List.of("YEAR", "AUTHOR", "INSTITUTION", "FUND", "TOPIC")) {
                    checkStopped();
                    if ("TOTAL".equals(code) && "TOPIC".equals(type)) continue;
                    q.setType(type);
                    var ranked = prepared.rank(q, PreparedMetadataParser.table(type));
                    for (var row : ranked) {
                        row.setScopeCode(code);
                        row.setDimensionType(type);
                    }
                    for (int offset = 0;
                         offset < ranked.size();
                         offset += 50)
                        prepared.insertDimensions(id, ranked.subList(offset, Math.min(offset + 50, ranked.size())));
                }
            }
            q = scope(snapshot, cfg, "TOTAL");
            for (var entry : names(cfg).entrySet()) {
                checkStopped();
                q.setScope(entry.getKey());
                save(id, "TOTAL", "DATABASE", entry.getKey(), entry.getValue(), exact(prepared.count(q)));
            }
            Set<String> topicSources = new LinkedHashSet<>();
            for (var entry : topics(cfg).entrySet()) {
                checkStopped();
                int count = 0;
                for (String code : entry.getValue()) {
                    q.setScope(code);
                    count = Math.addExact(count, exact(prepared.count(q)));
                    topicSources.add(code);
                }
                String[] parts = entry.getKey().split("\\|", 2);
                save(id, "TOTAL", "TOPIC", parts[0], parts.length > 1 ? parts[1] : parts[0], count);
            }
            q = scope(snapshot, cfg, "TOTAL");
            q.setSourceFilter(new ArrayList<>(topicSources));
            int topic = exact(prepared.count(q));
            checkStopped();
            // BUILDING期间汇总字段仍为初始值；所有范围聚合成功后才一次性回写并发布。
            mapper.markSuccess(id, total, resources, year, topic, author, institution, fund);
            message = "快照生成完成";
        } catch (Stopped ex) {
            mapper.deleteSnapshotDimensions(id);
            mapper.markStopped(id, "已停止");
            message = "快照已停止";
        } catch (Exception ex) {
            mapper.deleteSnapshotDimensions(id);
            String error = Objects.toString(ex.getMessage(), ex.getClass().getSimpleName());
            mapper.markFailed(id, error.substring(0, Math.min(1900, error.length())));
            message = "快照失败：" + error.substring(0, Math.min(300, error.length()));
        } finally {
            running.set(false);
            stop.set(false);
        }
    }

    /**
     * 保存单个年度、来源或总分类统计点；零计数分类也写入，不在此处过滤。
     */
    private void save(long id, String scope, String type, String key, String label, int count) {
        var row = new ResearchSnapshotDimension();
        row.setScopeCode(scope);
        row.setDimensionType(type);
        row.setDimensionKey(key);
        row.setDisplayName(label);
        row.setPublicationCount(count);
        prepared.insertDimension(id, row);
    }

    /**
     * 请求当前快照构建在检查点停止，避免发布仅生成一部分的统计结果。
     * @return 按上述规则计算或读取的ResearchSnapshotStatus结果
     */
    @Override
    public ResearchSnapshotStatus stopBuild() {
        if (running.get()) stop.set(true);
        return status();
    }

    /**
     * 清理已中断且未发布的构建状态；不会把部分统计结果直接标记为成功。
     */
    public synchronized ResearchSnapshotStatus recover() {
        if (running.get()) throw conflict("当前进程仍在生成快照");
        var building = mapper.selectAnyBuildingSnapshot();
        if (building != null) {
            mapper.deleteSnapshotDimensions(building.getId());
            mapper.markFailed(building.getId(), "进程中断，已回收未发布版本");
        }
        return status();
    }

    /**
     * 组合当前进程运行标记及数据库持久化进度，供前端轮询；不启动新任务。
     */
    @Override
    public ResearchSnapshotStatus status() {
        var building = mapper.selectAnyBuildingSnapshot();
        return new ResearchSnapshotStatus(running.get() || building != null, message, building != null ? building : currentId == null ? null : mapper.selectDetail(currentId), latest());
    }

    /**
     * 读取当前已注册并启用的来源编码及展示名称，前端不维护固定数据库列表。
     * @return 按上述规则计算或读取的List<ResearchPublicationSourceOption>结果
     */
    @Override
    public List<ResearchPublicationSourceOption> sourceOptions() {
        return registry.options();
    }

    /**
     * 读取可查询的历史快照版本列表，具体格式由当前统计实现限定。
     * @return 按上述规则计算或读取的List<ResearchSnapshotDetail>结果
     */
    @Override
    public List<ResearchSnapshotDetail> versions() {
        return prepared.versions();
    }

    /**
     * 返回最近成功发布的快照；正在构建或失败的版本不作为可用最新结果。
     * @return 按上述规则计算或读取的ResearchSnapshotDetail结果
     */
    @Override
    public ResearchSnapshotDetail latest() {
        return prepared.latestSuccess();
    }

    /**
     * 按标识读取当前资源或快照详情；不存在时由业务异常明确告知调用方。
     * @param id 本对象或持久化记录的主键标识
     * @return 按上述规则计算或读取的ResearchSnapshotDetail结果
     */
    @Override
    public ResearchSnapshotDetail detail(long id) {
        config(id);
        var row = mapper.selectDetail(id);
        if (row == null) throw missing("快照不存在");
        return row;
    }

    /**
     * 读取最近成功快照的统计结果，没有可用版本时返回明确提示。
     * @param limit 结果条数上限
     * @return 按上述规则计算或读取的ResearchSnapshotResult结果
     */
    @Override
    public ResearchSnapshotResult latestResult(int limit) {
        var latest = latest();
        if (latest == null) throw missing("尚无整理后生成的成功快照，请先整理数据");
        return result(latest.getId(), limit);
    }

    /**
     * 按指定快照读取总统计结果，并根据请求限制排名展示数量。
     * @param id 本对象或持久化记录的主键标识
     * @param limit 结果条数上限
     * @return 按上述规则计算或读取的ResearchSnapshotResult结果
     */
    @Override
    public ResearchSnapshotResult result(long id, int limit) {
        return scopedResult(id, "TOTAL", limit);
    }

    /**
     * 读取指定快照、指定数据库来源的分库统计；不将其他来源结果合并进该范围。
     * @param id 本对象或持久化记录的主键标识
     * @param code 当前来源、策略或业务响应使用的编码
     * @param limit 结果条数上限
     * @return 按上述规则计算或读取的ResearchSnapshotResult结果
     */
    @Override
    public ResearchSnapshotResult databaseResult(long id, String code, int limit) {
        String scope = code.toUpperCase(Locale.ROOT);
        if (!names(config(id)).containsKey(scope)) throw missing("该快照没有此来源");
        return scopedResult(id, scope, limit);
    }

    /**
     * 读取已发布快照的图表数据；总分类不受Top N限制，其他排名最多读取50项。
     */
    private ResearchSnapshotResult scopedResult(long id, String code, int limit) {
        var snapshot = detail(id);
        if (!"SUCCESS".equals(snapshot.getStatus())) throw conflict("该快照尚未成功发布");
        Map<String, List<ResearchSnapshotDimension>> result = new LinkedHashMap<>();
        for (String type : TYPES)
            result.put(type, mapper.selectDimensions(id, code, type, Set.of("YEAR", "DATABASE").contains(type) || ("TOTAL".equals(code) && "TOPIC".equals(type)) ? 1000 : Math.min(50, Math.max(1, limit))));
        if ("TOTAL".equals(code)) result.put("TOPIC", completeTopicCategories(id, result.get("TOPIC")));
        return new ResearchSnapshotResult(snapshot, result);
    }

    /**
     * Keep the frozen taxonomy visible even when a category has no source or no matching records.
     */
    private List<ResearchSnapshotDimension> completeTopicCategories(long id, List<ResearchSnapshotDimension> stored) {
        Map<String, ResearchSnapshotDimension> byCode = new LinkedHashMap<>();
        for (var row : stored) byCode.put(row.getDimensionKey(), row);
        List<ResearchSnapshotDimension> result = new ArrayList<>();
        for (String category : topics(config(id)).keySet()) {
            String[] parts = category.split("\\|", 2);
            var row = byCode.remove(parts[0]);
            if (row == null) {
                row = new ResearchSnapshotDimension();
                row.setScopeCode("TOTAL");
                row.setDimensionType("TOPIC");
                row.setDimensionKey(parts[0]);
                row.setDisplayName(parts.length > 1 ? parts[1] : parts[0]);
                row.setPublicationCount(0);
            }
            result.add(row);
        }
        result.addAll(byCode.values());
        return result;
    }

    /**
     * 返回当前接口范围内可用的数据库列表，关联采集和快照场景各自使用对应来源配置。
     * @param id 本对象或持久化记录的主键标识
     * @return 按上述规则计算或读取的List<ResearchSnapshotDatabaseOption>结果
     */
    @Override
    public List<ResearchSnapshotDatabaseOption> databases(long id) {
        detail(id);
        return mapper.selectDimensions(id, "TOTAL", "DATABASE", 1000).stream().map(d -> new ResearchSnapshotDatabaseOption(d.getDimensionKey(), d.getDisplayName(), d.getPublicationCount())).toList();
    }

    /**
     * 按快照冻结的主题映射查找该分类对应的数据库，保持历史分类范围稳定。
     * @param id 本对象或持久化记录的主键标识
     * @param topic 研究主题编码或主题维度结果
     * @return 按上述规则计算或读取的List<ResearchSnapshotDatabaseOption>结果
     */
    @Override
    public List<ResearchSnapshotDatabaseOption> topicDatabases(long id, String topic) {
        var codes = topics(config(id)).entrySet().stream().filter(e -> e.getKey().split("\\|", 2)[0].equalsIgnoreCase(topic)).flatMap(e -> e.getValue().stream()).collect(Collectors.toSet());
        return databases(id).stream().filter(d -> codes.contains(d.code())).toList();
    }

    /**
     * 读取某类统计维度结果，维度类型与展示条数由当前接口校验。
     * @param id 本对象或持久化记录的主键标识
     * @param type 当前分类、维度或唯一值类型，按调用上下文解释
     * @param limit 结果条数上限
     * @return 按上述规则计算或读取的List<ResearchSnapshotDimension>结果
     */
    @Override
    public List<ResearchSnapshotDimension> dimensions(long id, String type, int limit) {
        String t = type.toUpperCase(Locale.ROOT);
        if (!TYPES.contains(t)) throw missing("无此维度");
        return scopedResult(id, "TOTAL", limit).dimensions().get(t);
    }

    /**
     * 按维度及快照修订读取去重文献；关联成员表与document，不依赖旧候选表或旧维度穿透表。
     */
    @Override
    public ResearchSnapshotPublicationPage dimensionPublications(long id, long dimensionId, int page, int size) {
        var snapshot = detail(id);
        if (!"SUCCESS".equals(snapshot.getStatus())) throw conflict("只能查询已发布快照");
        var row = prepared.dimension(id, dimensionId);
        if (row == null) throw missing("维度不存在");
        var cfg = config(id);
        var q = scope(snapshot, cfg, row.getScopeCode());
        q.setType(row.getDimensionType());
        q.setMemberKey(row.getDimensionKey());
        String table = null;
        if ("TOTAL".equals(q.getScope()) && "TOPIC".equals(q.getType())) {
            q.setType("CATEGORY");
            q.setSourceFilter(topics(cfg).entrySet().stream().filter(e -> e.getKey().split("\\|", 2)[0].equals(row.getDimensionKey())).flatMap(e -> e.getValue().stream()).toList());
        } else if (!Set.of("YEAR", "DATABASE").contains(q.getType())) table = PreparedMetadataParser.table(q.getType());
        int p = Math.max(1, page), n = Math.max(1, Math.min(100, size));
        long count = prepared.coverage(q, table);
        var items = prepared.publications(q, table, (long) (p - 1) * n, n);
        items.forEach(r -> r.setScopeCode(row.getScopeCode()));
        return new ResearchSnapshotPublicationPage(count, p, n, items);
    }

    /**
     * 删除指定快照及本实现对应的配置或关系；不删除原始采集文献。
     * @param id 本对象或持久化记录的主键标识
     */
    @Override
    public void deleteSnapshot(long id) {
        var snapshot = detail(id);
        if ("BUILDING".equals(snapshot.getStatus())) throw conflict("先停止快照");
        tx.executeWithoutResult(s -> {
            prepared.deleteConfig(id);
            mapper.deleteSnapshot(id);
        });
    }

    /**
     * 只修改快照展示名称，不改变统计结果或快照对应的数据范围。
     * @param id 本对象或持久化记录的主键标识
     * @param name 当前对象或资源的展示名称
     * @return 按上述规则计算或读取的ResearchSnapshotDetail结果
     */
    @Override
    public ResearchSnapshotDetail renameSnapshot(long id, String name) {
        detail(id);
        if (name == null || name.isBlank() || name.trim().length() > 100) throw conflict("名称必须为1至100字");
        mapper.updateSnapshotName(id, name.trim());
        return detail(id);
    }

    /**
     * 加载快照冻结的整理输入；旧格式快照没有配置时明确拒绝，不混用旧统计口径。
     */
    private PreparedSnapshotConfig config(long id) {
        var c = prepared.config(id);
        if (c == null) throw missing("快照不存在或属于旧统计格式；旧数据保留，请从整理结果生成新快照");
        return c;
    }

    /**
     * 把快照选项转换为查询范围；TOTAL跨来源去重，指定编码只统计该来源。
     */
    private PreparedScope scope(ResearchSnapshotDetail s, PreparedSnapshotConfig c, String code) {
        var q = new PreparedScope();
        q.setRevision(c.getPreparationRevision());
        q.setSources(new ArrayList<>(names(c).keySet()));
        q.setScope(code);
        q.setIncludeRelated(Boolean.TRUE.equals(s.getIncludeRelated()));
        q.setIncludeResources(Boolean.TRUE.equals(s.getIncludeResources()));
        return q;
    }

    /**
     * 读取本快照冻结的来源编码到显示名称映射，不使用后续配置覆盖历史名称。
     * @param c 当前处理对象，具体类型见方法签名
     * @return 按上述规则计算或读取的Map<String, String>结果
     */
    private Map<String, String> names(PreparedSnapshotConfig c) {
        try {
            return json.readValue(c.getSourceNamesJson(), json.getTypeFactory().constructMapType(LinkedHashMap.class, String.class, String.class));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * 读取本快照冻结的总分类范围，保持历史分类归属稳定。
     * @param c 当前处理对象，具体类型见方法签名
     * @return 按上述规则计算或读取的Map<String, List<String>>结果
     */
    private Map<String, List<String>> topics(PreparedSnapshotConfig c) {
        try {
            return json.readValue(c.getTopicScopesJson(), new com.fasterxml.jackson.core.type.TypeReference<LinkedHashMap<String, List<String>>>() {
            });
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * 把结构化对象序列化成JSON，供配置冻结或稳定摘要使用。
     * @param v 待处理的原始值
     * @return 按上述规则计算或读取的String结果
     */
    private String encode(Object v) {
        try {
            return json.writeValueAsString(v);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * 整理当前业务字符串的空白、大小写或固定前缀，供后续匹配使用。
     * @param s 待处理的字符串或当前对象
     * @return 按上述规则计算或读取的String结果
     */
    private static String normalize(String s) {
        return s == null ? "" : s.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    /**
     * 将统计数量转换为接口规定的整数类型，超出范围时明确失败而不静默截断。
     * @param n 长度或数量上限
     * @return 按上述规则计算或读取的int结果
     */
    private static int exact(long n) {
        return Math.toIntExact(n);
    }

    /**
     * 在批次或维度边界检查人工停止标记，避免发布不完整快照。
     */
    private void checkStopped() {
        if (stop.get()) throw new Stopped();
    }

    /**
     * 快照人工停止时使用的内部控制流异常，不应解释为上游服务故障。
     */
    private static class Stopped extends RuntimeException {
    }

    /**
     * 构造带稳定错误码的业务冲突异常，交由全局异常处理器统一返回。
     * @param s 待处理的字符串或当前对象
     * @return 按上述规则计算或读取的BusinessException结果
     */
    private static BusinessException conflict(String s) {
        return new BusinessException(HttpStatus.CONFLICT, "SNAPSHOT_NOT_READY", s);
    }

    /**
     * 构造当前查询对象不存在的业务异常，区别于空列表和上游服务故障。
     * @param s 待处理的字符串或当前对象
     * @return 按上述规则计算或读取的BusinessException结果
     */
    private static BusinessException missing(String s) {
        return new BusinessException(HttpStatus.NOT_FOUND, "SNAPSHOT_NOT_FOUND", s);
    }

    /**
     * 应用退出时通知工作线程停止并等待收尾，超时后中断；已提交的数据库断点保留。
     */
    @PreDestroy
    public void shutdown() {
        stop.set(true);
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) executor.shutdownNow();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }
}

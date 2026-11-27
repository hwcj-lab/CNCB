package com.example.bioplatform.research.preparation.service;

import com.example.bioplatform.common.BusinessException;
import com.example.bioplatform.research.collection.common.dto.SourcePublicationRecord;
import com.example.bioplatform.research.collection.common.service.SourceMembership;
import com.example.bioplatform.research.collection.openlb.mapper.OpenLbRoutingMapper;
import com.example.bioplatform.research.common.FundAgencyParser;
import com.example.bioplatform.research.common.PublicationLinks;
import com.example.bioplatform.research.common.PublicationValues;
import com.example.bioplatform.research.integration.service.impl.ResearchPublicationSourceRegistry;
import com.example.bioplatform.research.preparation.mapper.PreparationMapper;
import com.example.bioplatform.research.preparation.model.PreparationJob;
import com.example.bioplatform.research.preparation.model.PreparationRequest;
import com.example.bioplatform.research.preparation.model.PreparationSourceCursor;
import com.example.bioplatform.research.preparation.model.PreparedDocument;
import com.example.bioplatform.research.statistics.mapper.ResearchSnapshotMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 独立本地整理服务：读取注册来源，分批维护版本化document及五类成员表。采集不会自动调用；成功后供快照读取。
 */
@Service
public class PreparationService {
    /**
     * mapper：本组件使用的持久层接口，SQL由对应XML维护。
     */
    private final PreparationMapper mapper;
    /**
     * registry：可用文献来源的注册中心。
     */
    private final ResearchPublicationSourceRegistry registry;
    /**
     * dictionaries：读取分类与基金名称字典的持久层。
     */
    private final ResearchSnapshotMapper dictionaries;
    /**
     * routing：OpenLB来源分流组件。
     */
    private final OpenLbRoutingMapper routing;
    /**
     * parser：当前组件使用的来源记录或元数据解析器。
     */
    private final PreparedMetadataParser parser;
    /**
     * membership：根据明确资源路由判断本库归属的组件。
     */
    private final SourceMembership membership;
    /**
     * json：JSON解析与序列化组件。
     */
    private final ObjectMapper json;
    /**
     * tx：事务执行模板，保证本批数据与进度一起提交或回滚。
     */
    private final TransactionTemplate tx;
    /**
     * executor：本组件后台任务执行器。
     */
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        var t = new Thread(r, "research-preparation");
        t.setDaemon(true);
        return t;
    });
    /**
     * running：当前进程是否正在执行对应后台任务。
     * stop：停止请求标记或用于检查停止状态的回调。
     */
    private final AtomicBoolean running = new AtomicBoolean(), stop = new AtomicBoolean();

    /**
     * 构造PreparationService，注入本组件所需依赖并建立初始状态。
     * @param mapper 本组件使用的持久层接口，SQL由对应XML维护
     * @param registry 可用文献来源的注册中心
     * @param dictionaries 读取分类与基金名称字典的持久层
     * @param routing OpenLB来源分流组件
     * @param parser 当前组件使用的来源记录或元数据解析器
     * @param membership 根据明确资源路由判断本库归属的组件
     * @param json JSON解析与序列化组件
     * @param tx 事务执行模板，保证本批数据与进度一起提交或回滚
     */
    public PreparationService(PreparationMapper mapper, ResearchPublicationSourceRegistry registry, ResearchSnapshotMapper dictionaries, OpenLbRoutingMapper routing, PreparedMetadataParser parser, SourceMembership membership, ObjectMapper json, TransactionTemplate tx) {
        this.mapper = mapper;
        this.registry = registry;
        this.dictionaries = dictionaries;
        this.routing = routing;
        this.parser = parser;
        this.membership = membership;
        this.json = json;
        this.tx = tx;
    }

    /**
     * overlapSeconds：增量扫描向前回退的重叠秒数，用于覆盖边界更新。
     */
    @org.springframework.beans.factory.annotation.Value("${app.research-preparation.overlap-seconds:5}")
    private long overlapSeconds = 5;

    /**
     * 启动全量扫描或强制重解析；失败、停止任务优先续传原任务，不能静默切换来源范围。
     */
    public synchronized Map<String, Object> start(PreparationRequest request, boolean rebuild) {
        return begin(request, rebuild ? "REBUILD" : "FULL");
    }

    /**
     * 按各来源上次成功时间边界启动增量整理；无可靠基线的来源退回全量扫描。
     */
    public synchronized Map<String, Object> incremental(PreparationRequest request) {
        return begin(request, "INCREMENTAL");
    }

    /**
     * 统一校验运行互斥与来源列表，在事务内建立任务及扫描计划，再提交后台线程执行。
     */
    private Map<String, Object> begin(PreparationRequest request, String mode) {
        if (running.get()) throw conflict("整理正在运行");
        PreparationJob old = mapper.latestJob();
        if (old != null && "RUNNING".equals(old.getStatus()))
            throw conflict("存在未结束任务；确认旧进程停止后调用recover恢复");
        var sources = registry.all(true);
        if (old != null && Set.of("FAILED", "STOPPED").contains(old.getStatus())) {
            boolean changingSources = request != null && request.sourceCodes() != null && !request.sourceCodes().isEmpty()
                    && !request.sourceCodes().stream().map(code -> code.trim().toUpperCase(Locale.ROOT)).distinct().sorted().toList()
                    .equals(decode(old.getSourceCodesJson()).stream().sorted().toList());
            if ("REBUILD".equals(mode) || changingSources)
                throw conflict("先续传未完成任务，或discard后再选择来源/重建");
            var resume = old;
            tx.executeWithoutResult(state -> {
                initializeCursors(resume, sources);
                resume.setStatus("RUNNING");
                resume.setErrorMessage(null);
                resume.setCompletedTime(null);
                mapper.updateJob(resume);
            });
        } else {
            var available = sources.stream().map(source -> source.sourceCode()).toList();
            boolean all = request == null || request.sourceCodes() == null || request.sourceCodes().isEmpty();
            List<String> selected = all ? available : request.sourceCodes().stream()
                    .map(code -> code.trim().toUpperCase(Locale.ROOT)).distinct().sorted().toList();
            if (selected.isEmpty() || !available.containsAll(selected)) throw conflict("请选择已注册且启用的来源");
            var create = new PreparationJob();
            create.setStatus("RUNNING");
            create.setSourceCodesJson(encode(selected));
            create.setSourceIndex(0);
            create.setAfterId(0L);
            create.setRowsScanned(0L);
            create.setRowsChanged(0L);
            create.setAllSources(all);
            create.setRebuild("REBUILD".equals(mode));
            create.setScanMode(mode);
            create.setBatchSize(request == null || request.batchSize() == null ? 200 : Math.max(1, Math.min(200, request.batchSize())));
            tx.executeWithoutResult(state -> {
                mapper.createJob(create);
                initializeCursors(create, sources);
            });
            old = create;
        }
        running.set(true);
        stop.set(false);
        final var job = old;
        executor.submit(() -> run(job));
        return status();
    }

    /**
     * 冻结每个来源的扫描上界；增量下界回退重叠秒数，恢复任务复用已有窗口而不重算。
     */
    private void initializeCursors(PreparationJob job,
                                   List<com.example.bioplatform.research.collection.common.service.ResearchPublicationSource> sources) {
        var codes = decode(job.getSourceCodesJson());
        LocalDateTime now = mapper.databaseTime();
        // Exclude the currently changing second. A small replay window also protects same-second/commit boundaries.
        LocalDateTime until = (job.getStartedTime() == null ? now : job.getStartedTime()).withNano(0);
        for (int index = 0; index < codes.size(); index++) {
            String code = codes.get(index);
            if (mapper.sourceCursor(job.getId(), code) != null) continue;
            var source = sources.stream().filter(candidate -> code.equals(candidate.sourceCode())).findFirst()
                    .orElseThrow(() -> conflict("来源已停用，请丢弃未完成任务后重新整理: " + code));
            var baseline = mapper.successfulCursor(code);
            boolean delta = "INCREMENTAL".equals(job.getScanMode()) && source.supportsIncremental()
                    && baseline != null && mapper.unmappedActive(code) == 0;
            var cursor = new PreparationSourceCursor();
            cursor.setJobId(job.getId());
            cursor.setSourceCode(code);
            cursor.setScanMode(delta ? "INCREMENTAL" : "FULL");
            cursor.setStatus(index < job.getSourceIndex() ? "COMPLETE" : "PENDING");
            cursor.setUntilTime(until);
            cursor.setFromTime(delta ? baseline.getUntilTime().minusSeconds(Math.max(0, overlapSeconds)) : null);
            cursor.setAfterUpdateTime(delta ? cursor.getFromTime() : null);
            cursor.setAfterId(index == job.getSourceIndex() ? job.getAfterId() : 0L);
            mapper.insertCursor(cursor);
        }
    }

    /**
     * 设置停止请求，由后台在安全边界收尾；不直接删除已持久化的记录和断点。
     */
    public synchronized Map<String, Object> stop() {
        stop.set(true);
        return status();
    }

    /**
     * 把进程已退出但数据库仍为RUNNING的任务转成可续传状态；不会重新读取来源数据。
     */
    public synchronized Map<String, Object> recover() {
        if (running.get()) throw conflict("当前进程仍在运行，不能恢复");
        var job = mapper.latestJob();
        if (job != null && "RUNNING".equals(job.getStatus())) {
            job.setStatus("STOPPED");
            job.setErrorMessage("进程中断；将从已提交游标继续");
            mapper.updateJob(job);
        }
        return status();
    }

    /**
     * 在一个事务中撤销未发布修订的成员和文献版本，重新打开被它关闭的旧版本；不删除原始采集表。
     */
    public synchronized Map<String, Object> discard() {
        if (running.get()) throw conflict("先停止当前任务");
        var job = mapper.latestJob();
        if (job == null || !Set.of("FAILED", "STOPPED").contains(job.getStatus()))
            throw conflict("只能丢弃已停止或失败的未发布整理版本");
        tx.executeWithoutResult(s -> {
            for (String type : PreparedMetadataParser.TYPES)
                mapper.deleteRevisionMembers(PreparedMetadataParser.table(type), job.getId());
            mapper.deleteRevisionDocuments(job.getId());
            mapper.reopenRevision(job.getId());
            mapper.deleteIdentities(job.getId());
            job.setStatus("CANCELLED");
            job.setCompletedTime(LocalDateTime.now());
            mapper.updateJob(job);
        });
        return status();
    }

    /**
     * 组合进程状态、持久化游标及成功来源覆盖情况；只有无未完成任务且来源齐全时canBuild才为true。
     */
    public Map<String, Object> status() {
        Map<String, Object> result = new LinkedHashMap<>();
        var job = mapper.latestJob();
        Long revision = mapper.readyRevision();
        Set<String> covered = new HashSet<>();
        for (String payload : mapper.coveredSources()) covered.addAll(decode(payload));
        var missing = registry.options().stream().map(o -> o.code()).filter(c -> !covered.contains(c)).toList();
        boolean pending = job != null && Set.of("RUNNING", "FAILED", "STOPPED").contains(job.getStatus());
        result.put("running", running.get() || job != null && "RUNNING".equals(job.getStatus()));
        result.put("stopRequested", stop.get());
        result.put("job", job);
        result.put("sourceCursors", job == null ? List.of() : mapper.sourceCursors(job.getId()));
        result.put("overlapSeconds", Math.max(0, overlapSeconds));
        result.put("readyRevision", revision);
        result.put("missingSources", missing);
        result.put("canBuild", revision != null && !pending && missing.isEmpty());
        result.put("sources", registry.options());
        return result;
    }

    /**
     * 返回正式快照可读取的成功修订号；当前实现也会阻止整理运行期间生成新快照。
     */
    public long requireReady() {
        var state = status();
        if (!Boolean.TRUE.equals(state.get("canBuild")))
            throw conflict("请先完成全部已接入来源的整理，失败或停止任务必须续传/丢弃后才能生成新快照");
        return ((Number) state.get("readyRevision")).longValue();
    }

    /**
     * 逐来源、逐批整理；每批提交成员和游标，全部结束后校验完整性并生成DOI桥接，再发布SUCCESS。
     */
    private void run(PreparationJob job) {
        try {
            Map<String, String> routes = new HashMap<>();
            for (var route : routing.routes())
                routes.put(route.get("bank_db").toString().toLowerCase(Locale.ROOT), route.get("source_code").toString());
            List<String> codes = decode(job.getSourceCodesJson());
            while (job.getSourceIndex() < codes.size()) {
                if (stop.get()) {
                    finish(job, "STOPPED", null);
                    return;
                }
                String code = codes.get(job.getSourceIndex());
                var source = registry.all(true).stream().filter(candidate -> code.equals(candidate.sourceCode())).findFirst()
                        .orElseThrow(() -> new IllegalStateException("来源已停用，请discard后重新整理: " + code));
                var cursor = mapper.sourceCursor(job.getId(), code);
                if (cursor == null) throw new IllegalStateException("整理任务缺少来源游标: " + code);
                boolean delta = "INCREMENTAL".equals(cursor.getScanMode());
                var rows = delta ? source.loadChangedRecordsPage(true, cursor.getAfterUpdateTime(), cursor.getAfterId(), cursor.getUntilTime(), job.getBatchSize())
                        : source.loadRecordsPage(true, cursor.getAfterId(), job.getBatchSize());
                if (rows.isEmpty()) {
                    tx.executeWithoutResult(state -> {
                        // Delta scans never expire unseen records: most source documents were deliberately not read.
                        if (!delta) mapper.expireMissing(code, job.getId());
                        cursor.setStatus("COMPLETE");
                        mapper.updateCursor(cursor);
                        job.setSourceIndex(job.getSourceIndex() + 1);
                        job.setAfterId(0L);
                        job.setAfterUpdateTime(null);
                        mapper.updateJob(job);
                    });
                    continue;
                }
                validateCursor(cursor, rows);
                Map<String, String> dictionary = loadDictionary(rows);
                // 成员、文献版本及游标同批提交；任何写入失败都回滚，不能先推进断点。
                tx.executeWithoutResult(state -> processBatch(job, code, rows, dictionary, routes, cursor));
            }
            tx.executeWithoutResult(state -> {
                if (Boolean.TRUE.equals(job.getAllSources())) mapper.expireDisabled(codes, job.getId());
                for (String type : PreparedMetadataParser.TYPES) {
                    long invalid = "INCREMENTAL".equals(job.getScanMode())
                            ? mapper.invalidRevisionMembers(PreparedMetadataParser.table(type), job.getId())
                            : mapper.invalidMembers(PreparedMetadataParser.table(type));
                    if (invalid > 0) throw new IllegalStateException("整理成员完整性校验失败: " + type);
                }
                mapper.deleteIdentities(job.getId());
                // 此处基于本修订全部有效文献建立DOI桥接，不仅限于本次增量读到的记录。
                mapper.prepareIdentities(job.getId());
                finish(job, stop.get() ? "STOPPED" : "SUCCESS", null);
            });
        } catch (Exception ex) {
            var persisted = mapper.latestJob();
            if (persisted != null && Objects.equals(persisted.getId(), job.getId()))
                finish(persisted, "FAILED", truncate(ex.getMessage(), 1900));
        } finally {
            running.set(false);
            stop.set(false);
        }
    }

    /**
     * 校验来源分页严格递增；增量按更新时间再按ID排序，防止重复页或游标倒退导致死循环。
     */
    private void validateCursor(PreparationSourceCursor cursor, List<SourcePublicationRecord> rows) {
        long after = cursor.getAfterId();
        LocalDateTime time = cursor.getAfterUpdateTime();
        boolean delta = "INCREMENTAL".equals(cursor.getScanMode());
        for (var row : rows) {
            Long id = row.getSourceRowId();
            if (id == null || id <= 0) throw new IllegalStateException("来源记录缺少稳定主键");
            if (delta) {
                var updated = row.getSourceUpdatedTime();
                if (updated == null || !updated.isBefore(cursor.getUntilTime()) || updated.isBefore(time)
                        || updated.equals(time) && id <= after)
                    throw new IllegalStateException("来源更新时间/ID游标未严格递增: " + cursor.getSourceCode());
                time = updated;
            } else if (id <= after) throw new IllegalStateException("来源ID游标未严格递增: " + cursor.getSourceCode());
            after = id;
        }
    }

    /**
     * 在调用方事务内解析一批记录；内容相同只更新扫描标记，变更则替换版本并写五类成员，最后推进游标。
     */
    private void processBatch(PreparationJob job, String code, List<SourcePublicationRecord> rows,
                              Map<String, String> dictionary, Map<String, String> routes, PreparationSourceCursor cursor) {
        var hashes = rows.stream().map(row -> PublicationValues.sha(sourceKey(row))).toList();
        Map<String, PreparedDocument> byHash = new HashMap<>();
        Map<Long, PreparedDocument> byRow = new HashMap<>();
        for (var prior : mapper.activeDocuments(code, hashes, rows.stream().map(SourcePublicationRecord::getSourceRowId).toList())) {
            byHash.put(prior.getSourceHash(), prior);
            if (prior.getSourceRowId() != null) byRow.put(prior.getSourceRowId(), prior);
        }
        for (var row : rows) {
            String hash = PublicationValues.sha(sourceKey(row));
            var previous = byHash.get(hash);
            if (previous == null) previous = byRow.get(row.getSourceRowId());
            if (Boolean.FALSE.equals(row.getRecordActive())) {
                if (previous != null) {
                    retire(previous, job.getId());
                    byHash.remove(previous.getSourceHash());
                    byRow.remove(row.getSourceRowId());
                    job.setRowsChanged(job.getRowsChanged() + 1);
                }
                continue;
            }
            var parsed = parser.parse(row, dictionary);
            var document = document(code, row, job.getId(), routes);
            document.setSourceRowId(row.getSourceRowId());
            document.setValidFrom(null);
            document.setSeenRun(null);
            document.setContentHash(PublicationValues.sha(encode(document) + parsed.contentHash()));
            document.setValidFrom(job.getId());
            document.setSeenRun(job.getId());
            if (previous != null && !Boolean.TRUE.equals(job.getRebuild()) && document.getContentHash().equals(previous.getContentHash())) {
                mapper.touch(previous.getId(), job.getId(), row.getSourceRowId());
                previous.setSourceRowId(row.getSourceRowId());
                byRow.put(row.getSourceRowId(), previous);
                continue;
            }
            if (previous != null) {
                retire(previous, job.getId());
                byHash.remove(previous.getSourceHash());
            }
            mapper.insertDocument(document);
            byHash.put(document.getSourceHash(), document);
            byRow.put(row.getSourceRowId(), document);
            for (String type : PreparedMetadataParser.TYPES) {
                var members = parsed.members().get(type);
                for (var member : members) {
                    member.setDocumentId(document.getId());
                    member.setSourceCode(code);
                    member.setDocumentKey(document.getDocumentKey());
                    member.setPmid(document.getPmid());
                    member.setDoi(document.getDoi());
                    member.setSourceUrl(document.getSourceUrl());
                }
                for (int offset = 0; offset < members.size(); offset += 50)
                    mapper.insertMembers(PreparedMetadataParser.table(type), members.subList(offset, Math.min(offset + 50, members.size())));
            }
            job.setRowsChanged(job.getRowsChanged() + 1);
        }
        var last = rows.get(rows.size() - 1);
        cursor.setAfterId(last.getSourceRowId());
        if ("INCREMENTAL".equals(cursor.getScanMode())) cursor.setAfterUpdateTime(last.getSourceUpdatedTime());
        cursor.setStatus("RUNNING");
        mapper.updateCursor(cursor);
        job.setRowsScanned(job.getRowsScanned() + rows.size());
        job.setAfterId(cursor.getAfterId());
        job.setAfterUpdateTime(cursor.getAfterUpdateTime());
        mapper.updateJob(job);
    }

    /**
     * 同轮未发布版本可删除后替换；已发布旧版本只设置失效修订，保证历史快照仍能读取。
     */
    private void retire(PreparedDocument previous, long revision) {
        if (Objects.equals(previous.getValidFrom(), revision)) {
            // A source may be observed again after a late commit. Replace its unpublished version atomically.
            for (String type : PreparedMetadataParser.TYPES)
                mapper.deleteDocumentMembers(PreparedMetadataParser.table(type), previous.getId());
            mapper.deleteDocument(previous.getId());
        } else mapper.close(previous.getId(), revision);
    }

    /**
     * 构造来源文献事实；PMID优先，随后DOI或来源业务键；资源始终按来源业务键标识。
     */
    private PreparedDocument document(String code, SourcePublicationRecord r, long revision, Map<String, String> routes) {
        var d = new PreparedDocument();
        d.setSourceCode(code);
        d.setSourceKey(sourceKey(r));
        d.setSourceHash(PublicationValues.sha(d.getSourceKey()));
        d.setPmid(clean(r.getPmid()));
        d.setDoi(doi(r.getDoi()));
        d.setRecordKind("RESOURCE".equals(r.getRecordKind()) ? "RESOURCE" : "PUBLICATION");
        d.setDocumentKey("RESOURCE".equals(d.getRecordKind()) ? "S:" + PublicationValues.sha(code + ":" + d.getSourceKey()) : d.getPmid() != null ? "P:" + d.getPmid() : d.getDoi() != null ? "D:" + PublicationValues.sha(d.getDoi()) : "S:" + PublicationValues.sha(code + ":" + d.getSourceKey()));
        d.setTitle(r.getTitle());
        d.setJournalTitle(r.getJournalTitle());
        d.setPubYear(r.getPubYear());
        d.setResourceAccession(r.getResourceAccession());
        d.setGsaAccessions(r.getGsaAccessions());
        d.setBioprojectAccession(r.getBioprojectAccession());
        d.setPrimaryFlag(Boolean.TRUE.equals(r.getPrimaryFlag()) || membership.belongs(code, r.getCncbBanksJson(), routes));
        d.setSourceUrl("RESOURCE".equals(d.getRecordKind()) ? r.getResourceUrl() : r.getSourceUrl() != null && !r.getSourceUrl().isBlank() ? r.getSourceUrl() : PublicationLinks.external(r.getPmid(), r.getDoi(), r.getGsaAccessions(), r.getBioprojectAccession(), r.getCncbBanksJson(), json));
        d.setValidFrom(revision);
        d.setSeenRun(revision);
        return d;
    }

    /**
     * 仅收集缺少agency的资助编号，分批查询本地基金字典；不在整理时逐篇联网检索基金。
     */
    private Map<String, String> loadDictionary(List<SourcePublicationRecord> rows) {
        Set<String> ids = new LinkedHashSet<>();
        for (var r : rows) ids.addAll(FundAgencyParser.missingAgencyGrantIds(r.getGrantsJson(), json));
        Map<String, String> result = new HashMap<>();
        var list = new ArrayList<>(ids);
        for (int i = 0;
             i < list.size();
             i += 200)
            for (var entry : dictionaries.selectFundDictionaryByGrantIds(list.subList(i, Math.min(i + 200, list.size()))))
                result.put(entry.getGrantId().toLowerCase(Locale.ROOT), entry.getFullName());
        return result;
    }

    /**
     * 持久化整理任务最终状态、错误和结束时间；不是更新快照结果表。
     */
    private void finish(PreparationJob job, String status, String error) {
        job.setStatus(status);
        job.setErrorMessage(error);
        job.setCompletedTime(LocalDateTime.now());
        mapper.updateJob(job);
    }

    /**
     * 把结构化对象序列化成JSON，供配置冻结或稳定摘要使用。
     * @param value 待校验、转换或合并的值
     * @return 按上述规则计算或读取的String结果
     */
    private String encode(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * 把已保存的JSON列表还原为业务集合；结构异常时明确报错。
     * @param value 待校验、转换或合并的值
     * @return 按上述规则计算或读取的List<String>结果
     */
    public List<String> decode(String value) {
        try {
            return json.readValue(value, json.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * 取得来源稳定业务键；缺少业务键时不能用不稳定分页位置代替。
     * @param r 当前响应、记录或文献对象
     * @return 按上述规则计算或读取的String结果
     */
    private static String sourceKey(SourcePublicationRecord r) {
        if (clean(r.getSourceRecordKey()) == null) throw new IllegalStateException("Missing source business key");
        return r.getSourceRecordKey();
    }

    /**
     * 去除DOI访问地址前缀并规范大小写，用于已有标识的匹配。
     * @param value 待校验、转换或合并的值
     * @return 按上述规则计算或读取的String结果
     */
    public static String doi(String value) {
        String s = clean(value);
        return s == null ? null : s.toLowerCase(Locale.ROOT).replaceFirst("^https?://(dx\\.)?doi\\.org/", "").replaceFirst("^doi:\\s*", "");
    }

    /**
     * 清理字段两端空白和无意义缺失值，返回可参与比较的规范文本。
     * @param v 待处理的原始值
     * @return 按上述规则计算或读取的String结果
     */
    private static String clean(String v) {
        return v == null || v.isBlank() || "null".equalsIgnoreCase(v.trim()) ? null : v.trim();
    }

    /**
     * 按指定最大长度截断字段或错误文本。
     * @param v 待处理的原始值
     * @param n 长度或数量上限
     * @return 按上述规则计算或读取的String结果
     */
    private static String truncate(String v, int n) {
        return v == null ? "Preparation failed" : v.substring(0, Math.min(v.length(), n));
    }

    /**
     * 构造带稳定错误码的业务冲突异常，交由全局异常处理器统一返回。
     * @param m 当前成员或合并目标
     * @return 按上述规则计算或读取的BusinessException结果
     */
    private static BusinessException conflict(String m) {
        return new BusinessException(HttpStatus.CONFLICT, "PREPARATION_NOT_READY", m);
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

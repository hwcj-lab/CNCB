package com.example.bioplatform.research.collection.genbase.service.impl;
import com.example.bioplatform.research.common.GrantPayloads;
import com.example.bioplatform.research.common.ResearchPublicationJsonMerger;
import com.example.bioplatform.research.collection.genbase.dto.GenBaseCollectionStartRequest;
import com.example.bioplatform.research.collection.genbase.dto.GenBaseOpenLbWriteRow;
import com.example.bioplatform.research.collection.genbase.dto.GenBasePmidTask;
import com.example.bioplatform.research.collection.genbase.dto.GenBasePublicationWriteRow;
import com.example.bioplatform.research.collection.genbase.mapper.GenBaseCollectionMapper;
import com.example.bioplatform.research.collection.genbase.service.GenBaseCollectionService;
import com.example.bioplatform.research.integration.service.ResearchPublicationIntegrationService;
import com.example.bioplatform.research.collection.genbase.vo.GenBaseCollectionStatus;
import com.example.bioplatform.research.collection.genbase.vo.GenBaseJobState;

import com.example.bioplatform.common.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * GenBase支持文章的分页采集实现。
 * <p>
 * 每页只进行一次HTTP请求和一次批量INSERT，随后在同一事务推进持久化断点。
 * 当前按官方列表做完整扫描，唯一索引负责跳过已入库文章。
 *
 * TODO 增量采集：若官方接口后续提供稳定的发布日期筛选、更新时间或游标，
 * 改为按上次水位增量获取；在此之前不能仅凭列表排序假设增量边界，否则可能漏数。
 */
@Service
@RequiredArgsConstructor
public class GenBaseCollectionServiceImpl implements GenBaseCollectionService {
    private static final Pattern YEAR = Pattern.compile("^(\\d{4})");
    private final GenBaseCollectionMapper mapper;
    private final ResearchPublicationIntegrationService publicationIntegration;
    private final GenBaseMetadataClient metadata;
    private final ObjectMapper json;
    private final TransactionTemplate transactions;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> daemon(r, "genbase-job"));
    private final ExecutorService enrichmentExecutor = Executors.newFixedThreadPool(4, r -> daemon(r, "genbase-openlb-worker"));
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean stopRequested = new AtomicBoolean(false);
    private final AtomicLong currentDelayMs = new AtomicLong(500);
    private final AtomicInteger consecutiveThrottles = new AtomicInteger(0);
    private volatile String message = "未启动";
    private volatile long activeJobId;

    @Value("${app.genbase.endpoint:https://ngdc.cncb.ac.cn/genbase/statistics/getSupportPaperList}")
    private String endpoint;

    /** {@inheritDoc} */
    @Override
    public synchronized GenBaseCollectionStatus start(GenBaseCollectionStartRequest request) {
        if (!running.compareAndSet(false, true))
            throw new BusinessException(HttpStatus.CONFLICT, "GENBASE_JOB_RUNNING", "GenBase采集已在运行");
        stopRequested.set(false);
        currentDelayMs.set(request.minDelayMsOrDefault());
        consecutiveThrottles.set(0);
        try {
            activeJobId = transactions.execute(status -> prepareJob(request.pageSizeOrDefault()));
            message = "GenBase采集启动中";
            executor.submit(() -> runJob(request));
            return status();
        } catch (RuntimeException e) {
            running.set(false);
            throw e;
        }
    }

    /** 恢复异常作业，优先续传；没有断点时创建一次新的全量扫描。 */
    private long prepareJob(int pageSize) {
        mapper.recoverInterruptedJobs("应用重启后恢复为可续传状态");
        GenBaseJobState resumable = mapper.findResumableJob();
        if (resumable != null) {
            mapper.resumeJob(resumable.jobId(), pageSize);
            return resumable.jobId();
        }
        mapper.createJob(pageSize);
        Long id = mapper.lastInsertId();
        if (id == null) throw new IllegalStateException("无法取得GenBase采集作业主键");
        return id;
    }

    /** {@inheritDoc} */
    @Override
    public GenBaseCollectionStatus stop() {
        stopRequested.set(true);
        message = running.get() ? "已请求停止，当前页完成后保存断点" : "当前没有运行中的GenBase采集";
        return status();
    }

    /** 保留现有文献，只将GenBase元数据补全状态批量重置；随后由/jobs/start重新执行。 */
    @Override
    public synchronized GenBaseCollectionStatus resetEnrichment() {
        if (running.get()) {
            throw new BusinessException(HttpStatus.CONFLICT, "GENBASE_JOB_RUNNING", "GenBase采集运行中，不能重置补全状态");
        }
        int resetCount = mapper.resetPmidEnrichment();
        message = "已重置 " + resetCount + " 条GenBase元数据补全任务（包括无PMID记录）；调用/jobs/start开始重新补全";
        return status();
    }

    /** {@inheritDoc} */
    @Override
    public GenBaseCollectionStatus status() {
        GenBaseJobState state = mapper.findLatestJob();
        if (state == null)
            return new GenBaseCollectionStatus(running.get(), stopRequested.get(), message, null, null,
                    0, null, null, 0, 0, 0, null, currentDelayMs.get(), null, null);
        return new GenBaseCollectionStatus(running.get(), stopRequested.get(), message, state.jobId(), state.jobStatus(),
                state.nextStart(), state.pageSize(), state.recordsTotal(), state.rowsSeen(), state.rowsInserted(),
                state.attemptCount(), state.lastError(), currentDelayMs.get(), state.startedTime(), state.completedTime());
    }

    /** 逐页运行，停止、成功和最终失败都会写入作业表。 */
    private void runJob(GenBaseCollectionStartRequest config) {
        try {
            message = "GenBase采集运行中";
            boolean catalogComplete = false;
            while (!stopRequested.get()) {
                GenBaseJobState state = mapper.findLatestJob();
                Page page = fetchWithRetry(state.nextStart(), config);
                if (page == null) break;
                var rows = toRows(page.data());
                int nextStart = state.nextStart() + page.data().size();
                transactions.executeWithoutResult(tx -> {
                    int inserted = rows.isEmpty() ? 0 : mapper.insertPublicationsIgnore(rows);

                    publicationIntegration.syncGenBasePaperIds(rows.stream().map(GenBasePublicationWriteRow::paperId).toList());
                    mapper.advanceJob(activeJobId, nextStart, page.recordsTotal(), page.data().size(), inserted);
                });
                message = "已读取 " + nextStart + " / " + page.recordsTotal() + "，当前限速 " + currentDelayMs.get() + " ms";
                onSuccess(config.minDelayMsOrDefault());
                if (page.data().isEmpty() || nextStart >= page.recordsTotal()) {
                    catalogComplete = true;
                    break;
                }
                sleep(currentDelayMs.get());
            }
            if (!stopRequested.get() && catalogComplete) enrichPendingFromOpenLb(config);
            if (stopRequested.get()) {
                mapper.finishJob(activeJobId, "STOPPED", null);
                if (!message.startsWith("连续限流")) message = "GenBase采集已停止，可从断点继续";
            } else {
                mapper.finishJob(activeJobId, "SUCCESS", null);
                message = "GenBase列表采集及元数据补全完成";
            }
        } catch (Exception e) {
            String error = abbreviate(rootMessage(e), 1000);
            mapper.finishJob(activeJobId, "FAILED", error);
            message = "GenBase采集异常停止：" + error;
        } finally {
            running.set(false);
        }
    }

    /**
     * 从GenBase表批量领取PMID，并使用OpenLB补充作者、机构、主题和基金信息。
     * GenBase已经提供了文献关联证据，因此cncbBanks为null或空数组时仍保存文献详情。
     */
    private void enrichPendingFromOpenLb(GenBaseCollectionStartRequest config) throws Exception {
        mapper.recoverRunningEnrichment("应用中断后恢复OpenLB补全任务");
        while (!stopRequested.get()) {
            var tasks = mapper.findPendingEnrichment(Math.min(config.pageSizeOrDefault(), 100));
            if (tasks.isEmpty()) return;
            mapper.markEnrichmentRunning(tasks.stream().map(GenBasePmidTask::getPaperId).toList());
            var results = new ArrayList<EnrichmentResult>();
            List<CompletableFuture<EnrichmentResult>> futures = tasks.stream()
                    .map(task -> CompletableFuture.supplyAsync(() -> collectOpenLb(task, config), enrichmentExecutor))
                    .toList();
            for (int i = 0; i < futures.size(); i++) {
                if (stopRequested.get()) break;
                try {
                    EnrichmentResult result = futures.get(i).join();
                    if (result != null) results.add(result);
                } catch (CompletionException e) {
                    results.add(EnrichmentResult.failed(tasks.get(i), rootMessage(e)));
                }
            }
            persistEnrichment(results, config.maxAttemptsOrDefault());
            message = "正在读取GenBase元数据（原有PMID／项目详情），当前批次 " + results.size() + " 条";
        }
        mapper.recoverRunningEnrichment("采集停止，等待下次继续补全");
    }

    /** 已有PMID、DOI或项目号均可补全；失败使用原有任务重试预算，绝不删除原文献。 */
    private EnrichmentResult collectOpenLb(GenBasePmidTask task, GenBaseCollectionStartRequest config) {
        if(stopRequested.get())return null;
        try {
            var row=metadata.enrich(task,currentDelayMs.get(),stopRequested::get,value->message=value);
            onSuccess(config.minDelayMsOrDefault());
            return EnrichmentResult.success(task,row);
        } catch(Exception e) {
            if(e instanceof InterruptedException)Thread.currentThread().interrupt();
            if(stopRequested.get())return null;
            String error=rootMessage(e);
            if(e instanceof java.net.http.HttpTimeoutException || error.contains("429") || error.contains("HTTP 503"))onThrottle(config,error);
            return EnrichmentResult.failed(task,error);
        }
    }

    /** 在单一事务内批量回填成功、空结果和失败状态。 */
    private void persistEnrichment(java.util.List<EnrichmentResult> results,int maxAttempts) {
        var success=new ArrayList<>(results.stream().filter(r->r.outcome==EnrichmentOutcome.SUCCESS).toList());
        var failed=new ArrayList<>(results.stream().filter(r->r.outcome==EnrichmentOutcome.FAILED).toList());
        transactions.executeWithoutResult(tx->{
            if(!success.isEmpty()) {
                var live=mapper.lockEnrichment(success.stream().map(r->r.task.getPaperId()).toList()).stream().collect(java.util.stream.Collectors.toMap(GenBasePmidTask::getPaperId,java.util.function.Function.identity()));
                for(var result:success){
                    var current=live.get(result.task.getPaperId());if(current==null)throw new IllegalStateException("GenBase补全期间原文献已删除");
                    result.row.setAuthorsJson(com.example.bioplatform.research.common.ResearchPublicationJsonMerger.mergeAuthors(current.getAuthorsJson(),result.row.getAuthorsJson(),json));
                    result.row.setGrantsJson(GrantPayloads.merge(current.getGrantsJson(),result.row.getGrantsJson(),json));
                    result.row.setCncbBanksJson(com.example.bioplatform.research.common.ResearchPublicationJsonMerger.mergeBanks(current.getCncbBanksJson(),result.row.getCncbBanksJson(),json));
                }
                if(!success.isEmpty())mapper.updateEnrichmentSuccess(success.stream().map(EnrichmentResult::row).toList());
            }
            if(!success.isEmpty()) publicationIntegration.syncGenBasePaperIds(success.stream().map(r -> r.task.getPaperId()).toList());
            if(!failed.isEmpty()) mapper.markEnrichmentFailedOrPending(failed.stream()
                    .map(r->new GenBaseCollectionMapper.EnrichmentFailure(r.task.getPaperId(),abbreviate(r.error,1000))).toList(),maxAttempts);
        });
    }

    /** 当前页带有限次数重试；失败时断点不前移。 */
    private Page fetchWithRetry(int start, GenBaseCollectionStartRequest config) throws Exception {
        Exception last = null;
        for (int attempt = 1; attempt <= config.maxAttemptsOrDefault() && !stopRequested.get(); attempt++) {
            try {
                return fetchPage(start, config.pageSizeOrDefault());
            } catch (Exception e) {
                last = e;
                mapper.recordFailure(activeJobId, abbreviate(rootMessage(e), 1000));
                boolean throttle = e instanceof ThrottledException || e instanceof java.net.http.HttpTimeoutException;
                if (throttle) onThrottle(config, rootMessage(e));
                if (stopRequested.get()) return null;
                sleep(Math.min(30_000L, 1000L * attempt * attempt));
            }
        }
        throw last == null ? new IOException("GenBase当前页请求失败") : last;
    }

    /** 请求DataTables分页接口；只需要draw、start和length三个参数。 */
    private Page fetchPage(int start, int length) throws Exception {
        String url = endpoint + "?draw=1&start=" + start + "&length=" + length;
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(45))
                .header("Accept", "application/json")
                .header("User-Agent", "BioPortalResearchCollector/1.0")
                .header("Referer", "https://ngdc.cncb.ac.cn/genbase/statistics/supportpaper")
                .GET().build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() == 429 || response.statusCode() == 502 || response.statusCode() == 503 || response.statusCode() == 504)
            throw new ThrottledException("HTTP " + response.statusCode());
        if (response.statusCode() != 200) throw new IOException("HTTP " + response.statusCode());
        JsonNode root = json.readTree(response.body());
        if (!root.path("data").isArray()) throw new IOException("GenBase响应缺少data数组");
        return new Page(root.path("recordsTotal").asInt(root.path("recordsFiltered").asInt()), root.path("data"));
    }

    /** 将一页JSON转换为批量写入行；缺失paperId或标题的异常记录不入库。 */
    private ArrayList<GenBasePublicationWriteRow> toRows(JsonNode data) {
        var rows = new ArrayList<GenBasePublicationWriteRow>();
        for (JsonNode node : data) {
            if (!node.path("paperId").canConvertToLong() || blankToNull(node.path("paperTitle").asText(null)) == null) continue;
            String raw;
            try { raw = json.writeValueAsString(node); } catch (Exception e) { continue; }
            String publishTime = blankToNull(node.path("publishTime").asText(null));
            // 上游接口字段名仍为 accessionRange；入库后统一使用 genbase_accessions。
            String genbaseAccessions = blankToNull(node.path("accessionRange").asText(null));
            rows.add(new GenBasePublicationWriteRow(
                    node.path("paperId").asLong(), numericOrNull(node.path("pubmedId").asText(null)),
                    normalizeDoi(node.path("doi").asText(null)), blankToNull(node.path("bioproject").asText(null)),
                    blankToNull(node.path("journalName").asText(null)), node.path("paperTitle").asText(),
                    year(publishTime), genbaseAccessions, endpoint, raw, sha256(raw)));
        }
        return rows;
    }

    /** 限流后指数降低速度；达到阈值时安全停止。 */
    private void onThrottle(GenBaseCollectionStartRequest config, String reason) {
        long delay = currentDelayMs.updateAndGet(value -> Math.min(10_000L, Math.max(config.minDelayMsOrDefault(), value * 2)));
        int count = consecutiveThrottles.incrementAndGet();
        message = "检测到GenBase限流或超时，已降速到 " + delay + " ms；连续次数 " + count;
        if (count >= config.stopThresholdOrDefault()) {
            message = "连续限流达到阈值，已自动停止：" + reason;
            stopRequested.set(true);
        }
    }

    /** 成功后清零连续失败，并缓慢恢复请求速度。 */
    private void onSuccess(long baseDelay) {
        consecutiveThrottles.set(0);
        currentDelayMs.updateAndGet(value -> Math.max(baseDelay, value - 50));
    }

    private static String numericOrNull(String value) { value = blankToNull(value); return value != null && value.matches("\\d+") ? value : null; }

    /** 生成来源记录SHA-256，用于批量扫描时区分无变化记录和需要重新关联的变化记录。 */
    private static String sha256(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalStateException("无法计算GenBase来源摘要", e); }
    }
    private static String normalizeDoi(String value) {
        value = blankToNull(value);
        if (value == null) return null;
        return value.replaceFirst("(?i)^https?://(?:dx\\.)?doi\\.org/", "").replaceFirst("(?i)^doi:\\s*", "").trim().toLowerCase(Locale.ROOT);
    }
    private static String blankToNull(String value) { return value == null || value.isBlank() || "null".equalsIgnoreCase(value.trim()) ? null : value.trim(); }
    private static Integer year(String value) { if (value == null) return null; var m=YEAR.matcher(value); if(!m.find())return null; int y=Integer.parseInt(m.group(1)); return y>=1500&&y<=2200?y:null; }
    private static void sleep(long millis) throws InterruptedException { Thread.sleep(millis); }
    private static String abbreviate(String value, int max) { return value == null ? null : value.substring(0, Math.min(value.length(), max)); }
    private static String rootMessage(Throwable e) { while (e.getCause()!=null) e=e.getCause(); return e.getMessage()==null?e.getClass().getSimpleName():e.getMessage(); }
    private static Thread daemon(Runnable r, String name) { var t=new Thread(r,name); t.setDaemon(true); return t; }

    private record Page(int recordsTotal, JsonNode data) {}
    private enum EnrichmentOutcome { SUCCESS, FAILED }
    private record EnrichmentResult(GenBasePmidTask task,EnrichmentOutcome outcome,GenBaseOpenLbWriteRow row,String error) {
        static EnrichmentResult success(GenBasePmidTask task,GenBaseOpenLbWriteRow row){return new EnrichmentResult(task,EnrichmentOutcome.SUCCESS,row,null);}
        static EnrichmentResult failed(GenBasePmidTask task,String error){return new EnrichmentResult(task,EnrichmentOutcome.FAILED,null,error);}
    }
    private static class ThrottledException extends IOException { private ThrottledException(String message) { super(message); } }

    /** Spring关闭时请求停止并关闭后台执行器。 */
    @PreDestroy void shutdown() { stopRequested.set(true); executor.shutdownNow(); enrichmentExecutor.shutdownNow(); }
}


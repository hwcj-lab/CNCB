package com.example.bioplatform.research.collection.openlb.service.impl;

import com.example.bioplatform.research.common.GrantPayloads;
import com.example.bioplatform.research.collection.openlb.dto.OpenLbCollectionStartRequest;
import com.example.bioplatform.research.collection.openlb.dto.OpenLbPublicationWriteRow;
import com.example.bioplatform.research.collection.openlb.mapper.OpenLbCollectionMapper;
import com.example.bioplatform.research.collection.openlb.service.OpenLbCollectionService;
import com.example.bioplatform.research.integration.service.ResearchPublicationIntegrationService;
import com.example.bioplatform.research.collection.openlb.vo.OpenLbCollectionStatus;

import com.example.bioplatform.common.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * OpenLB 采集流程实现。
 * <p>
 * 网络工作线程只负责获取和解析响应，不直接操作数据库；
 * 一批请求全部结束后，再通过 Mapper 在单一事务中批量落库。
 * 这种设计同时控制上游请求速率和本地数据库压力。
 */
@Service
@RequiredArgsConstructor
public class OpenLbCollectionServiceImpl implements OpenLbCollectionService {
    private final OpenLbCollectionMapper mapper;
    private final OpenLbRoutingService routing;
    private final ResearchPublicationIntegrationService publicationIntegration;
    private final ObjectMapper json;
    private final TransactionTemplate transactions;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    private final ExecutorService jobExecutor = Executors.newSingleThreadExecutor(r -> daemon(r, "openlb-job"));
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean stopRequested = new AtomicBoolean(false);
    private final AtomicLong nextRequestAt = new AtomicLong(0);
    private final AtomicLong currentDelayMs = new AtomicLong(350);
    private final AtomicInteger consecutiveThrottles = new AtomicInteger(0);
    private volatile String message = "未启动";
    private volatile LocalDateTime startedAt;
    private volatile LocalDateTime finishedAt;

    @Value("${app.openlb.csv-path:docs/openlb_pmid.csv}")
    private String csvPath;
    @Value("${app.openlb.endpoint:https://ngdc.cncb.ac.cn/openlb/api/pmid?pmid=}")
    private String endpoint;

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized OpenLbCollectionStatus importCsv() {
        if (running.get())
            throw new BusinessException(HttpStatus.CONFLICT, "OPENLB_JOB_RUNNING", "采集运行中，不能导入新任务");
        Path path = Path.of(csvPath).toAbsolutePath().normalize();
        if (!Files.isRegularFile(path) && !Path.of(csvPath).isAbsolute()) {
            Path moduleRelative = Path.of("..", csvPath).toAbsolutePath().normalize();
            if (Files.isRegularFile(moduleRelative)) path = moduleRelative;
        }
        if (!Files.isRegularFile(path))
            throw new BusinessException(HttpStatus.BAD_REQUEST, "OPENLB_CSV_NOT_FOUND", "未找到PMID文件：" + path);
        try {
            var pmids = Files.readAllLines(path, StandardCharsets.UTF_8).stream().skip(1).map(String::trim)
                    .filter(value -> value.matches("\\d+")).distinct().toList();
            for (int start = 0; start < pmids.size(); start += 500)
                mapper.insertTasksIgnore(pmids.subList(start, Math.min(start + 500, pmids.size())));
            message = "CSV已导入：" + pmids.size() + " 个唯一PMID，已存在任务未覆盖";
            return status();
        } catch (IOException e) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "OPENLB_CSV_READ_FAILED", "PMID文件读取失败：" + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized OpenLbCollectionStatus start(OpenLbCollectionStartRequest request) {
        if (!running.compareAndSet(false, true))
            throw new BusinessException(HttpStatus.CONFLICT, "OPENLB_JOB_RUNNING", "OpenLB采集已在运行");
        stopRequested.set(false);
        startedAt = LocalDateTime.now();
        finishedAt = null;
        currentDelayMs.set(request.minDelayMsOrDefault());
        consecutiveThrottles.set(0);
        message = "采集启动中";
        jobExecutor.submit(() -> runJob(request));
        return status();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OpenLbCollectionStatus stop() {
        stopRequested.set(true);
        message = running.get() ? "已请求停止，正在等待当前请求结束" : "当前没有运行中的采集";
        return status();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized OpenLbCollectionStatus retryFailed() {
        if (running.get())
            throw new BusinessException(HttpStatus.CONFLICT, "OPENLB_JOB_RUNNING", "采集运行中，不能重置任务");
        int updated = mapper.retryFailed();
        message = "已重置失败任务：" + updated;
        return status();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OpenLbCollectionStatus status() {
        var counts = new LinkedHashMap<String, Long>();
        mapper.countByStatus().forEach(row -> counts.put(row.status(), row.total()));
        return new OpenLbCollectionStatus(running.get(), stopRequested.get(), message, currentDelayMs.get(), startedAt, finishedAt, counts);
    }

    /**
     * 执行一次完整的后台采集会话。
     * <p>
     * 启动时先校正已入库文献的任务状态，再恢复上次中断遗留的 RUNNING。
     * 之后按 batchSize 取任务、并发请求、批量落库，直到无待处理任务或收到停止请求。
     */
    private void runJob(OpenLbCollectionStartRequest config) {
        ExecutorService workers = Executors.newFixedThreadPool(config.concurrencyOrDefault(), r -> daemon(r, "openlb-worker"));
        try {
            mapper.reconcileCompletedTasks();
            mapper.recoverRunningTasks("Recovered after interrupted process");
            message = "采集运行中";
            while (!stopRequested.get()) {
                var pmids = mapper.findPendingPmids(config.batchSizeOrDefault());
                if (pmids.isEmpty()) {
                    message = "没有可执行的待采集任务";
                    break;
                }
                mapper.markRunning(pmids);
                var futures = pmids.stream().map(pmid -> CompletableFuture.supplyAsync(() -> collectOne(pmid, config), workers)).toList();
                var results = futures.stream().map(CompletableFuture::join).filter(Objects::nonNull).toList();
                persistBatch(results, config);
                message = "采集运行中，当前限速间隔 " + currentDelayMs.get() + " ms";
            }
            if (stopRequested.get() && !message.startsWith("连续限流")) message = "已按请求停止，未完成任务可继续";
        } catch (Exception e) {
            message = "采集异常停止：" + rootMessage(e);
        } finally {
            workers.shutdownNow();
            try {
                mapper.recoverRunningTasks("Recovered after stopped job");
            } catch (Exception recoveryError) {
                message += "；恢复待采集状态失败：" + rootMessage(recoveryError);
            } finally {
                running.set(false);
                finishedAt = LocalDateTime.now();
            }
        }
    }

    /**
     * 请求并解析一个 PMID。
     * <p>
     * 方法不写数据库，只返回 SUCCESS、EMPTY 或 FAILED 内存结果，
     * 以便当前整批请求完成后统一提交。
     */
    private CollectionResult collectOne(String pmid, OpenLbCollectionStartRequest config) {
        if (stopRequested.get()) return null;
        try {
            throttle();
            var request = HttpRequest.newBuilder(URI.create(endpoint + pmid)).timeout(Duration.ofSeconds(30)).header("Accept", "application/json").GET().build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 429 || response.statusCode() == 502 || response.statusCode() == 503 || response.statusCode() == 504) {
                onThrottle(config, "HTTP " + response.statusCode());
                return CollectionResult.failed(pmid, "上游限流：HTTP " + response.statusCode());
            }
            if (response.statusCode() != 200) throw new IOException("HTTP " + response.statusCode());
            JsonNode payload = json.readTree(response.body());
            if (!"200".equals(payload.path("code").asText()))
                throw new IOException("OpenLB code " + payload.path("code").asText());
            JsonNode record = firstRecord(payload, pmid);
            if (record == null) {
                onSuccess(config.minDelayMsOrDefault());
                return CollectionResult.empty(pmid);
            }
            onSuccess(config.minDelayMsOrDefault());
            return CollectionResult.success(pmid, record, response.body());
        } catch (Exception e) {
            if (e instanceof java.net.http.HttpTimeoutException) onThrottle(config, "请求超时");
            return CollectionResult.failed(pmid, rootMessage(e));
        }
    }

    /**
     * 在一个事务内批量保存当前网络批次的全部结果。
     * <p>
     * SUCCESS 先批量 INSERT IGNORE 文献再标记任务；EMPTY 不写文献表；
     * FAILED 通过 CASE WHEN 一次写入不同错误文本并计算重试状态。
     */
    private void persistBatch(List<CollectionResult> results, OpenLbCollectionStartRequest config) {
        var successes = results.stream().filter(result -> result.outcome == Outcome.SUCCESS).toList();
        var empty = results.stream().filter(result -> result.outcome == Outcome.EMPTY).toList();
        var failed = results.stream().filter(result -> result.outcome == Outcome.FAILED).toList();
        transactions.executeWithoutResult(status -> {
            if (!successes.isEmpty()) {
                routing.route(successes.stream().map(this::toWriteRow).toList());
                publicationIntegration.syncIdentifiers(successes.stream().map(CollectionResult::pmid).toList(), List.of());
                mapper.markSuccess(successes.stream().map(CollectionResult::pmid).toList());
            }
            if (!empty.isEmpty()) mapper.markEmpty(empty.stream().map(CollectionResult::pmid).toList());
            if (!failed.isEmpty()) mapper.markFailedOrPending(failed.stream()
                    .map(result -> new OpenLbCollectionMapper.FailureRow(result.pmid, abbreviate(result.error, 1000))).toList(), config.maxAttemptsOrDefault());
        });
    }

    /**
     * 将 OpenLB JSON 记录转换为 Mapper 批量写入模型。
     * 原始嵌套数组保留为 JSON 文本，无效年份转为 NULL。
     */
    private OpenLbPublicationWriteRow toWriteRow(CollectionResult result) {
        JsonNode row = result.record;
        // 采集只保留作者原始署名；机构解析在快照批处理中进行，避免逐篇调用ROR拖慢采集。
        String authors = compact(row.path("authors"));
        return OpenLbPublicationWriteRow.builder()
                .pmid(text(row, "pmid")).pubYear(validYear(row.path("year").asInt())).journalTitle(text(row, "journalTitle"))
                .title(text(row, "title")).abstractText(text(row, "abstractText")).affiliationInfo(affiliationSummary(row, authors))
                .keywordList(text(row, "keywordList")).doi(text(row, "doi")).sourceUrl(text(row, "url"))
                .authorsJson(authors).meshHeadingsJson(compact(row.path("meshHeadings")))
                .grantsJson(GrantPayloads.normalize(row.path("grants"), row.path("funding"), json))
                .cncbBanksJson(compact(row.path("cncbBanks"))).rawPayload(result.rawPayload)
                .sourceHash(sha256(compact(row))).build();
    }

    /**
     * 处理明确的限流或超时信号。
     * 全局请求间隔指数增大（上限10秒），连续次数达阈值时请求安全停止任务。
     */
    private void onThrottle(OpenLbCollectionStartRequest config, String reason) {
        long delay = currentDelayMs.updateAndGet(value -> Math.min(10000, Math.max(config.minDelayMsOrDefault(), value * 2)));
        int count = consecutiveThrottles.incrementAndGet();
        message = "检测到限流，已降速到 " + delay + " ms；连续次数 " + count;
        if (count >= config.stopThresholdOrDefault()) {
            message = "连续限流达到阈值，已自动停止：" + reason;
            stopRequested.set(true);
        }
    }

    /**
     * 记录一次成功请求。
     * 清零连续限流计数，并缓慢恢复到用户配置的最小间隔，避免立即提速再次触发限流。
     */
    private void onSuccess(long baseDelay) {
        consecutiveThrottles.set(0);
        currentDelayMs.updateAndGet(value -> Math.max(baseDelay, value - 25));
    }

    /**
     * 为全部工作线程分配下一个允许请求的时间片。
     * 使用 CAS 共享 nextRequestAt，保证 concurrency 不会绕过全局限速。
     */
    private void throttle() throws InterruptedException {
        long wait;
        while (true) {
            long now = System.currentTimeMillis();
            long observed = nextRequestAt.get();
            long target = Math.max(now, observed);
            if (nextRequestAt.compareAndSet(observed, target + currentDelayMs.get())) {
                wait = target - now;
                break;
            }
        }
        if (wait > 0) Thread.sleep(wait);
    }

    /**
     * 从 OpenLB 响应数组中优先找到 PMID 精确匹配的记录。
     */
    private JsonNode firstRecord(JsonNode payload, String pmid) {
        for (JsonNode node : payload.path("result").path("data"))
            if (pmid.equals(node.path("pmid").asText())) return node;
        return payload.path("result").path("data").isEmpty() ? null : payload.path("result").path("data").get(0);
    }

    /**
     * 将嵌套 JSON 压缩为单行文本；极端序列化失败时使用空数组保持字段可解析。
     */
    private String compact(JsonNode node) {
        try {
            return json.writeValueAsString(node);
        } catch (Exception e) {
            return "[]";
        }
    }

    /**
     * 顶层affiliationInfo为空时，从每位作者自己的affiliation汇总文章机构。
     */
    private String affiliationSummary(JsonNode row, String authorsJson) {
        String direct = text(row, "affiliationInfo");
        if (direct != null && !direct.isBlank()) return direct;
        LinkedHashSet<String> affiliations = new LinkedHashSet<>();
        try {
            JsonNode authors = json.readTree(authorsJson);
            if (authors.isArray()) authors.forEach(author -> {
                String value = text(author, "institutionName");
                if (value != null && !value.isBlank()) affiliations.add(value);
            });
        } catch (Exception ignored) {
        }
        if (affiliations.isEmpty()) {
            JsonNode authors = row.path("authors");
            if (authors.isArray()) authors.forEach(author -> {
                String value = text(author, "affiliation");
                if (value != null && !value.isBlank()) affiliations.add(value.trim().replaceAll("\\s+", " "));
            });
        }
        return affiliations.isEmpty() ? null : String.join("; ", affiliations);
    }

    /**
     * 读取可空文本字段，统一将 JSON null 转为 Java null。
     */
    private static String text(JsonNode node, String field) {
        var value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    /**
     * 只接受1500至2200年；OpenLB中的0或越界值按缺失处理。
     */
    private static Integer validYear(int year) {
        return year >= 1500 && year <= 2200 ? year : null;
    }

    /**
     * 限制持久化错误文本长度，避免超过 last_error 字段上限。
     */
    private static String abbreviate(String value, int max) {
        return value == null ? null : value.substring(0, Math.min(value.length(), max));
    }

    /**
     * 展开异常链并返回最底层可诊断原因。
     */
    private static String rootMessage(Throwable e) {
        while (e.getCause() != null) e = e.getCause();
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }

    /**
     * 生成稳定的SHA-256十六进制摘要，供批量upsert判断上游记录是否变化。
     */
    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("无法计算来源摘要", e);
        }
    }

    /**
     * 创建守护线程，防止采集线程在 Spring 容器关闭时独立阻塞 JVM 退出。
     */
    private static Thread daemon(Runnable r, String name) {
        var t = new Thread(r, name);
        t.setDaemon(true);
        return t;
    }

    private enum Outcome {SUCCESS, EMPTY, FAILED}

    /**
     * 单个 PMID 的内存采集结果，在网络层与批量持久化层之间传递。
     */
    private record CollectionResult(String pmid, Outcome outcome, JsonNode record, String rawPayload, String error) {
        static CollectionResult success(String pmid, JsonNode record, String rawPayload) {
            return new CollectionResult(pmid, Outcome.SUCCESS, record, rawPayload, null);
        }

        static CollectionResult empty(String pmid) {
            return new CollectionResult(pmid, Outcome.EMPTY, null, null, null);
        }

        static CollectionResult failed(String pmid, String error) {
            return new CollectionResult(pmid, Outcome.FAILED, null, null, error);
        }
    }

    /**
     * Spring 容器销毁时请求停止采集并关闭任务调度器。
     */
    @PreDestroy
    void shutdown() {
        stopRequested.set(true);
        jobExecutor.shutdownNow();
    }
}

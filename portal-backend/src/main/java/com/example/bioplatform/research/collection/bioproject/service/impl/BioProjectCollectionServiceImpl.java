package com.example.bioplatform.research.collection.bioproject.service.impl;

import com.example.bioplatform.common.BusinessException;
import com.example.bioplatform.research.collection.bioproject.dto.BioProjectCollectionStartRequest;
import com.example.bioplatform.research.collection.bioproject.dto.BioProjectEnrichmentTask;
import com.example.bioplatform.research.collection.bioproject.dto.BioProjectEnrichmentWrite;
import com.example.bioplatform.research.collection.bioproject.dto.BioProjectPublicationSeed;
import com.example.bioplatform.research.collection.bioproject.mapper.BioProjectCollectionMapper;
import com.example.bioplatform.research.collection.bioproject.service.BioProjectCollectionService;
import com.example.bioplatform.research.collection.bioproject.vo.BioProjectCollectionJob;
import com.example.bioplatform.research.collection.common.service.impl.CollectionHttpTransport;
import com.example.bioplatform.research.collection.common.service.impl.NativeResourceObservations;
import com.example.bioplatform.research.common.GrantPayloads;
import com.example.bioplatform.research.common.ResearchPublicationJsonMerger;
import com.example.bioplatform.research.integration.service.ResearchPublicationIntegrationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * BIOPROJECT项目出版文献独立采集：固定上游快照→流式批量导入→逐篇HTTP补全、批量写库→增量整合。
 */
@Service
@RequiredArgsConstructor
public class BioProjectCollectionServiceImpl implements BioProjectCollectionService {
    /**
     * resourceObservations：原库资源观察的规范化构造组件。
     */
    private final NativeResourceObservations resourceObservations;
    /**
     * mapper：本组件使用的持久层接口，SQL由对应XML维护。
     */
    private final BioProjectCollectionMapper mapper;
    /**
     * http：共享HTTP传输组件或HTTP客户端。
     */
    private final CollectionHttpTransport http = new CollectionHttpTransport();
    /**
     * metadata：来源元数据补全组件或规范化元数据结果。
     */
    private final BioProjectCollectionMetadataClient metadata;
    /**
     * integration：旧全量文献整合服务。
     */
    private final ResearchPublicationIntegrationService integration;
    /**
     * json：JSON解析与序列化组件。
     */
    private final ObjectMapper json;
    /**
     * transactions：事务执行组件。
     */
    private final TransactionTemplate transactions;
    /**
     * running：当前进程是否正在执行对应后台任务。
     * stopped：是否已经停止或检查停止状态的回调。
     */
    private final AtomicBoolean running = new AtomicBoolean(), stopped = new AtomicBoolean();
    /**
     * executor：本组件后台任务执行器。
     */
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "bioproject-collector");
        t.setDaemon(true);
        return t;
    });
    /**
     * message：响应说明、当前阶段说明或错误提示。
     * currentRecord：当前流式读取的原始记录。
     */
    private volatile String message = "未启动", currentRecord = "";
    /**
     * endpoint：上游接口地址，由配置或默认值提供。
     */
    @Value("${app.bioproject.statistics-endpoint:https://ngdc.cncb.ac.cn/bioproject/json/ngdcBrowse.json}")
    private String endpoint;
    /**
     * cacheDirectory：本地接口快照或下载缓存目录。
     */
    @Value("${app.bioproject.cache-directory:F:/project/bio-portal-suite/docs/bioproject-cache}")
    private String cacheDirectory;

    /**
     * 启动立即返回；STOPPED/FAILED恢复同一快照，SUCCESS后start建立新扫描。restart显式丢弃旧扫描游标。
     */
    @Override
    public synchronized Map<String, Object> start(BioProjectCollectionStartRequest request, boolean restart) {
        if (!running.compareAndSet(false, true)) throw conflict("BIOPROJECT采集正在运行");
        stopped.set(false);
        http.configure(request.delay());
        try {
            mapper.recover();
            BioProjectCollectionJob job = mapper.latest();
            if (!restart && job != null && Set.of("STOPPED", "FAILED").contains(job.getStatus())) {
                mapper.resume(job.getId());
                mapper.retryFailed();
            } else {
                job = new BioProjectCollectionJob();
                job.setCacheFile(Path.of(cacheDirectory).toAbsolutePath().normalize().resolve("bioproject-" + UUID.randomUUID() + ".json").toString());
                mapper.create(job);
            }
            long id = job.getId();
            message = restart ? "正在建立新的BIOPROJECT全量扫描" : "BIOPROJECT采集启动/恢复中";
            executor.submit(() -> run(id, request));
            return status();
        } catch (RuntimeException e) {
            running.set(false);
            throw e;
        }
    }

    /**
     * 停止下载、导入或补全，当前批次在事务提交后收尾；再次start续做。
     */
    @Override
    public Map<String, Object> stop() {
        stopped.set(true);
        message = "已请求停止，等待当前网络请求或批次收尾";
        return status();
    }

    /**
     * 同时返回内存进度和持久化作业、补全状态数量，不读取全部文献。
     */
    @Override
    public Map<String, Object> status() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("running", running.get());
        result.put("stopRequested", stopped.get());
        result.put("message", message);
        result.put("currentRecord", currentRecord);
        result.put("currentDelayMs", http.delay());
        result.put("job", mapper.latest());
        result.put("enrichmentCounts", mapper.counts());
        return result;
    }

    /**
     * 保留原始文献，仅重置BIOPROJECT元数据补全状态；不碰OpenLB任务表。
     */
    @Override
    public synchronized Map<String, Object> resetEnrichment() {
        if (running.get()) throw conflict("请先停止BIOPROJECT采集");
        int count = mapper.resetEnrichment();
        message = "已重置 " + count + " 条补全状态，再调用BIOPROJECT jobs/start";
        return status();
    }

    /**
     * 读取当前项目的完整文档，供项目出版列表与元数据解析复用。
     * @param row 本次处理的一条记录
     * @param config 本次来源或快照配置
     * @return 按上述规则计算或读取的JsonNode结果
     */
    private JsonNode projectDocument(JsonNode row, BioProjectCollectionStartRequest config) throws Exception {
        String code = BioProjectPublicationParser.projectCode(row);
        currentRecord = code;
        message = "读取BioProject项目 " + code;
        var project = BioProjectDocumentParser.fetch(code, json, url -> http.text(url, config.attempts(), stopped::get, v -> message = v));
        resourceObservations.bioProject(code, project);
        return project;
    }

    /**
     * 执行本任务或请求前检查回调；停止和异常处理遵循当前实现的约定。
     * @param jobId 所属持久化任务或作业ID
     * @param config 本次来源或快照配置
     */
    private void run(long jobId, BioProjectCollectionStartRequest config) {
        try {
            BioProjectCollectionJob job = mapper.latest();
            if (!Objects.equals(jobId, job.getId())) throw new IllegalStateException("BIOPROJECT作业ID已改变");
            Path file = Path.of(job.getCacheFile());
            String phase = job.getPhase();
            if ("DOWNLOAD".equals(phase)) {
                message = "正在读取BioProject官方项目列表";
                if (config.projectAccessions() != null && !config.projectAccessions().isEmpty()) {
                    Files.createDirectories(file.getParent());
                    var list = json.createArrayNode();
                    config.projectAccessions().stream().distinct().forEach(code -> list.addObject().put("accession", code));
                    json.writeValue(file.toFile(), list);
                } else http.download(endpoint, file, config.attempts(), stopped::get, v -> message = v);
                mapper.phase(jobId, "IMPORT");
                phase = "IMPORT";
            }
            if (stopped.get()) throw new InterruptedException("采集已停止");
            if ("IMPORT".equals(phase)) {
                if (!Files.isRegularFile(file))
                    throw new IllegalStateException("固定快照缓存缺失，不能续用旧索引；请调用jobs/restart建立新扫描");
                long after = job.getNextIndex() == null ? 0 : job.getNextIndex();
                boolean complete = BioProjectStatisticsReader.read(file, after, config.batch(), json, row -> projectDocument(row, config), (batch, next) -> {
                    if (stopped.get()) return false;
                    importBatch(jobId, batch, next);
                    message = "BIOPROJECT列表已导入到第 " + next + " 条（项目全部出版文献）";
                    return !stopped.get();
                });
                if (!complete) throw new InterruptedException("列表导入已停止");
                mapper.phase(jobId, "ENRICH");
            }
            enrich(jobId, config);
            if (stopped.get()) throw new InterruptedException("采集已停止");
            mapper.phase(jobId, "COMPLETE");
            mapper.finish(jobId, "SUCCESS", null);
            message = "BIOPROJECT主文献导入及元数据补全完成；NO_PMID/EMPTY详见计数，关联文献需另启公共任务";
            Files.deleteIfExists(file);
        } catch (InterruptedException e) {
            mapper.finish(jobId, "STOPPED", "人工停止；保留固定快照及已提交游标");
            message = "BIOPROJECT采集已停止，可以断点继续";
        } catch (Exception e) {
            mapper.finish(jobId, "FAILED", abbreviate(e));
            message = "BIOPROJECT采集失败并停止：" + abbreviate(e);
        } finally {
            currentRecord = "";
            running.set(false);
        }
    }

    /**
     * 批量查已有业务键，避免DOI补出PMID后再次扫描产生重复；断点与来源写入同事务。
     */
    private void importBatch(long jobId, List<JsonNode> records, long next) {
        if (records.isEmpty()) {
            mapper.advance(jobId, next, 0);
            return;
        }
        List<BioProjectPublicationSeed> parsed = records.stream().map(row -> BioProjectPublicationParser.parse(row, json)).toList();
        transactions.executeWithoutResult(tx -> {
            Map<String, BioProjectEnrichmentTask> existing = new HashMap<>();
            for (var row : mapper.existing(parsed)) {
                if (row.getRecordKey() != null) existing.put(row.getRecordKey(), row);
                if (row.getPmid() != null) existing.put("PMID:" + row.getPmid(), row);
                if (row.getDoi() != null) existing.put("DOI:" + row.getDoi().toLowerCase(Locale.ROOT), row);
            }
            Map<String, BioProjectPublicationSeed> unique = new LinkedHashMap<>();
            for (var row : parsed) {
                var old = existing.get(row.getRecordKey());
                if (old == null && row.getPmid() != null) old = existing.get("PMID:" + row.getPmid());
                if (old == null && row.getDoi() != null) old = existing.get("DOI:" + row.getDoi());
                if (old != null) {
                    row.setGrantsJson(GrantPayloads.merge(old.getGrantsJson(), row.getGrantsJson(), json));
                    if (old.getRecordKey() != null) row.setRecordKey(old.getRecordKey());
                    row.setCncbBanksJson(ResearchPublicationJsonMerger.mergeBanks(old.getCncbBanksJson(), row.getCncbBanksJson(), json));
                }
                var prior = unique.get(row.getRecordKey());
                if (prior != null) {
                    row.setGrantsJson(GrantPayloads.merge(prior.getGrantsJson(), row.getGrantsJson(), json));
                    row.setCncbBanksJson(ResearchPublicationJsonMerger.mergeBanks(prior.getCncbBanksJson(), row.getCncbBanksJson(), json));
                    row.setAccessions(union(prior.getAccessions(), row.getAccessions()));
                    row.setBioprojectAccession(union(prior.getBioprojectAccession(), row.getBioprojectAccession()));
                }
                row.setSourceHash(BioProjectPublicationParser.sha(String.join("|", row.getRecordKey(), Objects.toString(row.getTitle(), ""), Objects.toString(row.getPubYear(), ""), Objects.toString(row.getJournalTitle(), ""), row.getCncbBanksJson(), row.getGrantsJson())));
                unique.put(row.getRecordKey(), row);
            }
            var rows = new ArrayList<>(unique.values());
            mapper.upsertSeeds(rows);
            rows.stream().map(BioProjectPublicationSeed::getBioprojectAccession).filter(Objects::nonNull).distinct().forEach(code -> resourceObservations.retire("BIOPROJECT", code));
            integration.syncSourceKeys("BIOPROJECT", rows.stream().map(BioProjectPublicationSeed::getRecordKey).toList());
            mapper.advance(jobId, next, records.size());
        });
    }

    /**
     * 每批只持有有限条补全结果；成功记录及时写入，单条上游失败后停止并保存错误。
     */
    private void enrich(long jobId, BioProjectCollectionStartRequest config) throws Exception {
        while (!stopped.get()) {
            var tasks = mapper.pending(config.batch());
            if (tasks.isEmpty()) return;
            List<BioProjectEnrichmentWrite> results = new ArrayList<>();
            Exception failure = null;
            for (var task : tasks) {
                if (stopped.get()) break;
                currentRecord = task.getRecordKey() == null ? task.getPmid() : task.getRecordKey();
                message = "正在补全BIOPROJECT文献 " + currentRecord;
                try {
                    results.add(metadata.enrich(task, config, stopped::get, v -> message = v));
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    var failed = BioProjectCollectionMetadataClient.fromTask(task);
                    failed.setStatus("FAILED");
                    failed.setError(abbreviate(e));
                    results.add(failed);
                    failure = e;
                    break;
                }
            }
            if (!results.isEmpty()) transactions.executeWithoutResult(tx -> {
                Map<Long, BioProjectEnrichmentTask> latest = new HashMap<>();
                mapper.lockMetadata(results.stream().map(BioProjectEnrichmentWrite::getId).toList()).forEach(t -> latest.put(t.getId(), t));
                for (var result : results) {
                    var live = latest.get(result.getId());
                    if (live == null) throw new IllegalStateException("BIOPROJECT文献在补全期间被删除");
                    result.setGrantsJson(GrantPayloads.merge(live.getGrantsJson(), result.getGrantsJson(), json));
                    result.setCncbBanksJson(ResearchPublicationJsonMerger.mergeBanks(live.getCncbBanksJson(), result.getCncbBanksJson(), json));
                    result.setAuthorsJson(ResearchPublicationJsonMerger.mergeAuthors(live.getAuthorsJson(), result.getAuthorsJson(), json));
                    if (result.getSourceHash() != null) {
                        result.setSourceHash(null);
                        try {
                            result.setSourceHash(BioProjectPublicationParser.sha(json.writeValueAsString(result)));
                        } catch (Exception e) {
                            throw new IllegalStateException(e);
                        }
                    }
                }
                mapper.updateMetadata(results);
                // 按稳定来源键同步，包含暂时没有PMID的论文，避免只有部分文献进总库。
                Set<Long> ids = new HashSet<>(results.stream().map(BioProjectEnrichmentWrite::getId).toList());
                integration.syncSourceKeys("BIOPROJECT", tasks.stream().filter(t -> ids.contains(t.getId())).map(t -> t.getRecordKey() != null ? t.getRecordKey() : "PMID:" + t.getPmid()).toList());
                mapper.progress(jobId, results.size());
            });
            if (failure != null) throw failure;
        }
    }

    /**
     * 按业务值合并两个集合并去重，保留各自互补信息。
     * @param a 参与比较或合并的第一项
     * @param b 参与比较或合并的第二项
     * @return 按上述规则计算或读取的String结果
     */
    private static String union(String a, String b) {
        Set<String> set = new LinkedHashSet<>();
        for (String value : List.of(a == null ? "" : a, b == null ? "" : b))
            for (String item : value.split(",")) if (!item.isBlank()) set.add(item.trim());
        return String.join(",", set);
    }

    /**
     * 按持久化字段的长度上限截断说明文本，防止错误记录本身写库失败。
     * @param e 当前异常或XML元素，按声明类型解释
     * @return 按上述规则计算或读取的String结果
     */
    private static String abbreviate(Exception e) {
        String m = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        return m.length() > 1800 ? m.substring(0, 1800) : m;
    }

    /**
     * 构造带稳定错误码的业务冲突异常，交由全局异常处理器统一返回。
     * @param m 当前成员或合并目标
     * @return 按上述规则计算或读取的BusinessException结果
     */
    private static BusinessException conflict(String m) {
        return new BusinessException(HttpStatus.CONFLICT, "BIOPROJECT_JOB_RUNNING", m);
    }

    /**
     * 释放本轮缓存或网络响应等当前对象持有的资源，具体清理由本类职责决定。
     */
    @PreDestroy
    void close() {
        stopped.set(true);
        executor.shutdownNow();
    }
}

package com.example.bioplatform.research.collection.common.service.impl;

import com.example.bioplatform.common.BusinessException;
import com.example.bioplatform.research.collection.common.dto.*;
import com.example.bioplatform.research.collection.common.mapper.PublicationRelationCollectionMapper;
import com.example.bioplatform.research.collection.common.service.PublicationRelationCollectionService;
import com.example.bioplatform.research.collection.common.vo.PublicationRelationJobStatus;
import com.example.bioplatform.research.collection.common.vo.RelatedPublicationPage;
import com.example.bioplatform.research.common.GrantPayloads;
import com.example.bioplatform.research.integration.service.ResearchPublicationIntegrationService;
import com.example.bioplatform.research.integration.service.impl.ResearchPublicationSourceRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 单文献刷新及递归关联发现实现。
 * <p>只有人工调用start/resume才会运行；队列和visited均持久化，应用重启不会丢失进度。</p>
 */
@Service
@RequiredArgsConstructor
public class PublicationRelationCollectionServiceImpl implements PublicationRelationCollectionService {
    private final ResearchPublicationSourceRegistry sourceRegistry;

    private Set<String> databases() {
        var codes = new HashSet<String>();
        sourceRegistry.all().forEach(s -> codes.add(s.sourceCode()));
        return codes;
    }

    private final PublicationRelationCollectionMapper mapper;
    private final com.example.bioplatform.research.integration.mapper.ResearchResourceMapper resourceMapper;
    private final NativeRelatedPublicationWriter nativeRelated;
    private final ResearchPublicationIntegrationService publicationIntegration;
    private final com.example.bioplatform.research.collection.common.service.relation.RelationDiscoveryFactory discoveryFactory;
    private final com.example.bioplatform.research.collection.common.service.relation.RelatedPublicationStore publicationStore;
    private final com.example.bioplatform.research.collection.common.service.relation.RelationIdentifierFactory identifierFactory;
    private final com.example.bioplatform.research.collection.openlb.service.relation.OpenLbPublicationDetailsClient detailsClient;
    private final com.example.bioplatform.research.collection.common.service.relation.RelatedPublicationAssembler publicationAssembler;
    private final ObjectMapper json;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> daemon(r, "publication-relation-job"));
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean stopRequested = new AtomicBoolean(false);
    private volatile Long activeJobId;


    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public synchronized PublicationRelationJobStatus start(PublicationRelationCollectionRequest request) {
        if (!running.compareAndSet(false, true)) throw conflict("已有文献关联采集作业正在运行");
        try {
            RootIdentifier root = resolveRoot(request.databaseCode(), request.identifiers());
            mapper.createJob(root.databaseCode, root.type, root.value, request.refreshPublicationOrDefault(),
                    request.refreshRelationsOrDefault(), request.maxPublicationsOrDefault(), request.requestDelayMsOrDefault());
            Long jobId = mapper.lastInsertId();
            if (jobId == null) throw new IllegalStateException("无法取得关联采集作业ID");
            mapper.insertRootQueue(jobId, root.type, root.value);
            activeJobId = jobId;
            stopRequested.set(false);
            submitAfterCommit(() -> runJob(jobId, request.refreshPublicationOrDefault(), request.refreshRelationsOrDefault(),
                    request.maxPublicationsOrDefault(), request.requestDelayMsOrDefault()));
            return status(jobId);
        } catch (RuntimeException e) {
            running.set(false);
            throw e;
        }
    }

    /**
     * 自动从来源表领取待关联记录。根节点按来源表主键稳定排序并批量入队；队列和作业均已持久化，
     * 达到处理预算会暂停，应用中断后可以通过原有resume接口继续，不会重新领取已入队节点。
     */
    @Override
    @Transactional
    public synchronized PublicationRelationJobStatus startPendingBatch(PendingRelationBatchRequest request) {
        if (!running.compareAndSet(false, true)) throw conflict("已有文献关联采集作业正在运行");
        try {
            String databaseCode = normalizeDatabaseCode(request.databaseCode());
            List<PendingRelationRoot> roots = mapper.selectPendingRoots(
                    databaseCode, request.includeFailedOrDefault(), request.batchSizeOrDefault());
            var resourceRoots = resourceMapper.pending(databaseCode, request.includeFailedOrDefault(), request.batchSizeOrDefault());
            var combined = new ArrayList<PendingRelationRoot>();
            for (int i = 0; combined.size() < request.batchSizeOrDefault() && (i < roots.size() || i < resourceRoots.size()); i++) {
                if (i < roots.size()) combined.add(roots.get(i));
                if (combined.size() < request.batchSizeOrDefault() && i < resourceRoots.size())
                    combined.add(resourceRoots.get(i));
            }
            roots = combined;
            if (roots.isEmpty()) {
                running.set(false);
                throw new BusinessException(HttpStatus.CONFLICT, "NO_PENDING_RELATION_PUBLICATION",
                        databaseCode + " 暂无可领取的待关联文献");
            }
            mapper.createJob(databaseCode, "BATCH", "AUTO_PENDING_" + roots.size(), false, true,
                    request.maxPublicationsOrDefault(), request.requestDelayMsOrDefault());
            Long jobId = mapper.lastInsertId();
            if (jobId == null) throw new IllegalStateException("无法取得批量关联采集作业ID");
            mapper.insertRootQueueBatch(jobId, roots);
            activeJobId = jobId;
            stopRequested.set(false);
            submitAfterCommit(() -> runJob(jobId, false, true,
                    request.maxPublicationsOrDefault(), request.requestDelayMsOrDefault()));
            return status(jobId);
        } catch (RuntimeException e) {
            running.set(false);
            activeJobId = null;
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized PublicationRelationJobStatus resume(long jobId) {
        if (!running.compareAndSet(false, true)) throw conflict("已有文献关联采集作业正在运行");
        PublicationRelationJobStatus job = requireJob(jobId);
        if (mapper.resumeJob(jobId) == 0) {
            running.set(false);
            throw new BusinessException(HttpStatus.CONFLICT, "RELATION_JOB_NOT_RESUMABLE", "仅PAUSED、STOPPED或FAILED作业可以继续");
        }
        mapper.recoverRunningQueue(jobId);
        mapper.retryFailedQueue(jobId);
        activeJobId = jobId;
        stopRequested.set(false);
        executor.submit(() -> runJob(jobId, false, true, job.maxPublications(), job.requestDelayMs()));
        return status(jobId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PublicationRelationJobStatus stop() {
        stopRequested.set(true);
        if (activeJobId != null) mapper.finishJob(activeJobId, "STOPPED", "人工请求停止；当前节点完成后退出");
        return status(activeJobId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PublicationRelationJobStatus status(Long jobId) {
        PublicationRelationJobStatus row = mapper.selectJob(jobId);
        if (row == null) return new PublicationRelationJobStatus(running.get(), stopRequested.get(), null, null, null,
                null, "NOT_FOUND", null, null, 0, 0, 0, 0L, "暂无关联采集作业", null, null);
        return new PublicationRelationJobStatus(running.get() && Objects.equals(activeJobId, row.jobId()),
                stopRequested.get() && Objects.equals(activeJobId, row.jobId()), row.jobId(), row.databaseCode(),
                row.rootIdentifierType(), row.rootIdentifier(), row.status(), row.maxPublications(), row.requestDelayMs(),
                row.processedCount(), row.discoveredCount(), row.relationCount(), row.pendingCount(), row.lastError(),
                row.startedTime(), row.completedTime());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RelatedPublicationPage relatedPublications(String databaseCode, String identifierType, String identifier, int page, int size) {
        String db = databaseCode == null ? "" : databaseCode.trim().toUpperCase(Locale.ROOT);
        if (!databases().contains(db))
            throw new BusinessException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_RELATION_DATABASE", "该数据库尚未配置采集来源");
        String type = identifierType == null ? "" : identifierType.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("PMID", "PAPER_ID", "DOI", "SOURCE_KEY").contains(type))
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_IDENTIFIER_TYPE", "唯一值类型支持PMID、PAPER_ID、DOI及GSA的SOURCE_KEY");
        String value = "DOI".equals(type) ? normalizeDoi(identifier) : identifier.trim();
        RelationSourceRecord source = mapper.selectSourceRecord(db, type, value);
        if (source != null && source.pmid() != null && !source.pmid().isBlank()) {
            type = "PMID";
            value = source.pmid();
        }
        int safePage = Math.max(1, page), safeSize = Math.max(1, Math.min(size, 100));
        long total = mapper.countRelations(db, type, value);
        var found = mapper.selectRelations(db, type, value, (safePage - 1) * safeSize, safeSize);
        var resourceIds = found.stream().filter(row -> isResource(row.identifierType(), row.identifierValue())).map(row -> row.identifierValue().substring(9)).distinct().toList();
        Map<String, com.example.bioplatform.research.collection.common.dto.SourcePublicationRecord> byResource = new HashMap<>();
        if (!resourceIds.isEmpty())
            resourceMapper.keys(db, resourceIds).forEach(row -> byResource.put(row.getResourceAccession(), row));
        var items = found.stream().map(row -> {
            var resource = isResource(row.identifierType(), row.identifierValue()) ? byResource.get(row.identifierValue().substring(9)) : null;
            if (resource != null)
                return new com.example.bioplatform.research.collection.common.vo.RelatedPublicationView(row.relationId(), row.identifierType(), row.identifierValue(), null, null, resource.getTitle(), resource.getPubYear(), row.evidenceUrl(), row.discoveryDepth(), row.active(), resource.getResourceUrl(), null, null, resource.getCncbBanksJson(), "RESOURCE", resource.getResourceAccession(), resource.getResourceUrl());
            return new com.example.bioplatform.research.collection.common.vo.RelatedPublicationView(row.relationId(), row.identifierType(), row.identifierValue(), row.pmid(), row.doi(), row.title(), row.pubYear(), row.evidenceUrl(), row.discoveryDepth(), row.active(), com.example.bioplatform.research.common.PublicationLinks.external(row.pmid(), row.doi(), row.gsaAccessions(), row.bioprojectAccession(), row.cncbBanksJson(), json), row.gsaAccessions(), row.bioprojectAccession(), row.cncbBanksJson(), "PUBLICATION", null, null);
        }).toList();
        return new RelatedPublicationPage(safePage, safeSize, total, items);
    }

    /**
     * 按持久化队列处理输入根文献的一层关联，达到单次预算时暂停而不是丢弃队列。
     */
    private void runJob(long jobId, boolean refreshRoot, boolean refreshRelations, int maxPublications, long delayMs) {
        try (var cache = com.example.bioplatform.research.collection.common.service.relation.RelationRunCache.open()) {
            mapper.recoverRunningQueue(jobId);
            mapper.finishDescendantQueue(jobId);
            int processedThisRun = 0;
            while (!stopRequested.get()) {
                RelationQueueNode node = mapper.findNextPending(jobId);
                if (node == null) {
                    mapper.finishJob(jobId, "SUCCESS", null);
                    return;
                }
                if (processedThisRun >= maxPublications) {
                    mapper.finishJob(jobId, "PAUSED", "达到本次maxPublications安全上限，可人工继续");
                    return;
                }
                mapper.markQueueRunning(node.id());
                try {
                    processNode(jobId, node, refreshRoot && node.depth() == 0, refreshRelations, delayMs);
                    processedThisRun++;
                } catch (com.example.bioplatform.research.collection.common.service.relation.PublicationNotFoundException e) {
                    mapper.skipQueue(node.id(), e.getMessage());
                    updateRelationStatus(requireJob(jobId).databaseCode(), node.identifierType(), node.identifierValue(), "COMPLETED", e.getMessage());
                    mapper.incrementJob(jobId, 1, 0, 0);
                    processedThisRun++;
                } catch (Exception e) {
                    String error = abbreviate(rootMessage(e), 1000);
                    mapper.failQueue(node.id(), error);
                    updateRelationStatus(requireJob(jobId).databaseCode(), node.identifierType(), node.identifierValue(), "FAILED", error);
                    mapper.incrementJob(jobId, 1, 0, 0);
                    mapper.finishJob(jobId, "FAILED", error);
                    return; // 接口故障或限流立即停止，避免逐条请求同一失败上游。
                }
            }
            mapper.finishJob(jobId, "STOPPED", "人工请求停止");
        } catch (Exception e) {
            mapper.finishJob(jobId, "FAILED", abbreviate(rootMessage(e), 2000));
        } finally {
            running.set(false);
            activeJobId = null;
        }
    }

    /**
     * 处理一个节点：必要时刷新正文，读取cncbBanks链接，发现并入库关联文献和无序关系。
     */
    private void processNode(long jobId, RelationQueueNode node, boolean refreshPublication,
                             boolean refreshRelations, long delayMs) throws Exception {
        PublicationRelationJobStatus job = requireJob(jobId);
        RelationSourceRecord source = sourceRecord(job.databaseCode(), node.identifierType(), node.identifierValue());
        String pmid = source == null ? ("PMID".equals(node.identifierType()) ? node.identifierValue() : null) : source.pmid();
        if (refreshPublication && pmid != null) {
            RelatedPublicationWriteRow refreshed = fetchOpenLbPublication(pmid, false, delayMs);
            publicationStore.write(job.databaseCode(), List.of(refreshed));
            source = sourceRecord(job.databaseCode(), node.identifierType(), node.identifierValue());
        }
        if (source == null) throw new IllegalArgumentException("当前数据库中找不到唯一值对应的文献，请先采集主文献");
        if (!refreshRelations) {
            mapper.finishQueue(node.id(), "SUCCESS");
            mapper.incrementJob(jobId, 1, 0, 0);
            return;
        }
        updateRelationStatus(job.databaseCode(), node.identifierType(), node.identifierValue(), "RUNNING", null);

        var discovery = discoveryFactory.discover(new com.example.bioplatform.research.collection.common.service.relation.RelationDiscoveryInput(
                job.databaseCode(), node.identifierType(), node.identifierValue(), source.pmid(), source.cncbBanksJson(), source.bioprojectAccession(), nativeRelated.supportsSource(job.databaseCode())), () -> sleep(delayMs));
        Map<String, String> discovered = discovery.pmids();
        List<JsonNode> references = discovery.publicationRecords();
        JsonNode resourceGrants = discovery.grants();

        if (!resourceGrants.isEmpty() && (source.pmid() != null || Set.of("GSA", "BIOPROJECT").contains(job.databaseCode()) || isResource(node.identifierType(), node.identifierValue()))) {
            String grantsJson = GrantPayloads.merge(source.grantsJson(), resourceGrants.toString(), json);
            if (source.pmid() != null) {
                mapper.updateSourceGrants(job.databaseCode(), source.pmid(), grantsJson);
                publicationIntegration.syncIdentifiers(List.of(source.pmid()), List.of());
            } else if (isResource(node.identifierType(), node.identifierValue())) {
                resourceMapper.grants(job.databaseCode(), node.identifierValue().substring(9), grantsJson);
                publicationIntegration.syncSourceKeys(job.databaseCode(), List.of(node.identifierValue()));
            } else {
                if ("BIOPROJECT".equals(job.databaseCode()))
                    mapper.updateBioProjectSourceGrants(node.identifierType(), node.identifierValue(), grantsJson);
                else mapper.updateGsaSourceGrants(node.identifierType(), node.identifierValue(), grantsJson);
                String key = "SOURCE_KEY".equals(node.identifierType()) ? node.identifierValue() : node.identifierType() + ":" + node.identifierValue();
                publicationIntegration.syncSourceKeys(job.databaseCode(), List.of(key));
            }
        }

        int newPublications = 0;
        int relations = 0;
        for (Map.Entry<String, String> entry : discovered.entrySet()) {
            String relatedPmid = entry.getKey();
            if (stopRequested.get()) throw new InterruptedException("人工停止");
            if (com.example.bioplatform.research.collection.common.service.relation.RelationRunCache.isMissing(relatedPmid)) continue;
            if (!com.example.bioplatform.research.collection.common.service.relation.RelationRunCache.wasWritten(job.databaseCode(), relatedPmid)) {
                RelatedPublicationWriteRow related;
                try {
                    related = fetchOpenLbPublication(relatedPmid, true, delayMs);
                } catch (com.example.bioplatform.research.collection.common.service.relation.PublicationNotFoundException e) {
                    com.example.bioplatform.research.collection.common.service.relation.RelationRunCache.missing(relatedPmid);
                    continue;
                }
                publicationStore.write(job.databaseCode(), List.of(related));
                com.example.bioplatform.research.collection.common.service.relation.RelationRunCache.written(job.databaseCode(), relatedPmid);
            }
            CanonicalPair pair = pair(source.pmid() == null ? node.identifierType() : "PMID", source.pmid() == null ? node.identifierValue() : source.pmid(), "PMID", relatedPmid);
            mapper.upsertRelation(job.databaseCode(), pair.leftType, pair.leftValue, pair.rightType, pair.rightValue,
                    entry.getValue(), node.depth() + 1, jobId);
            int queued = mapper.insertTerminalQueue(jobId, "PMID", relatedPmid, node.depth() + 1);
            newPublications += queued;
            relations++;
        }
        for (JsonNode reference : references) {
            if (stopRequested.get()) throw new InterruptedException("人工停止");
            var identifier = nativeRelated.write(job.databaseCode(), reference, delayMs, stopRequested::get, source.pmid());
            String leftType = source.pmid() == null ? node.identifierType() : "PMID", leftValue = source.pmid() == null ? node.identifierValue() : source.pmid();
            if (leftType.equals(identifier.type()) && leftValue.equals(identifier.value())) continue;
            var canonical = pair(leftType, leftValue, identifier.type(), identifier.value());
            mapper.upsertRelation(job.databaseCode(), canonical.leftType, canonical.leftValue, canonical.rightType, canonical.rightValue,
                    "https://ngdc.cncb.ac.cn/bioproject/browse/" + com.example.bioplatform.research.common.PublicationValues.text(reference, "proAccession"), node.depth() + 1, jobId);
            newPublications += mapper.insertTerminalQueue(jobId, identifier.type(), identifier.value(), node.depth() + 1);
            relations++;
        }
        String relationType = source.pmid() == null ? node.identifierType() : "PMID";
        String relationValue = source.pmid() == null ? node.identifierValue() : source.pmid();
        mapper.deactivateUnseenRelations(job.databaseCode(), relationType, relationValue, jobId);
        mapper.finishQueue(node.id(), discovered.isEmpty() && references.isEmpty() ? "EMPTY" : "SUCCESS");
        updateRelationStatus(job.databaseCode(), node.identifierType(), node.identifierValue(), "COMPLETED", null);
        mapper.incrementJob(jobId, 1, newPublications, relations);
    }

    private static boolean isResource(String type, String key) {
        return "SOURCE_KEY".equals(type) && key != null && key.startsWith("RESOURCE:");
    }

    private RelationSourceRecord sourceRecord(String code, String type, String key) {
        return isResource(type, key) ? resourceMapper.relationSource(code, key.substring(9)) : mapper.selectSourceRecord(code, type, key);
    }

    private int updateRelationStatus(String code, String type, String key, String status, String error) {
        return isResource(type, key) ? resourceMapper.relationStatus(code, key.substring(9), status, error) : mapper.updatePublicationRelationStatus(code, type, key, status, error);
    }

    /**
     * 请求OpenLB PMID接口并构造来源表写入记录；cncbBanks为空仍是有效关联终点。
     */
    private RelatedPublicationWriteRow fetchOpenLbPublication(String pmid, boolean related, long delayMs) throws Exception {
        sleep(delayMs);
        return publicationAssembler.assemble(pmid, detailsClient.read(pmid), related);
    }

    /**
     * 根据数据库将同一OpenLB详情写入对应来源表；数据库之间不在采集阶段互相去重。
     */

    private RootIdentifier resolveRoot(String database, Map<String, String> identifiers) {
        String code = normalizeDatabaseCode(database);
        var id = identifierFactory.resolve(code, identifiers);
        return new RootIdentifier(code, id.type(), id.value());
    }

    /**
     * 校验并规范化当前已实现关联解析器的数据库编码。
     */
    private String normalizeDatabaseCode(String database) {
        String code = database == null ? "" : database.trim().toUpperCase(Locale.ROOT);
        if (!databases().contains(code)) throw new BusinessException(HttpStatus.BAD_REQUEST,
                "UNSUPPORTED_RELATION_DATABASE", "该数据库尚未配置采集来源");
        return code;
    }

    /**
     * 事务提交后再启动后台线程，确保作业和批量队列对后台连接可见。
     */
    private void submitAfterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    executor.submit(task);
                }
            });
        } else {
            executor.submit(task);
        }
    }

    /**
     * 对两个端点排序，使A-B与B-A写入同一个唯一键。
     */
    private CanonicalPair pair(String typeA, String valueA, String typeB, String valueB) {
        String a = typeA + ":" + valueA, b = typeB + ":" + valueB;
        return a.compareTo(b) <= 0 ? new CanonicalPair(typeA, valueA, typeB, valueB) : new CanonicalPair(typeB, valueB, typeA, valueA);
    }

    private PublicationRelationJobStatus requireJob(long jobId) {
        PublicationRelationJobStatus value = mapper.selectJob(jobId);
        if (value == null)
            throw new BusinessException(HttpStatus.NOT_FOUND, "RELATION_JOB_NOT_FOUND", "关联采集作业不存在：" + jobId);
        return value;
    }

    private BusinessException conflict(String message) {
        return new BusinessException(HttpStatus.CONFLICT, "RELATION_JOB_RUNNING", message);
    }

    private static String normalizeDoi(String value) {
        return value.trim().replaceFirst("(?i)^https?://(?:dx\\.)?doi\\.org/", "").toLowerCase(Locale.ROOT);
    }

    private static void sleep(long ms) throws InterruptedException {
        if (ms > 0) Thread.sleep(ms);
    }

    private static String abbreviate(String value, int max) {
        return value == null ? null : value.substring(0, Math.min(value.length(), max));
    }

    private static String rootMessage(Throwable e) {
        while (e.getCause() != null) e = e.getCause();
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }

    private static Thread daemon(Runnable r, String name) {
        Thread t = new Thread(r, name);
        t.setDaemon(true);
        return t;
    }


    private record RootIdentifier(String databaseCode, String type, String value) {
    }

    private record CanonicalPair(String leftType, String leftValue, String rightType, String rightValue) {
    }

    /**
     * 容器关闭时只停止本地工作线程，不触发或继续任何采集。
     */
    @PreDestroy
    void shutdown() {
        stopRequested.set(true);
        executor.shutdownNow();
    }
}

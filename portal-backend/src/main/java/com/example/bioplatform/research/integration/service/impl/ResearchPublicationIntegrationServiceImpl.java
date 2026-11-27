package com.example.bioplatform.research.integration.service.impl;

import com.example.bioplatform.common.BusinessException;
import com.example.bioplatform.research.collection.common.dto.SnapshotPublicationRow;
import com.example.bioplatform.research.collection.common.dto.SourcePublicationRecord;
import com.example.bioplatform.research.collection.common.service.ResearchPublicationSource;
import com.example.bioplatform.research.collection.common.vo.PublicationIntegrationStatus;
import com.example.bioplatform.research.common.GrantPayloads;
import com.example.bioplatform.research.common.ResearchPublicationJsonMerger;
import com.example.bioplatform.research.integration.service.ResearchPublicationIntegrationService;
import com.example.bioplatform.research.statistics.mapper.ResearchSnapshotMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * 将采集来源增量合并到一张全局去重文献表；快照构建不调用写入方法。
 */
@Service
@RequiredArgsConstructor
public class ResearchPublicationIntegrationServiceImpl implements ResearchPublicationIntegrationService {
    private static final int BATCH_SIZE = 500;
    private final ResearchSnapshotMapper mapper;
    private final ResearchPublicationSourceRegistry registry;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final AtomicBoolean running = new AtomicBoolean();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "research-publication-integrator");
        thread.setDaemon(true);
        return thread;
    });
    private volatile String message = "整合表尚未执行全量重建";
    private volatile long processed;
    private volatile long total;

    /**
     * 只读取与本次OpenLB PMID相关的来源行，并一并合并同PMID/DOI的GenBase记录。
     */
    @Override
    public synchronized void syncOpenLbPmids(List<String> pmids) {
        if (pmids == null || pmids.isEmpty()) return;
        syncIdentifiers(pmids, List.of());
    }

    /**
     * 只读取本批GenBase paper_id及其跨来源同PMID/DOI记录。
     */
    @Override
    public synchronized void syncGenBasePaperIds(List<Long> paperIds) {
        if (paperIds == null || paperIds.isEmpty()) return;
        sync(mapper.selectGenBaseRecordsByPaperIds(paperIds.stream().filter(Objects::nonNull).distinct().toList()));
    }

    /**
     * 关联采集写入时按PMID/DOI补齐已有来源记录后再更新整合表。
     */
    @Override
    public synchronized void syncIdentifiers(List<String> pmids, List<String> dois) {
        List<String> normalizedPmids = clean(pmids);
        List<String> normalizedDois = clean(dois);
        if (normalizedPmids.isEmpty() && normalizedDois.isEmpty()) return;
        List<SourcePublicationRecord> rows = new ArrayList<>();
        for (ResearchPublicationSource source : registry.all())
            rows.addAll(source.loadRecordsByIdentifiers(normalizedPmids, normalizedDois));
        sync(rows);
    }

    /**
     * 根据指定来源的业务键同步无PMID记录；不要求用标题猜测PMID。
     */
    @Override
    public synchronized void syncSourceKeys(String code, List<String> keys) {
        if (keys == null || keys.isEmpty()) return;
        sync(registry.require(code).loadRecordsBySourceKeys(keys));
    }

    /**
     * 立即返回状态，后台执行全量来源重读和批量upsert；历史引用的文献ID保持不变。
     */
    @Override
    public PublicationIntegrationStatus startFullRebuild() {
        if (mapper.countBuildingSnapshots() > 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "SNAPSHOT_ALREADY_BUILDING",
                    "统计快照正在生成，完成或停止后再重建整合表");
        }
        if (!running.compareAndSet(false, true)) return status();
        processed = 0;
        total = 0;
        message = "正在读取全部文献来源";
        executor.submit(this::rebuildAll);
        return status();
    }

    /**
     * 读取全部来源后，事务内先将旧整合行标记为失效，再批量重新激活当前数据。
     */
    private synchronized void rebuildAll() {
        try {
            List<ResearchPublicationSource> sources = registry.all(false);
            if (sources.isEmpty()) throw new IllegalStateException("没有已注册的文献来源");
            total = sources.stream().mapToLong(source -> source.countRecords(true)).sum();
            if (total == 0)
                throw new IllegalStateException("所有来源均未返回文献；为避免误将整合表全部标记失效，已取消重建");
            transactionTemplate.executeWithoutResult(tx -> {
                mapper.deactivateAllIntegratedPublications();
                for (ResearchPublicationSource source : sources) {
                    long afterId = 0;
                    List<SourcePublicationRecord> page;
                    while (!(page = source.loadRecordsPage(true, afterId, BATCH_SIZE)).isEmpty()) {
                        for (SourcePublicationRecord row : page) {
                            row.setSourceCode(source.sourceCode());
                            if (row.getSourceRowId() == null || row.getSourceRowId() <= afterId)
                                throw new IllegalStateException("来源分页缺少递增主键游标: " + source.sourceCode());
                            afterId = row.getSourceRowId();
                        }
                        // 每批仅保留当前来源页及其跨来源匹配记录；PMID/DOI跨库补齐由注册来源适配器完成。
                        sync(page);
                        processed = Math.min(total, processed + page.size());
                        message = "正在分批重建整合表 " + processed + "/" + total + "（" + source.sourceCode() + "）";
                    }
                }
            });
            message = "整合表全量重建完成";
        } catch (Exception ex) {
            message = "整合表重建失败：" + safeMessage(ex);
        } finally {
            running.set(false);
        }
    }

    /**
     * 对受影响的唯一值聚合来源记录，只写本批涉及的文献。
     */
    private void sync(List<SourcePublicationRecord> changedRows) {
        if (changedRows == null || changedRows.isEmpty()) return;
        var retired = changedRows.stream().filter(row -> "RESOURCE".equals(row.getRecordKind()) && Boolean.FALSE.equals(row.getRecordActive())).toList();
        if (!retired.isEmpty())
            mapper.deactivateIntegratedKeys(new ArrayList<>(IntegrationIdentityResolver.keys(retired)));
        changedRows = changedRows.stream().filter(row -> !Boolean.FALSE.equals(row.getRecordActive())).toList();
        if (changedRows.isEmpty()) return;
        Set<String> pmids = changedRows.stream().map(SourcePublicationRecord::getPmid).filter(ResearchPublicationIntegrationServiceImpl::notBlank).collect(Collectors.toCollection(TreeSet::new));
        Set<String> dois = changedRows.stream().map(SourcePublicationRecord::getDoi).filter(ResearchPublicationIntegrationServiceImpl::notBlank).map(ResearchPublicationIntegrationServiceImpl::normalizeDoi).collect(Collectors.toCollection(TreeSet::new));
        List<SourcePublicationRecord> all = new ArrayList<>(changedRows);
        if (!pmids.isEmpty() || !dois.isEmpty()) {
            for (ResearchPublicationSource source : registry.all())
                all.addAll(source.loadRecordsByIdentifiers(new ArrayList<>(pmids), new ArrayList<>(dois)));
        }
        List<String> candidateKeys = new ArrayList<>(IntegrationIdentityResolver.keys(all));
        var existing = mapper.selectIntegratedIdentityMatches(candidateKeys, new ArrayList<>(pmids), new ArrayList<>(dois));
        List<SnapshotPublicationRow> writes = new ArrayList<>();
        Set<String> obsolete = new LinkedHashSet<>();
        for (var group : IntegrationIdentityResolver.resolve(all, existing)) {
            MergedRecord merged = new MergedRecord(group.key());
            group.rows().forEach(row -> merged.merge(row, objectMapper));
            for (var previous : group.previous()) {
                if (!notBlank(merged.pmid)) merged.pmid = previous.getPmid();
                if (!notBlank(merged.doi)) merged.doi = previous.getDoi();
                if (!previous.getPublicationKey().equals(group.key())) obsolete.add(previous.getPublicationKey());
            }
            writes.add(toWriteRow(merged));
        }
        // 仅停用当前批次已确认重复的旧键；保留行供旧快照按旧键读取，不破坏历史穿透。
        if (!obsolete.isEmpty()) mapper.deactivateIntegratedKeys(new ArrayList<>(obsolete));
        for (int from = 0; from < writes.size(); from += BATCH_SIZE) {
            mapper.insertPublications(writes.subList(from, Math.min(from + BATCH_SIZE, writes.size())));
        }
    }

    /**
     * 将标准化记录映射为全局表批量upsert模型，并生成稳定内容摘要。
     */
    private SnapshotPublicationRow toWriteRow(MergedRecord row) {
        try {
            String affiliations = affiliationSummary(row.authorsJson, row.affiliationInfo);
            String recordKeys = objectMapper.writeValueAsString(row.recordKeys);
            String codes = String.join(",", row.sourceCodes);
            String content = String.join("\u001f", safe(row.kind), safe(row.resourceAccession), safe(row.resourceUrl), safe(row.pmid), safe(row.doi), safe(row.title), safe(row.pubYear),
                    safe(row.journalTitle), codes, recordKeys, safe(row.authorsJson), safe(affiliations),
                    safe(row.grantsJson), safe(row.keywordList), safe(row.meshHeadingsJson), safe(row.cncbBanksJson), String.valueOf(row.primary));
            return SnapshotPublicationRow.builder().publicationKey(row.key).contentHash(sha256(content)
                    ).recordKind(row.kind).resourceAccession(row.resourceAccession).resourceUrl(row.resourceUrl).pmid(row.pmid).doi(row.doi).title(row.title).pubYear(row.pubYear).journalTitle(row.journalTitle)
                    .sourceCodes(codes).sourceRecordKeys(recordKeys).authorsJson(row.authorsJson)
                    .affiliationInfo(affiliations).grantsJson(row.grantsJson).keywordList(row.keywordList)
                    .meshHeadingsJson(row.meshHeadingsJson).cncbBanksJson(row.cncbBanksJson)
                    .hasPrimarySource(row.primary).build();
        } catch (Exception ex) {
            throw new IllegalStateException("无法生成整合文献写入行", ex);
        }
    }

    /**
     * 全局去重键按PMID、DOI、来源记录键顺序选择。
     */
    private static final class MergedRecord {
        final String key;
        String kind = "PUBLICATION", resourceAccession, resourceUrl;
        String pmid, doi, title, journalTitle, authorsJson, affiliationInfo, grantsJson, keywordList, meshHeadingsJson, cncbBanksJson;
        Integer pubYear;
        boolean primary;
        final SortedSet<String> sourceCodes = new TreeSet<>();
        final Map<String, String> recordKeys = new TreeMap<>();

        MergedRecord(String key) {
            this.key = key;
        }

        void merge(SourcePublicationRecord row, ObjectMapper objectMapper) {
            kind = row.getRecordKind();
            resourceAccession = row.getResourceAccession();
            resourceUrl = row.getResourceUrl();
            sourceCodes.add(row.getSourceCode());
            recordKeys.put(row.getSourceCode(), row.getSourceRecordKey());
            primary |= Boolean.TRUE.equals(row.getPrimaryFlag());
            pmid = longer(pmid, row.getPmid());
            doi = longer(doi, row.getDoi());
            title = longer(title, row.getTitle());
            journalTitle = longer(journalTitle, row.getJournalTitle());
            authorsJson = ResearchPublicationJsonMerger.mergeAuthors(authorsJson, row.getAuthorsJson(), objectMapper);
            affiliationInfo = longer(affiliationInfo, row.getAffiliationInfo());
            grantsJson = GrantPayloads.merge(grantsJson, row.getGrantsJson(), objectMapper);
            keywordList = longer(keywordList, row.getKeywordList());
            meshHeadingsJson = longer(meshHeadingsJson, row.getMeshHeadingsJson());
            cncbBanksJson = ResearchPublicationJsonMerger.mergeBanks(cncbBanksJson, row.getCncbBanksJson(), objectMapper);
            if (pubYear == null && row.getPubYear() != null && row.getPubYear() > 1000 && row.getPubYear() < 3000)
                pubYear = row.getPubYear();
        }

        private static String longer(String left, String right) {
            if (!notBlank(right)) return left;
            return !notBlank(left) || right.length() > left.length() ? right : left;
        }
    }

    /**
     * 查询当前重建任务进度。
     */
    @Override
    public PublicationIntegrationStatus status() {
        return new PublicationIntegrationStatus(running.get(), message, processed, total);
    }

    private static List<String> clean(List<String> values) {
        if (values == null) return List.of();
        return values.stream().filter(ResearchPublicationIntegrationServiceImpl::notBlank).map(String::trim).distinct().toList();
    }

    /**
     * 清理DOI前缀及大小写，形成用于匹配的规范化值；不是联网解析DOI。
     */
    private static String normalizeDoi(String doi) {
        return doi == null ? null : doi.trim().replaceFirst("(?i)^https?://(dx\\.)?doi\\.org/", "").replaceFirst("(?i)^doi:\\s*", "").toLowerCase(Locale.ROOT);
    }

    private static String affiliationSummary(String authorsJson, String direct) {
        if (notBlank(direct)) return direct;
        if (!notBlank(authorsJson)) return null;
        try {
            Set<String> values = new LinkedHashSet<>();
            collectAffiliations(new ObjectMapper().readTree(authorsJson), values);
            return values.isEmpty() ? null : String.join("; ", values);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void collectAffiliations(com.fasterxml.jackson.databind.JsonNode node, Set<String> output) {
        if (node == null) return;
        if (node.isArray()) node.forEach(child -> collectAffiliations(child, output));
        else if (node.isObject()) node.fields().forEachRemaining(entry -> {
            if ("affiliation".equalsIgnoreCase(entry.getKey()) && entry.getValue().isTextual()) {
                String value = entry.getValue().asText().trim().replaceAll("\\s+", " ");
                if (!value.isEmpty()) output.add(value);
            } else collectAffiliations(entry.getValue(), output);
        });
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    /**
     * 计算稳定内容摘要，用于增量比较及唯一键辅助。
     */
    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("无法计算整合数据摘要", ex);
        }
    }

    private static String safeMessage(Exception ex) {
        return ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
    }
}

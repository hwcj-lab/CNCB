package com.example.bioplatform.research.statistics.service.impl;

import com.example.bioplatform.common.BusinessException;
import com.example.bioplatform.research.collection.common.dto.*;
import com.example.bioplatform.research.collection.common.service.ResearchPublicationSource;
import com.example.bioplatform.research.collection.common.vo.ResearchPublicationSourceOption;
import com.example.bioplatform.research.common.FundAgencyParser;
import com.example.bioplatform.research.integration.service.ResearchPublicationIntegrationService;
import com.example.bioplatform.research.integration.service.impl.ResearchPublicationSourceRegistry;
import com.example.bioplatform.research.statistics.dto.ResearchSnapshotBuildRequest;
import com.example.bioplatform.research.statistics.dto.ResearchSnapshotCreate;
import com.example.bioplatform.research.statistics.mapper.ResearchSnapshotMapper;
import com.example.bioplatform.research.statistics.service.ResearchSnapshotService;
import com.example.bioplatform.research.statistics.vo.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 旧版文献驱动快照实现，保留用于兼容和对照；当前默认Spring服务为PreparedSnapshotService。
 */
@RequiredArgsConstructor
public class ResearchSnapshotServiceImpl implements ResearchSnapshotService {
    /**
     * BATCH_SIZE：本组件批量处理的固定数量上限。
     */
    private static final int BATCH_SIZE = 500;
    /**
     * TYPES：本组件支持的维度或类型白名单。
     */
    private static final Set<String> TYPES = Set.of("YEAR", "TOPIC", "AUTHOR", "INSTITUTION", "FUND", "DATABASE");
    /**
     * registry：可用文献来源的注册中心。
     */
    private final ResearchPublicationSourceRegistry registry;
    /**
     * mapper：本组件使用的持久层接口，SQL由对应XML维护。
     */
    private final ResearchSnapshotMapper mapper;
    /**
     * integrationService：旧全量文献整合服务。
     */
    private final ResearchPublicationIntegrationService integrationService;
    /**
     * objectMapper：JSON解析与序列化组件。
     */
    private final ObjectMapper objectMapper;
    /**
     * running：当前进程是否正在执行对应后台任务。
     */
    private final AtomicBoolean running = new AtomicBoolean(false);
    /**
     * stopRequested：是否收到人工停止请求。
     */
    private final AtomicBoolean stopRequested = new AtomicBoolean(false);
    /**
     * executor：本组件后台任务执行器。
     */
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "research-snapshot-builder");
        thread.setDaemon(true);
        return thread;
    });
    /**
     * currentSnapshotId：当前正在构建的快照ID。
     */
    private volatile Long currentSnapshotId;
    /**
     * currentMessage：供状态接口展示的当前处理说明。
     */
    private volatile String currentMessage = "尚未启动统计";
    /**
     * 单批基金字典映射；避免逐条访问字典表，也避免在全量快照期间缓存全部文献对象。
     */
    private volatile Map<String, String> grantNameLookup = Map.of();

    /**
     * 创建BUILDING版本后在独立线程运行，避免HTTP连接等待整个汇总过程。
     */
    @Override
    public synchronized ResearchSnapshotStatus startBuild(ResearchSnapshotBuildRequest request) {
        if (integrationService.status().running()) {
            throw new BusinessException(HttpStatus.CONFLICT, "PUBLICATION_INTEGRATION_RUNNING",
                    "全局文献整合表正在重建，请完成后再生成统计快照");
        }
        if (mapper.countBuildingSnapshots() > 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "SNAPSHOT_ALREADY_BUILDING",
                    "已有统计快照正在生成，请等待完成或停止后再创建新快照");
        }
        if (!running.compareAndSet(false, true)) {
            throw new BusinessException(HttpStatus.CONFLICT, "SNAPSHOT_ALREADY_BUILDING",
                    "已有统计快照正在生成，请等待完成或停止后再创建新快照");
        }
        try {
            boolean includeResources = request != null && Boolean.TRUE.equals(request.getIncludeResources());
            List<ResearchPublicationSource> sources = registry.all(includeResources);
            if (sources.isEmpty()) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "NO_RESEARCH_SOURCE", "当前没有已注册的科研文献来源");
            }
            stopRequested.set(false);
            ResearchSnapshotCreate create = new ResearchSnapshotCreate();
            create.setVersionNo(mapper.selectNextVersionNo());
            String requestedName = request == null ? null : request.getName();
            create.setSnapshotName(notBlank(requestedName) ? requestedName.trim() : "统计快照 v" + create.getVersionNo());
            create.setSourceCodes(sources.stream().map(ResearchPublicationSource::sourceCode).collect(Collectors.joining(",")));
            create.setSourceCount(sources.size());
            create.setIncludeResources(includeResources);
            create.setIncludeRelated(request != null && Boolean.TRUE.equals(request.getIncludeRelated()));
            create.setStartedTime(LocalDateTime.now());
            mapper.insertSnapshot(create);
            currentSnapshotId = create.getId();
            currentMessage = "统计快照已启动";
            executor.submit(() -> build(create.getId(), sources, create.getIncludeRelated(), includeResources));
            return status();
        } catch (RuntimeException ex) {
            running.set(false);
            throw ex;
        }
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
     * 请求当前快照构建在检查点停止，避免发布仅生成一部分的统计结果。
     * @return 按上述规则计算或读取的ResearchSnapshotStatus结果
     */
    @Override
    public ResearchSnapshotStatus stopBuild() {
        if (running.get()) {
            stopRequested.set(true);
            currentMessage = "已请求停止，等待当前批次完成";
        }
        return status();
    }

    /**
     * 返回当前构建版本以及最近成功版本；构建失败不会影响旧版本读取。
     */
    @Override
    public ResearchSnapshotStatus status() {
        ResearchSnapshotDetail databaseBuilding = mapper.selectAnyBuildingSnapshot();
        ResearchSnapshotDetail current = databaseBuilding;
        if (current == null && currentSnapshotId != null) current = mapper.selectDetail(currentSnapshotId);
        boolean buildingExists = databaseBuilding != null;
        String message = buildingExists && !running.get()
                ? "检测到数据库中存在正在生成的统计快照"
                : currentMessage;
        return new ResearchSnapshotStatus(buildingExists || running.get(), message, current,
                mapper.selectLatestSuccess());
    }

    /**
     * 读取可查询的历史快照版本列表，具体格式由当前统计实现限定。
     * @return 按上述规则计算或读取的List<ResearchSnapshotDetail>结果
     */
    @Override
    public List<ResearchSnapshotDetail> versions() {
        return mapper.selectVersions();
    }

    /**
     * 返回最近成功发布的快照；正在构建或失败的版本不作为可用最新结果。
     * @return 按上述规则计算或读取的ResearchSnapshotDetail结果
     */
    @Override
    public ResearchSnapshotDetail latest() {
        return mapper.selectLatestSuccess();
    }

    /**
     * 读取最近成功快照的统计结果，没有可用版本时返回明确提示。
     * @param limitPerDimension 每个维度最多读取的排名数量
     * @return 按上述规则计算或读取的ResearchSnapshotResult结果
     */
    @Override
    public ResearchSnapshotResult latestResult(int limitPerDimension) {
        ResearchSnapshotDetail latest = mapper.selectLatestSuccess();
        if (latest == null)
            throw new BusinessException(HttpStatus.NOT_FOUND, "SNAPSHOT_NOT_FOUND", "尚无成功的统计快照");
        return result(latest.getId(), limitPerDimension);
    }

    /**
     * 按指定快照读取总统计结果，并根据请求限制排名展示数量。
     * @param snapshotId 统计快照主键
     * @param limitPerDimension 每个维度最多读取的排名数量
     * @return 按上述规则计算或读取的ResearchSnapshotResult结果
     */
    @Override
    public ResearchSnapshotResult result(long snapshotId, int limitPerDimension) {
        return scopedResult(snapshotId, "TOTAL", limitPerDimension);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ResearchSnapshotDatabaseOption> databases(long snapshotId) {
        detail(snapshotId);
        Map<String, String> names = registry.options().stream().collect(Collectors.toMap(
                ResearchPublicationSourceOption::code, ResearchPublicationSourceOption::name, (a, b) -> a));
        return mapper.selectDimensions(snapshotId, "TOTAL", "DATABASE", 1000).stream()
                .map(row -> new ResearchSnapshotDatabaseOption(row.getDimensionKey(),
                        names.getOrDefault(row.getDimensionKey(), row.getDisplayName()), row.getPublicationCount()))
                .toList();
    }

    /**
     * 按快照冻结的主题映射查找该分类对应的数据库，保持历史分类范围稳定。
     * @param snapshotId 统计快照主键
     * @param topicCode 总统计主题分类编码
     * @return 按上述规则计算或读取的List<ResearchSnapshotDatabaseOption>结果
     */
    @Override
    public List<ResearchSnapshotDatabaseOption> topicDatabases(long snapshotId, String topicCode) {
        String topic = topicCode == null ? "" : topicCode.trim().toUpperCase(Locale.ROOT);
        Set<String> codes = mapper.selectConfirmedDatabaseTopics().stream()
                .filter(row -> topic.equalsIgnoreCase(row.getTopicCode()))
                .map(row -> normalizeDatabaseCode(row.getDatabaseCode()))
                .collect(Collectors.toSet());
        return databases(snapshotId).stream()
                .filter(row -> codes.contains(normalizeDatabaseCode(row.code())))
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResearchSnapshotResult databaseResult(long snapshotId, String databaseCode, int limitPerDimension) {
        String code = databaseCode == null ? "" : databaseCode.trim().toUpperCase(Locale.ROOT);
        boolean exists = databases(snapshotId).stream().anyMatch(item -> item.code().equals(code));
        if (!exists) throw new BusinessException(HttpStatus.NOT_FOUND, "SNAPSHOT_DATABASE_NOT_FOUND",
                "该快照不包含数据库: " + databaseCode);
        return scopedResult(snapshotId, code, limitPerDimension);
    }

    /**
     * 按总统计或数据库范围分别读取，避免一次接口返回全部数据库的大结果。
     */
    private ResearchSnapshotResult scopedResult(long snapshotId, String scopeCode, int limitPerDimension) {
        ResearchSnapshotDetail snapshot = detail(snapshotId);
        int limit = Math.max(1, Math.min(limitPerDimension, 1000));
        /*
         * 维度表会随文献量快速增长。单条窗口函数需要扫描整个快照再进行分组排名，
         * 数据量较大时会超过前端超时时间。这里固定执行六次带 snapshot_id +
         * dimension_type 条件的索引查询，每次只返回页面所需的少量结果。
         */
        Map<String, List<ResearchSnapshotDimension>> grouped = new LinkedHashMap<>();
        TYPES.stream().sorted().forEach(type ->
                grouped.put(type, mapper.selectDimensions(snapshotId, scopeCode, type,
                        "YEAR".equals(type) ? 1000 : limit)));
        return new ResearchSnapshotResult(snapshot, grouped);
    }

    /**
     * 按标识读取当前资源或快照详情；不存在时由业务异常明确告知调用方。
     * @param snapshotId 统计快照主键
     * @return 按上述规则计算或读取的ResearchSnapshotDetail结果
     */
    @Override
    public ResearchSnapshotDetail detail(long snapshotId) {
        ResearchSnapshotDetail result = mapper.selectDetail(snapshotId);
        if (result == null)
            throw new BusinessException(HttpStatus.NOT_FOUND, "SNAPSHOT_NOT_FOUND", "统计快照不存在: " + snapshotId);
        return result;
    }

    /**
     * 仅允许白名单维度，且限制单次图表返回量。
     */
    @Override
    public List<ResearchSnapshotDimension> dimensions(long snapshotId, String type, int limit) {
        detail(snapshotId);
        String normalized = type == null ? "" : type.trim().toUpperCase(Locale.ROOT);
        if (!TYPES.contains(normalized))
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_DIMENSION", "不支持的统计维度: " + type);
        return mapper.selectDimensions(snapshotId, "TOTAL", normalized, Math.max(1, Math.min(limit, 1000)));
    }

    /**
     * 图表点击后分页返回构成该维度计数的具体文献。
     */
    @Override
    public ResearchSnapshotPublicationPage dimensionPublications(long snapshotId, long dimensionId, int page, int size) {
        detail(snapshotId);
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, Math.min(size, 100));
        String scopeCode = mapper.selectDimensionScope(snapshotId, dimensionId);
        if (scopeCode == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "SNAPSHOT_DIMENSION_NOT_FOUND", "统计维度不存在: " + dimensionId);
        }
        long total = mapper.countDimensionPublications(snapshotId, dimensionId);
        List<String> keys = mapper.selectDimensionPublicationKeys(snapshotId, dimensionId,
                (safePage - 1) * safeSize, safeSize);
        if (keys.isEmpty()) return new ResearchSnapshotPublicationPage(total, safePage, safeSize, List.of());
        List<ResearchSnapshotPublication> items;
        if ("TOTAL".equals(scopeCode)) {
            items = mapper.selectIntegratedPublicationDetails(keys);
            items.forEach(item -> item.setSourceUrl(com.example.bioplatform.research.common.PublicationLinks.external(item.getPmid(), item.getDoi(), item.getGsaAccessions(), item.getBioprojectAccession(), item.getCncbBanksJson(), objectMapper)));
            items.stream().filter(item -> "RESOURCE".equals(item.getRecordKind())).forEach(item -> item.setSourceUrl(item.getResourceUrl()));
        } else {
            ResearchPublicationSource source = registry.require(scopeCode);
            Map<String, SourcePublicationRecord> byKey = source.loadRecordsBySourceKeys(keys).stream()
                    .collect(Collectors.toMap(SourcePublicationRecord::getSourceRecordKey, Function.identity(),
                            (first, ignored) -> first, LinkedHashMap::new));
            items = keys.stream().map(key -> {
                SourcePublicationRecord row = byKey.get(key);
                if (row == null)
                    throw new IllegalStateException("来源记录未找到，范围=" + scopeCode + "，业务唯一值=" + key);
                return toSnapshotPublication(scopeCode, row);
            }).toList();
        }
        if (items.size() != keys.size()) {
            throw new IllegalStateException("穿透关系与文献记录数量不一致，范围=" + scopeCode
                    + "，关系键=" + keys.size() + "，实际记录=" + items.size());
        }
        return new ResearchSnapshotPublicationPage(total, safePage, safeSize, items);
    }

    /**
     * 将来源表记录映射为穿透响应；publicationKey在分库范围内就是该库的原生业务键。
     */
    private ResearchSnapshotPublication toSnapshotPublication(String scopeCode, SourcePublicationRecord row) {
        ResearchSnapshotPublication result = new ResearchSnapshotPublication();
        result.setScopeCode(scopeCode);
        result.setPublicationKey(row.getSourceRecordKey());
        result.setRecordKind(row.getRecordKind());
        result.setResourceAccession(row.getResourceAccession());
        result.setResourceUrl(row.getResourceUrl());
        result.setPmid(row.getPmid());
        result.setDoi(row.getDoi());
        result.setTitle(row.getTitle());
        result.setPubYear(row.getPubYear());
        result.setJournalTitle(row.getJournalTitle());
        result.setSourceCodes(scopeCode);
        result.setAuthorsJson(row.getAuthorsJson());
        result.setAffiliationInfo(row.getAffiliationInfo());
        result.setGrantsJson(row.getGrantsJson());
        result.setGsaAccessions(row.getGsaAccessions());
        result.setBioprojectAccession(row.getBioprojectAccession());
        result.setCncbBanksJson(row.getCncbBanksJson());
        result.setSourceUrl(com.example.bioplatform.research.common.PublicationLinks.external(row.getPmid(), row.getDoi(), row.getGsaAccessions(), row.getBioprojectAccession(), row.getCncbBanksJson(), objectMapper));
        if ("RESOURCE".equals(row.getRecordKind())) result.setSourceUrl(row.getResourceUrl());
        try {
            result.setSourceRecordKeys(objectMapper.writeValueAsString(Map.of(scopeCode, row.getSourceRecordKey())));
        } catch (Exception ex) {
            throw new IllegalStateException("无法序列化来源业务唯一值", ex);
        }
        return result;
    }

    /**
     * 删除一个非构建中的快照；数据库级联删除其全部子记录。
     */
    @Override
    @Transactional
    public void deleteSnapshot(long snapshotId) {
        ResearchSnapshotDetail snapshot = detail(snapshotId);
        if (running.get() && Objects.equals(currentSnapshotId, snapshotId)) {
            throw new BusinessException(HttpStatus.CONFLICT, "SNAPSHOT_BUILDING", "正在构建的快照不能删除，请先停止构建");
        }
        if ("BUILDING".equals(snapshot.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "SNAPSHOT_BUILDING", "BUILDING状态快照不能直接删除");
        }
        if (mapper.deleteSnapshot(snapshotId) != 1) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "SNAPSHOT_NOT_FOUND", "统计快照不存在: " + snapshotId);
        }
        if (Objects.equals(currentSnapshotId, snapshotId)) currentSnapshotId = null;
    }

    /**
     * 只修改快照展示名称，不改变统计结果或快照对应的数据范围。
     * @param snapshotId 统计快照主键
     * @param name 当前对象或资源的展示名称
     * @return 按上述规则计算或读取的ResearchSnapshotDetail结果
     */
    @Override
    public ResearchSnapshotDetail renameSnapshot(long snapshotId, String name) {
        detail(snapshotId);
        String clean = name == null ? "" : name.trim();
        if (clean.isEmpty() || clean.length() > 100) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_SNAPSHOT_NAME", "快照名称必须为1至100个字符");
        }
        mapper.updateSnapshotName(snapshotId, clean);
        return detail(snapshotId);
    }

    /**
     * 从已维护的全局整合表读取总统级数据、从来源表读取数据库内统计数据；本流程只写快照表。
     */
    private void build(long snapshotId, List<ResearchPublicationSource> sources, boolean includeRelated, boolean includeResources) {
        try {
            mapper.deleteDimensionCandidates(snapshotId);
            Map<String, List<ResearchDatabaseTopicMapping>> topicMappings = mapper.selectConfirmedDatabaseTopics().stream()
                    .collect(Collectors.groupingBy(row -> normalizeDatabaseCode(row.getDatabaseCode())));
            Map<String, String> topicNames = new HashMap<>();
            mapper.selectEnabledTopicCategories().forEach(row ->
                    topicNames.put(row.getTopicCode().toUpperCase(Locale.ROOT), row.getTopicName()));
            SnapshotCoverage coverage = new SnapshotCoverage();
            List<SnapshotDimensionCandidateRow> candidateBatch = new ArrayList<>(BATCH_SIZE);
            List<SnapshotDimensionPublicationRow> directRelationBatch = new ArrayList<>(500);
            List<SnapshotDimensionSeedRow> seeds = createDimensionSeeds(snapshotId, sources, includeRelated, includeResources, topicMappings, topicNames);
            if (!seeds.isEmpty()) mapper.insertDimensionSeeds(seeds);
            Map<String, Long> seedIds = mapper.selectDimensionSeeds(snapshotId).stream()
                    .collect(Collectors.toMap(row -> seedKey(row.getScopeCode(), row.getDimensionType(), row.getDimensionKey()),
                            SnapshotDimensionSeedRow::getId, (a, b) -> a));

            long afterId = 0;
            List<IntegratedPublicationRecord> totalPage;
            while (!(totalPage = mapper.selectIntegratedRecordPageForMode(includeRelated, includeResources, afterId, BATCH_SIZE)).isEmpty()) {
                checkStopped();
                grantNameLookup = loadGrantNames(totalPage.stream().map(IntegratedPublicationRecord::getGrantsJson).toList());
                for (IntegratedPublicationRecord row : totalPage) {
                    MergedPublication publication = MergedPublication.fromIntegrated(row);
                    appendPublicationCandidates(snapshotId, "TOTAL", publication, true, topicMappings,
                            topicNames, seedIds, candidateBatch, directRelationBatch, coverage);
                    afterId = row.getId();
                }
                flushCandidates(candidateBatch);
                flushDimensionRelations(directRelationBatch);
                currentMessage = "正在分析总统计文献，已处理至整合记录 " + afterId;
            }

            long sourceRecordCount = 0;
            for (ResearchPublicationSource source : sources) {
                checkStopped();
                long sourceAfterId = 0;
                List<SourcePublicationRecord> sourcePage;
                while (!(sourcePage = source.loadRecordsPage(includeRelated, sourceAfterId, BATCH_SIZE)).isEmpty()) {
                    checkStopped();
                    grantNameLookup = loadGrantNames(sourcePage.stream().map(SourcePublicationRecord::getGrantsJson).toList());
                    for (SourcePublicationRecord record : sourcePage) {
                        record.setSourceCode(source.sourceCode());
                        String key = sourceRecordKey(record);
                        MergedPublication publication = MergedPublication.fromSource(record, key);
                        appendPublicationCandidates(snapshotId, source.sourceCode(), publication, false,
                                topicMappings, topicNames, seedIds, candidateBatch, directRelationBatch, coverage);
                        if (record.getSourceRowId() == null || record.getSourceRowId() <= sourceAfterId)
                            throw new IllegalStateException("来源分页缺少递增主键游标: " + source.sourceCode());
                        sourceAfterId = record.getSourceRowId();
                        sourceRecordCount++;
                    }
                    flushCandidates(candidateBatch);
                    flushDimensionRelations(directRelationBatch);
                    currentMessage = "正在分析来源 " + source.sourceCode() + "，已处理至记录 " + sourceAfterId;
                }
            }
            if (mapper.countIntegratedRecordsForMode(includeRelated, includeResources) == 0 && sourceRecordCount > 0) {
                throw new IllegalStateException("全局整合文献表没有活动记录，请先执行全量重建");
            }
            checkStopped();
            currentMessage = "正在汇总年度和主题维度计数";
            mapper.refreshSeedDimensionCounts(snapshotId);
            mapper.deleteEmptySeedDimensions(snapshotId);
            currentMessage = "正在由数据库汇总作者、机构和基金并选择排名项";
            mapper.materializeDimensionsFromCandidates(snapshotId);
            currentMessage = "正在建立Top 50作者、机构和基金穿透关系";
            mapper.materializeDimensionPublications(snapshotId);
            mapper.deleteDimensionCandidates(snapshotId);
            checkStopped();
            long totalCount = mapper.countIntegratedRecordsForMode(includeRelated, includeResources);
            mapper.markSuccess(snapshotId, Math.toIntExact(totalCount), coverage.resources, coverage.year, coverage.topic,
                    coverage.author, coverage.institution, coverage.fund);
            currentMessage = "统计快照构建完成";
        } catch (SnapshotStoppedException ex) {
            mapper.deleteDimensionCandidates(snapshotId);
            mapper.deleteSnapshotDimensions(snapshotId);
            mapper.markStopped(snapshotId, "用户主动停止；已写入的未完成数据不会参与展示");
            currentMessage = "统计快照已停止";
        } catch (Exception ex) {
            String message = safeError(ex);
            mapper.deleteDimensionCandidates(snapshotId);
            mapper.deleteSnapshotDimensions(snapshotId);
            mapper.markFailed(snapshotId, message);
            currentMessage = "统计失败: " + message;
        } finally {
            running.set(false);
        }
    }

    /**
     * 将单篇文献转换为数据库统计候选行；只在当前文章范围内持有作者集合。
     */
    private void appendPublicationCandidates(long snapshotId, String scopeCode, MergedPublication publication,
                                             boolean total, Map<String, List<ResearchDatabaseTopicMapping>> topicMappings,
                                             Map<String, String> topicNames,
                                             Map<String, Long> seedIds, List<SnapshotDimensionCandidateRow> batch,
                                             List<SnapshotDimensionPublicationRow> relationBatch, SnapshotCoverage coverage) {
        if (total && "RESOURCE".equals(publication.kind)) coverage.resources++;
        if (publication.pubYear != null) {
            appendDirectRelation(seedIds, scopeCode, "YEAR", String.valueOf(publication.pubYear), publication.key, relationBatch);
            if (total) coverage.year++;
        }
        var parsed = new com.example.bioplatform.research.common.PublicationMetadataProcessor(objectMapper).parse(
                new com.example.bioplatform.research.common.PublicationMetadataProcessor.Input(publication.authorsJson, publication.affiliationInfo, publication.grantsJson, publication.keywordList, publication.meshHeadingsJson, publication.cncbBanksJson), grantNameLookup);
        Set<String> topics = total ? resourceTopicCodes(publication, topicMappings) : parsed.topics();
        if (total)
            topics.forEach(topic -> appendDirectRelation(seedIds, scopeCode, "TOPIC", topic, publication.key, relationBatch));
        else topics.forEach(topic -> appendCandidate(snapshotId, scopeCode, "TOPIC", normalizeKey(topic), topic,
                null, null, 0, publication.key, batch));
        var authors = parsed.authors();
        Set<String> institutions = parsed.institutions();
        Set<String> funds = parsed.funds();
        if (total) {
            if (!topics.isEmpty()) coverage.topic++;
            if (!authors.isEmpty()) coverage.author++;
            if (!institutions.isEmpty()) coverage.institution++;
            if (!funds.isEmpty()) coverage.fund++;
        }
        authors.forEach(author -> {
            String institution = notBlank(author.institution()) ? author.institution() : null;
            String label = author.name() + " · " + (institution == null ? "机构未提供" : institution);
            String key = normalizeKey(author.name()) + "|" + (institution == null ? "" : normalizeKey(institution));
            appendCandidate(snapshotId, scopeCode, "AUTHOR", key, label, author.name(), institution,
                    0, publication.key, batch);
        });
        institutions.forEach(value -> appendCandidate(snapshotId, scopeCode, "INSTITUTION",
                normalizeKey(value), value, null, null, 0, publication.key, batch));
        funds.forEach(value -> appendCandidate(snapshotId, scopeCode, "FUND",
                normalizeKey(value), value, null, null, 0, publication.key, batch));
    }

    /**
     * 预建当前快照实际年份、已确认主题以及来源分布维度，避免把这些关系重复写入候选表。
     */
    private List<SnapshotDimensionSeedRow> createDimensionSeeds(long snapshotId, List<ResearchPublicationSource> sources,
                                                                boolean includeRelated, boolean includeResources, Map<String, List<ResearchDatabaseTopicMapping>> mappings, Map<String, String> topicNames) {
        List<SnapshotDimensionSeedRow> seeds = new ArrayList<>();
        Set<Integer> totalYears = new TreeSet<>(mapper.selectIntegratedYearsForMode(includeRelated, includeResources));
        for (ResearchPublicationSource source : sources) totalYears.addAll(source.distinctYears(includeRelated));
        totalYears.forEach(year -> seeds.add(seed(snapshotId, "TOTAL", "YEAR", String.valueOf(year),
                String.valueOf(year), year, 0)));
        Set<String> totalTopics = new TreeSet<>(topicNames.keySet());
        totalTopics.forEach(topic -> seeds.add(seed(snapshotId, "TOTAL", "TOPIC", topic,
                topicNames.getOrDefault(topic.toUpperCase(Locale.ROOT), topic), 0, 0)));
        for (ResearchPublicationSource source : sources) {
            String code = source.sourceCode();
            Set<Integer> years = new TreeSet<>(source.distinctYears(includeRelated));
            years.forEach(year -> seeds.add(seed(snapshotId, code, "YEAR", String.valueOf(year),
                    String.valueOf(year), year, 0)));
            seeds.add(seed(snapshotId, "TOTAL", "DATABASE", code, source.sourceName(), 0,
                    source.countRecords(includeRelated)));
        }
        return seeds;
    }

    /**
     * 创建旧版年度或分类维度的预建统计行，供后续累计。
     * @param snapshotId 统计快照主键
     * @param scope 统计范围；TOTAL表示总统计，其余为指定来源编码
     * @param type 当前分类、维度或唯一值类型，按调用上下文解释
     * @param key 当前对象的业务匹配键
     * @param label 成员或页面的展示文本
     * @param year 当前记录或统计点对应的年份
     * @param count 当前查询或处理得到的数量
     * @return 按上述规则计算或读取的SnapshotDimensionSeedRow结果
     */
    private SnapshotDimensionSeedRow seed(long snapshotId, String scope, String type, String key,
                                          String label, int year, long count) {
        SnapshotDimensionSeedRow row = new SnapshotDimensionSeedRow();
        row.setSnapshotId(snapshotId);
        row.setScopeCode(scope);
        row.setDimensionType(type);
        row.setDimensionKey(truncate(key, 700));
        row.setDimensionHash(dimensionHash(type + "\u001f" + key + "\u001f" + year));
        row.setDisplayName(truncate(label, 1000));
        row.setStatYear(year);
        row.setPublicationCount(count);
        return row;
    }

    /**
     * 把旧版预建维度与文献的对应关系追加到批次，达到阈值后统一写入。
     * @param seedIds 旧版预建维度键到维度ID的映射
     * @param scope 统计范围；TOTAL表示总统计，其余为指定来源编码
     * @param type 当前分类、维度或唯一值类型，按调用上下文解释
     * @param key 当前对象的业务匹配键
     * @param publicationKey 统一文献身份键，用于统计去重和穿透定位
     * @param batch 当前批次数据或批次大小
     */
    private void appendDirectRelation(Map<String, Long> seedIds, String scope, String type, String key,
                                      String publicationKey, List<SnapshotDimensionPublicationRow> batch) {
        Long dimensionId = seedIds.get(seedKey(scope, type, key));
        if (dimensionId == null) return;
        SnapshotDimensionPublicationRow row = new SnapshotDimensionPublicationRow();
        row.setDimensionId(dimensionId);
        row.setPublicationKey(publicationKey);
        batch.add(row);
        if (batch.size() >= 500) flushDimensionRelations(batch);
    }

    /**
     * 构造旧版范围、维度类型及成员对应的预建统计点索引键。
     * @param scope 统计范围；TOTAL表示总统计，其余为指定来源编码
     * @param type 当前分类、维度或唯一值类型，按调用上下文解释
     * @param key 当前对象的业务匹配键
     * @return 按上述规则计算或读取的String结果
     */
    private String seedKey(String scope, String type, String key) {
        return scope + "\u001f" + type + "\u001f" + key;
    }

    /**
     * 总统计主题按实际采集来源数据库映射；全局去重文献对同一标签只计一次。
     */
    private Set<String> resourceTopicCodes(MergedPublication publication,
                                           Map<String, List<ResearchDatabaseTopicMapping>> mappings) {
        LinkedHashSet<String> databaseCodes = new LinkedHashSet<>();
        if (notBlank(publication.sourceCodes))
            for (String code : publication.sourceCodes.split(",")) {
                if (!"OPENLB".equals(normalizeDatabaseCode(code))) addNormalized(databaseCodes, code);
            }
        LinkedHashSet<String> topics = new LinkedHashSet<>();
        databaseCodes.forEach(code -> mappings.getOrDefault(code, List.of()).forEach(mapping -> topics.add(mapping.getTopicCode())));
        return topics;
    }

    /** 分库主题来自该库文献的关键词和 MeSH；每篇文献对同一主题只计一次。 */


    /**
     * 统一整理 MeSH 倒装词条；数据库类省略共同前缀，其余倒装词条改为自然语序。
     */


    private void addNormalized(Set<String> values, String value) {
        if (notBlank(value)) values.add(normalizeDatabaseCode(value));
    }

    /**
     * 清理数据库编码中的空白和符号，使配置别名能够稳定匹配。
     * @param value 待校验、转换或合并的值
     * @return 按上述规则计算或读取的String结果
     */
    private String normalizeDatabaseCode(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    /**
     * 批量写入旧版维度穿透关系并清空批次，不把全部关系永久保留在内存。
     * @param rows 本批待处理记录
     */
    private void flushDimensionRelations(List<SnapshotDimensionPublicationRow> rows) {
        if (rows.isEmpty()) return;
        checkStopped();
        mapper.insertDimensionPublications(rows);
        rows.clear();
    }

    /**
     * 候选行使用维度键摘要去重；每批达到上限即写库，不保留跨批文献关系集合。
     */
    private void appendCandidate(long snapshotId, String scopeCode, String type, String key, String label,
                                 String authorName, String institutionName, int year, String publicationKey,
                                 List<SnapshotDimensionCandidateRow> batch) {
        String cleanKey = truncate(key, 700);
        String hashInput = type + "\u001f" + cleanKey + "\u001f" + year;
        SnapshotDimensionCandidateRow row = new SnapshotDimensionCandidateRow();
        row.setSnapshotId(snapshotId);
        row.setScopeCode(scopeCode);
        row.setDimensionType(type);
        row.setDimensionHash(dimensionHash(hashInput));
        row.setDimensionKey(cleanKey);
        row.setDisplayName(truncate(label, 1000));
        row.setAuthorName(authorName == null ? null : truncate(authorName, 500));
        row.setInstitutionName(institutionName == null ? null : truncate(institutionName, 1000));
        row.setStatYear(year);
        row.setPublicationKey(publicationKey);
        batch.add(row);
        if (batch.size() >= 300) flushCandidates(batch);
    }

    /**
     * 计算旧版维度身份摘要，用于持久化唯一索引及候选去重。
     * @param value 待校验、转换或合并的值
     * @return 按上述规则计算或读取的byte[]结果
     */
    private byte[] dimensionHash(String value) {
        try {
            return java.security.MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("无法生成快照维度摘要", ex);
        }
    }

    /**
     * 批量写入旧版维度候选行并清空缓冲，供数据库后续聚合。
     * @param rows 本批待处理记录
     */
    private void flushCandidates(List<SnapshotDimensionCandidateRow> rows) {
        if (rows.isEmpty()) return;
        checkStopped();
        mapper.insertDimensionCandidates(rows);
        rows.clear();
    }

    /**
     * 旧版快照各类元数据的文献覆盖统计，用于区分成员数量与文献数量。
     */
    private static final class SnapshotCoverage {
        /**
         * resources：资源结果、资源来源或资源缓存，类型决定具体用途。
         */
        long resources;
        /**
         * year：当前记录或统计点对应的年份。
         * topic：研究主题编码或主题维度结果。
         * author：作者维度相关的处理结果或覆盖量。
         * institution：机构维度相关的处理结果或覆盖量。
         * fund：资助方维度相关的处理结果或覆盖量。
         */
        int year, topic, author, institution, fund;
    }

    /**
     * 根据维度按保守规则解析结构化JSON；无法确认的字段不会被猜测。
     */


    /**
     * 作者维度使用“署名作者 + 作者机构”。优先读作者自己的机构；作者缺失时，
     * 只有同篇其他作者的已知机构一致时才继承；存在不同机构时保持未知。
     */


    /**
     * 只为当前文献页中缺少agency的基金批量查询全称；不逐文献查库，不请求外部服务。
     */
    private Map<String, String> loadGrantNames(Collection<String> jsonValues) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (String value : jsonValues) {
            if (!notBlank(value)) continue;
            try {
                JsonNode root = objectMapper.readTree(value);
                if (root.isArray()) root.forEach(item -> {
                    if (FundAgencyParser.agency(item).isEmpty()) {
                        String id = FundAgencyParser.identifier(item);
                        if (!id.isEmpty()) ids.add(id);
                    }
                });
            } catch (Exception ignored) {
            }
        }
        if (ids.isEmpty()) return Map.of();
        Map<String, String> names = new HashMap<>();
        List<String> batch = new ArrayList<>(ids);
        for (int from = 0; from < batch.size(); from += 500) {
            List<ResearchFundDictionaryEntry> entries = mapper.selectFundDictionaryByGrantIds(
                    batch.subList(from, Math.min(from + 500, batch.size())));
            for (ResearchFundDictionaryEntry entry : entries) {
                String id = FundAgencyParser.clean(entry.getGrantId());
                String name = FundAgencyParser.clean(entry.getFullName());
                if (!id.isEmpty() && !name.isEmpty()) names.put(id.toLowerCase(Locale.ROOT), name);
            }
        }
        return names;
    }

    /** 每个基金只生成一个名称：agency优先，缺失时仅用本地编号字典全称；未匹配项舍弃。 */


    /** 清理文本两端空白后加入结果集，并跳过空字符串和null字面量。 */


    /**
     * 将冗长署名地址提取为主机构名称；原始地址保留在作者JSON中供追溯。
     */


    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * 构造或提取原库唯一值，供同来源更新及穿透定位。
     * @param record 待解析或写入的原始记录
     * @return 按上述规则计算或读取的String结果
     */
    private String sourceRecordKey(SourcePublicationRecord record) {
        if (notBlank(record.getSourceRecordKey())) return record.getSourceRecordKey().trim();
        if (notBlank(record.getPmid())) return "PMID:" + record.getPmid().trim();
        if (notBlank(record.getDoi())) return "DOI:" + normalizeDoi(record.getDoi());
        throw new IllegalStateException("来源记录缺少业务唯一值: " + record.getSourceCode());
    }

    /**
     * 清理DOI前缀及大小写，形成用于匹配的规范化值；不是联网解析DOI。
     */
    private String normalizeDoi(String doi) {
        return doi.trim().toLowerCase(Locale.ROOT).replaceFirst("^https?://(dx\\.)?doi\\.org/", "").replaceFirst("^doi:\\s*", "");
    }

    /**
     * 清理键值格式，用于当前范围内稳定比较和去重。
     * @param value 待校验、转换或合并的值
     * @return 按上述规则计算或读取的String结果
     */
    private String normalizeKey(String value) {
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }


    /**
     * 按指定最大长度截断字段或错误文本。
     * @param value 待校验、转换或合并的值
     * @param max 允许的最大值或文本长度上限
     * @return 按上述规则计算或读取的String结果
     */
    private String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }

    /**
     * 把错误内容转为长度受限的提示，避免超出状态字段长度。
     * @param ex 当前捕获的异常
     * @return 按上述规则计算或读取的String结果
     */
    private String safeError(Exception ex) {
        String m = ex.getMessage();
        return truncate(m == null ? ex.getClass().getSimpleName() : m, 2000);
    }

    /**
     * 在批次或维度边界检查人工停止标记，避免发布不完整快照。
     */
    private void checkStopped() {
        if (stopRequested.get()) throw new SnapshotStoppedException();
    }

    /**
     * 启动时关闭因进程终止遗留的BUILDING状态，避免页面永远显示运行中。
     */
    @PostConstruct
    public void recoverAbandonedBuilds() {
        mapper.failAbandonedBuilding();
        mapper.deleteOrphanDimensionCandidates();
        mapper.deleteIncompleteSnapshotDimensions();
    }

    /**
     * 应用退出时通知工作线程停止并等待收尾，超时后中断；已提交的数据库断点保留。
     */
    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }

    /**
     * 旧版快照在安全检查点响应停止请求的内部异常。
     */
    private static class SnapshotStoppedException extends RuntimeException {
    }


    /**
     * 单批统计使用的轻量文献视图，不保留来源关系或全量文献副本。
     */
    private static class MergedPublication {
        /**
         * key：当前对象的业务匹配键。
         */
        final String key;
        /**
         * pubYear：解析后的文献发表年份，缺失时为空。
         */
        Integer pubYear;
        /**
         * kind：当前记录或处理结果的类别。
         */
        String kind = "PUBLICATION";
        /**
         * authorsJson：逐作者原始信息及作者所属机构的JSON载荷。
         * affiliationInfo：来源提供的机构汇总原文，不应随意分配给所有作者。
         * grantsJson：保留完整资助记录的JSON载荷，便于整理时重新解析。
         * cncbBanksJson：CNCB资源关联数组，包含db、accession和link等。
         * keywordList：来源提供的论文关键词载荷。
         * meshHeadingsJson：PubMed MeSH主题词的原始JSON。
         * sourceCodes：参与本次处理的来源编码集合或拼接值。
         */
        String authorsJson, affiliationInfo, grantsJson, cncbBanksJson, keywordList, meshHeadingsJson, sourceCodes;

        /**
         * 构造MergedPublication，注入本组件所需依赖并建立初始状态。
         * @param key 当前对象的业务匹配键
         */
        MergedPublication(String key) {
            this.key = key;
        }

        /**
         * 把旧全量整合记录转换为旧快照内部的统一文献模型。
         * @param row 本次处理的一条记录
         * @return 按上述规则计算或读取的MergedPublication结果
         */
        static MergedPublication fromIntegrated(IntegratedPublicationRecord row) {
            MergedPublication item = new MergedPublication(row.getPublicationKey());
            item.kind = row.getRecordKind();
            item.pubYear = row.getPubYear();
            item.authorsJson = row.getAuthorsJson();
            item.affiliationInfo = row.getAffiliationInfo();
            item.grantsJson = row.getGrantsJson();
            item.cncbBanksJson = row.getCncbBanksJson();
            item.sourceCodes = row.getSourceCodes();
            item.keywordList = row.getKeywordList();
            item.meshHeadingsJson = row.getMeshHeadingsJson();
            return item;
        }

        /**
         * 把来源适配记录转换为旧快照内部的统一文献模型。
         * @param row 本次处理的一条记录
         * @param sourceKey 来源内的稳定业务唯一键
         * @return 按上述规则计算或读取的MergedPublication结果
         */
        static MergedPublication fromSource(SourcePublicationRecord row, String sourceKey) {
            MergedPublication item = new MergedPublication(sourceKey);
            item.kind = row.getRecordKind();
            item.pubYear = row.getPubYear();
            item.authorsJson = row.getAuthorsJson();
            item.affiliationInfo = row.getAffiliationInfo();
            item.grantsJson = row.getGrantsJson();
            item.cncbBanksJson = row.getCncbBanksJson();
            item.sourceCodes = row.getSourceCode();
            item.keywordList = row.getKeywordList();
            item.meshHeadingsJson = row.getMeshHeadingsJson();
            return item;
        }
    }
}

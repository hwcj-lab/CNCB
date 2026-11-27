package com.example.bioplatform.research.collection.common.service.impl;

import com.example.bioplatform.research.collection.bioproject.dto.BioProjectEnrichmentWrite;
import com.example.bioplatform.research.collection.bioproject.mapper.BioProjectCollectionMapper;
import com.example.bioplatform.research.collection.bioproject.service.impl.BioProjectPublicationParser;
import com.example.bioplatform.research.collection.bioproject.service.impl.BioProjectRelatedResourceResolver;
import com.example.bioplatform.research.collection.common.dto.PublicationEnrichmentTask;
import com.example.bioplatform.research.collection.gsa.dto.GsaEnrichmentWrite;
import com.example.bioplatform.research.collection.gsa.dto.GsaPublicationSeed;
import com.example.bioplatform.research.collection.gsa.mapper.GsaCollectionMapper;
import com.example.bioplatform.research.common.ResearchPublicationJsonMerger;
import com.example.bioplatform.research.integration.service.ResearchPublicationIntegrationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

/**
 * 无PMID项目论文沿用来源表稳定键写入；不创建额外论文表、不误标为主文献。
 */
@Component
public class NativeRelatedPublicationWriter {
    private final GsaCollectionMapper gsa;
    private final BioProjectCollectionMapper projects;
    private final ObjectMapper json;
    private final ResearchPublicationIntegrationService integration;
    private final TransactionTemplate tx;
    private final PublicationMetadataEnricher metadata;
    private final CollectionHttpTransport http = new CollectionHttpTransport();

    public NativeRelatedPublicationWriter(GsaCollectionMapper gsa, BioProjectCollectionMapper projects, ObjectMapper json, ResearchPublicationIntegrationService integration, TransactionTemplate tx, BioProjectRelatedResourceResolver resolver) {
        this.gsa = gsa;
        this.projects = projects;
        this.json = json;
        this.integration = integration;
        this.tx = tx;
        metadata = new PublicationMetadataEnricher(http, json, resolver);
    }

    public boolean supportsSource(String code) {
        return Set.of("GSA", "BIOPROJECT").contains(code);
    }

    public record Identifier(String type, String value) {
    }

    public Identifier write(String database, JsonNode reference, long delay, BooleanSupplier stop, String rootPmid) throws Exception {
        if (!Set.of("GSA", "BIOPROJECT").contains(database))
            throw new IllegalStateException("无PMID项目文献的稳定键写入尚未适配来源：" + database);
        var seed = BioProjectPublicationParser.parse(reference, json);
        seed.setPrimary(false);
        seed.setRelated(true);
        // 已存在的论文无需再次请求上游，仍由调用方写入关系并按visited队列去重。
        if ("BIOPROJECT".equals(database)) {
            var existing = projects.existing(List.of(seed));
            if (!existing.isEmpty()) {
                var old = existing.get(0);
                if (rootPmid != null && rootPmid.equals(old.getPmid()))
                    return identifier(old.getPmid(), old.getDoi(), old.getRecordKey());
                projects.markRelated(old.getId(), GrantPayloadsHelper.merge(old.getGrantsJson(), seed.getGrantsJson(), json), ResearchPublicationJsonMerger.mergeBanks(old.getCncbBanksJson(), seed.getCncbBanksJson(), json));
                integration.syncSourceKeys(database, List.of(old.getRecordKey() != null ? old.getRecordKey() : "PMID:" + old.getPmid()));
                return identifier(old.getPmid(), old.getDoi(), old.getRecordKey());
            }
        } else {
            var g = new GsaPublicationSeed();
            BeanUtils.copyProperties(seed, g);
            var existing = gsa.existing(List.of(g));
            if (!existing.isEmpty()) {
                var old = existing.get(0);
                if (rootPmid != null && rootPmid.equals(old.getPmid()))
                    return identifier(old.getPmid(), old.getDoi(), old.getRecordKey());
                gsa.markRelated(old.getId(), GrantPayloadsHelper.merge(old.getGrantsJson(), seed.getGrantsJson(), json), ResearchPublicationJsonMerger.mergeBanks(old.getCncbBanksJson(), seed.getCncbBanksJson(), json));
                integration.syncSourceKeys(database, List.of(old.getRecordKey() != null ? old.getRecordKey() : "PMID:" + old.getPmid()));
                return identifier(old.getPmid(), old.getDoi(), old.getRecordKey());
            }
        }
        var task = new PublicationEnrichmentTask();
        BeanUtils.copyProperties(seed, task);
        task.setCncbBanksJson(seed.getCncbBanksJson());
        task.setGrantsJson(seed.getGrantsJson());
        http.configure(delay);
        var enriched = metadata.enrich(task, 3, stop, v -> {
        });
        if (rootPmid != null && rootPmid.equals(enriched.getPmid())) return new Identifier("PMID", rootPmid);
        return tx.execute(status -> {
            String key = seed.getRecordKey();
            if ("BIOPROJECT".equals(database)) {
                var existing = projects.existing(List.of(seed));
                var old = existing.isEmpty() ? null : existing.get(0);
                if (old != null) {
                    if (old.getRecordKey() != null) seed.setRecordKey(old.getRecordKey());
                    seed.setGrantsJson(GrantPayloadsHelper.merge(old.getGrantsJson(), enriched.getGrantsJson(), json));
                    seed.setCncbBanksJson(ResearchPublicationJsonMerger.mergeBanks(old.getCncbBanksJson(), enriched.getCncbBanksJson(), json));
                } else {
                    seed.setGrantsJson(enriched.getGrantsJson());
                    seed.setCncbBanksJson(enriched.getCncbBanksJson());
                }
                seed.setPmid(enriched.getPmid());
                projects.upsertSeeds(List.of(seed));
                var stored = projects.existing(List.of(seed)).get(0);
                var write = new BioProjectEnrichmentWrite();
                BeanUtils.copyProperties(enriched, write);
                write.setId(stored.getId());
                write.setGrantsJson(seed.getGrantsJson());
                write.setCncbBanksJson(seed.getCncbBanksJson());
                write.setAuthorsJson(ResearchPublicationJsonMerger.mergeAuthors(stored.getAuthorsJson(), write.getAuthorsJson(), json));
                projects.updateMetadata(List.of(write));
                key = stored.getRecordKey() != null ? stored.getRecordKey() : "PMID:" + stored.getPmid();
            } else {
                var g = new GsaPublicationSeed();
                BeanUtils.copyProperties(seed, g);
                var existing = gsa.existing(List.of(g));
                var old = existing.isEmpty() ? null : existing.get(0);
                if (old != null) {
                    if (old.getRecordKey() != null) g.setRecordKey(old.getRecordKey());
                    g.setCncbBanksJson(ResearchPublicationJsonMerger.mergeBanks(old.getCncbBanksJson(), enriched.getCncbBanksJson(), json));
                } else g.setCncbBanksJson(enriched.getCncbBanksJson());
                g.setPmid(enriched.getPmid());
                gsa.upsertSeeds(List.of(g));
                var stored = gsa.existing(List.of(g)).get(0);
                var write = new GsaEnrichmentWrite();
                BeanUtils.copyProperties(enriched, write);
                write.setId(stored.getId());
                write.setGrantsJson(GrantPayloadsHelper.merge(stored.getGrantsJson(), enriched.getGrantsJson(), json));
                write.setCncbBanksJson(g.getCncbBanksJson());
                write.setAuthorsJson(ResearchPublicationJsonMerger.mergeAuthors(stored.getAuthorsJson(), write.getAuthorsJson(), json));
                gsa.updateMetadata(List.of(write));
                key = stored.getRecordKey() != null ? stored.getRecordKey() : "PMID:" + stored.getPmid();
            }
            integration.syncSourceKeys(database, List.of(key));
            return new Identifier(enriched.getPmid() != null ? "PMID" : seed.getDoi() != null ? "DOI" : "SOURCE_KEY", enriched.getPmid() != null ? enriched.getPmid() : seed.getDoi() != null ? seed.getDoi() : key);
        });
    }

    private static Identifier identifier(String pmid, String doi, String key) {
        return pmid != null ? new Identifier("PMID", pmid) : doi != null ? new Identifier("DOI", doi) : new Identifier("SOURCE_KEY", key);
    }

    private static class GrantPayloadsHelper {
        static String merge(String a, String b, ObjectMapper j) {
            return com.example.bioplatform.research.common.GrantPayloads.merge(a, b, j);
        }
    }
}

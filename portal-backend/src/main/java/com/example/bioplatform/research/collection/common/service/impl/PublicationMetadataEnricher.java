package com.example.bioplatform.research.collection.common.service.impl;

import com.example.bioplatform.research.collection.bioproject.service.impl.BioProjectRelatedResourceResolver;
import com.example.bioplatform.research.collection.common.dto.PublicationEnrichmentTask;
import com.example.bioplatform.research.collection.common.dto.PublicationEnrichmentWrite;
import com.example.bioplatform.research.common.GrantPayloads;
import com.example.bioplatform.research.common.PublicationMetadataProcessor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * 公共文献只用来源原有PMID查询OpenLB，空结果回退PubMed；无PMID不反查、不补写。
 */
@RequiredArgsConstructor
public class PublicationMetadataEnricher {
    private final CollectionHttpTransport http;
    private final ObjectMapper json;
    private final BioProjectRelatedResourceResolver projects;

    public PublicationEnrichmentWrite enrich(PublicationEnrichmentTask task, int attempts, BooleanSupplier stop, Consumer<String> progress) throws Exception {
        PublicationEnrichmentWrite out = fromTask(task);
        String pmid = task.getPmid();
        // PMID只采用当前来源原有值，不通过DOI检索推断或补写缺失PMID。
        JsonNode metadata = null;
        if (!blank(pmid)) {
            JsonNode root = json.readTree(http.text("https://ngdc.cncb.ac.cn/openlb/api/pmid?pmid=" + pmid, attempts, stop, progress));
            if (!"200".equals(root.path("code").asText()))
                throw new IllegalStateException("OpenLB返回业务错误：" + root.path("code").asText());
            JsonNode data = root.path("result").path("data");
            if (!data.isArray()) throw new IllegalStateException("OpenLB缺少result.data数组");
            for (JsonNode candidate : data)
                if (pmid.equals(candidate.path("pmid").asText())) {
                    metadata = candidate;
                    break;
                }
            if (metadata == null) {
                String xml = http.text("https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&retmode=xml&id=" + pmid, attempts, stop, progress);
                metadata = PubMedXmlMetadataParser.parse(xml, pmid, json);
            }
            out.setPmid(pmid);
        }
        if (metadata != null) {
            var processor = new PublicationMetadataProcessor(json);
            var merged = processor.merge(new PublicationMetadataProcessor.Input(task.getAuthorsJson(), task.getAffiliationInfo(), task.getGrantsJson(), task.getKeywordList(), task.getMeshHeadingsJson(), task.getCncbBanksJson()),
                    new PublicationMetadataProcessor.Input(array(metadata, "authors"), null, processor.normalizeGrants(metadata.path("grants"), metadata.path("funding")), null, null, array(metadata, "cncbBanks")));
            out.setAuthorsJson(merged.authorsJson());
            out.setAffiliationInfo(prefer(affiliations(out.getAuthorsJson()), task.getAffiliationInfo()));
            out.setGrantsJson(merged.grantsJson());
            out.setCncbBanksJson(merged.cncbBanksJson());
            out.setAbstractText(prefer(com.example.bioplatform.research.common.PublicationValues.text(metadata, "abstractText"), task.getAbstractText()));
            out.setMeshHeadingsJson(preferArray(array(metadata, "meshHeadings"), task.getMeshHeadingsJson()));
            String keywords = com.example.bioplatform.research.common.PublicationValues.text(metadata, "keywordList");
            if (blank(keywords) && metadata.path("keywords").isArray()) keywords = metadata.path("keywords").toString();
            out.setKeywordList(prefer(keywords, task.getKeywordList()));
            String title = com.example.bioplatform.research.common.PublicationValues.text(metadata, "title");
            if (!title.isBlank()) out.setTitle(title);
            String journal = com.example.bioplatform.research.common.PublicationValues.text(metadata, "journalTitle");
            if (!journal.isBlank()) out.setJournalTitle(journal);
            String doi = com.example.bioplatform.research.common.PublicationValues.normalizeDoi(com.example.bioplatform.research.common.PublicationValues.text(metadata, "doi"));
            if (!doi.isBlank()) out.setDoi(doi);
            int year = metadata.path("year").asInt(0);
            if (year >= 1500 && year <= 2200) out.setPubYear(year);
            out.setRawPayload(metadata.toString());
        }
        // 原文基金已有值则保留；仅不足时查询文章明确关联的项目，不把提交单位当作者机构。
        if (emptyArray(out.getGrantsJson()) && !blank(task.getBioprojectAccession())) {
            for (String project : task.getBioprojectAccession().split("[,;\\s]+")) {
                if (stop.getAsBoolean()) throw new InterruptedException("已停止");
                if (!project.matches("PRJ(?:CA|NA|EB|DA)[A-Z0-9]+")) continue;
                var data = projects.findRelatedPublications(project, null);
                out.setGrantsJson(GrantPayloads.merge(out.getGrantsJson(), data.grants().isArray() ? data.grants().toString() : "[]", json));
            }
        }
        out.setStatus(blank(pmid) ? "NO_PMID" : metadata == null ? "EMPTY" : "SUCCESS");
        out.setSourceHash(com.example.bioplatform.research.common.PublicationValues.sha(json.writeValueAsString(out)));
        return out;
    }

    /**
     * 失败或部分补全时保留原有字段，禁止用错误结果清空已有文献信息。
     */
    public static PublicationEnrichmentWrite fromTask(PublicationEnrichmentTask task) {
        PublicationEnrichmentWrite out = new PublicationEnrichmentWrite();
        out.setId(task.getId());
        out.setPmid(task.getPmid());
        out.setDoi(task.getDoi());
        out.setTitle(task.getTitle());
        out.setPubYear(task.getPubYear());
        out.setJournalTitle(task.getJournalTitle());
        out.setAuthorsJson(task.getAuthorsJson());
        out.setAffiliationInfo(task.getAffiliationInfo());
        out.setGrantsJson(task.getGrantsJson() == null ? "[]" : task.getGrantsJson());
        out.setCncbBanksJson(task.getCncbBanksJson() == null ? "[]" : task.getCncbBanksJson());
        out.setMeshHeadingsJson(task.getMeshHeadingsJson());
        out.setKeywordList(task.getKeywordList());
        out.setAbstractText(task.getAbstractText());
        return out;
    }

    private String affiliations(String authors) throws Exception {
        if (blank(authors)) return null;
        JsonNode list = json.readTree(authors);
        Set<String> values = new LinkedHashSet<>();
        if (list.isArray()) for (JsonNode author : list) {
            String affiliation = com.example.bioplatform.research.common.PublicationValues.text(author, "affiliation");
            if (!affiliation.isBlank()) values.add(affiliation);
        }
        return String.join("; ", values);
    }

    private boolean emptyArray(String value) throws Exception {
        if (blank(value)) return true;
        JsonNode node = json.readTree(value);
        return node == null || node.isNull() || node.isArray() && node.isEmpty();
    }

    private String preferArray(String newer, String old) {
        return "[]".equals(newer) ? old : newer;
    }

    private static String prefer(String newer, String old) {
        return blank(newer) ? old : newer;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static String array(JsonNode node, String key) {
        return node.path(key).isArray() ? node.path(key).toString() : "[]";
    }
}

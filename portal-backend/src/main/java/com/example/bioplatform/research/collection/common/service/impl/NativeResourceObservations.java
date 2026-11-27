package com.example.bioplatform.research.collection.common.service.impl;

import com.example.bioplatform.research.common.GrantPayloads;
import com.example.bioplatform.research.integration.dto.ResearchResourceInput;
import com.example.bioplatform.research.integration.service.impl.ResearchResourceService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 已确认的原库响应转换为资源观察；不从文章缺少PMID推断“没有文章”。
 */
@Component
@RequiredArgsConstructor
public class NativeResourceObservations {
    private final ResearchResourceService resources;
    private final ObjectMapper json;

    public void bioProject(String code, JsonNode project) {
        if (!project.path("listPublication").isArray()) throw new IllegalArgumentException("项目缺少原生出版信息数组");
        if (!project.path("listPublication").isEmpty()) return; // 原生论文完成入库后才停用旧无文献资源。
        var r = new ResearchResourceInput();
        r.setDatabaseCode("BIOPROJECT");
        r.setAccession(code);
        r.setTitle(text(project, "title"));
        r.setSourceUrl("https://ngdc.cncb.ac.cn/bioproject/browse/" + code);
        r.setHasPublication(false);
        r.setAffiliationInfo(project.path("submitter").path("organization").asText(null));
        r.setGrantsJson(GrantPayloads.normalize(project.path("listGrants"), project.path("funding"), json));
        r.setRawPayload(project.toString());
        r.setReleaseYear(year(text(project, "releaseTime")));
        var types = json.createArrayNode();
        for (JsonNode type : project.path("dataTypes")) {
            String value = type.isTextual() ? type.asText() : type.path("dataTypeName").asText("");
            if (!value.isBlank()) types.add(value);
        }
        r.setKeywordList(types.toString());
        resources.observe(List.of(r));
    }

    /**
     * 仅来源明确无文章标题/PMID/DOI的条目才走资源分支；有标题但无PMID仍是文章。
     */
    public boolean isResourceOnly(JsonNode row, String titleField, String pmidField) {
        if (row.path("hasPublication").isBoolean()) return !row.path("hasPublication").asBoolean();
        if (row.path("publications").isArray()) return row.path("publications").isEmpty();
        return false;
    }

    public void supportingResource(String source, JsonNode row) {
        String value = "GENBASE".equals(source) ? first(row, "accession", "accessionRange", "genbase_accessions") : first(row, "craAccession", "accession");
        for (String id : accessions(source, value)) {
            var r = new ResearchResourceInput();
            r.setDatabaseCode(source);
            r.setAccession(id);
            r.setHasPublication(false);
            r.setTitle(first(row, "resourceTitle", "description", "name"));
            r.setSourceUrl("GSA".equals(source) ? "https://ngdc.cncb.ac.cn/gsa/browse/" + id : first(row, "sourceUrl", "url"));
            if (r.getSourceUrl() == null || r.getSourceUrl().isBlank()) continue; // 没有确认的原库详情地址时不伪造URL或记录。
            r.setReleaseYear(year(first(row, "releaseDate", "releaseTime")));
            r.setRawPayload(row.toString());
            r.setGrantsJson(GrantPayloads.normalize(row.path("grants"), row.path("funding"), json));
            r.setAffiliationInfo(first(row, "organization", "affiliationInfo"));
            resources.observe(List.of(r));
        }
    }

    public void retire(String source, String raw) {
        if ("BIOPROJECT".equals(source)) {
            for (String id : raw.split("[,;\\s]+"))
                if (id.matches("PRJ(?:CA|NA|EB|DA)[A-Z0-9]+")) resources.retire(source, id);
            return;
        }
        for (String id : accessions(source, raw)) resources.retire(source, id);
    }

    static List<String> accessions(String source, String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        // 范围不是单个资源编号，不能将范围端点冒充全部资源，也不展开可能包含百万条的范围。
        if ("GENBASE".equals(source) && raw.matches("(?is).*C_[A-Z0-9]+(?:\\.[0-9]+)?\\s*[-~–]\\s*C_[A-Z0-9]+.*"))
            return List.of();
        Pattern p = Pattern.compile("GENBASE".equals(source) ? "(?i)\\bC_[A-Z0-9]+(?:\\.[0-9]+)?\\b" : "(?i)\\b(?:CRA|HRA|OMIX)[0-9]+\\b");
        Matcher m = p.matcher(raw);
        Set<String> ids = new LinkedHashSet<>();
        while (m.find()) ids.add(m.group().toUpperCase(Locale.ROOT));
        return List.copyOf(ids);
    }

    private static String first(JsonNode row, String... keys) {
        for (String key : keys) {
            String value = text(row, key);
            if (!value.isBlank()) return value;
        }
        return "";
    }

    private static String text(JsonNode row, String key) {
        return com.example.bioplatform.research.common.PublicationValues.text(row, key);
    }

    private static Integer year(String value) {
        if (value == null) return null;
        Matcher m = Pattern.compile("^(\\d{4})(?:$|[-/])").matcher(value);
        return m.find() ? Integer.valueOf(m.group(1)) : null;
    }
}

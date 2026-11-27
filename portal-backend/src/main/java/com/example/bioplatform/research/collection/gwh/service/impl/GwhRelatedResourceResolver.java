package com.example.bioplatform.research.collection.gwh.service.impl;

import com.example.bioplatform.research.collection.bioproject.service.impl.BioProjectRelatedResourceResolver;
import com.example.bioplatform.research.collection.common.service.RelatedResourceData;
import com.example.bioplatform.research.collection.common.service.RelatedResourceResolver;
import com.example.bioplatform.research.collection.common.service.impl.RelatedResourceHttpClient;
import com.example.bioplatform.research.collection.common.service.impl.ResourcePublicationParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GWH数字Assembly详情页：正文论文链接与项目链接均来自该组装记录，不采集页脚引用。
 */
@Component
@RequiredArgsConstructor
public class GwhRelatedResourceResolver implements RelatedResourceResolver {
    private final RelatedResourceHttpClient http;
    private final BioProjectRelatedResourceResolver projects;
    private final ObjectMapper json;

    @Override
    public List<String> supportedDatabaseNames() {
        return List.of("GWH", "Genome Warehouse");
    }

    @Override
    public RelatedResourceData findRelatedPublications(String accession, String url) throws Exception {
        Matcher id = Pattern.compile("/gwh/Assembly/([0-9]+)/show").matcher(url == null ? "" : url);
        String body;
        if (id.find()) body = http.get("https://ngdc.cncb.ac.cn/gwh/Assembly/" + id.group(1) + "/show");
        else {
            String code = ResourcePublicationParser.accession(accession, url, "GWH[A-Z]+[0-9]+(?:\\.[0-9]+)?").replaceFirst("\\.[0-9]+$", "");
            var assembly = json.readTree(http.get("https://ngdc.cncb.ac.cn/gwh/api/public/assembly/" + code));
            if (assembly.path("assemblyAccession").isNull() || assembly.path("assemblyAccession").isMissingNode())
                throw new IllegalStateException("GWH未找到组装：" + code);
            String project = assembly.path("bioprojectAccession").asText("");
            if (project.isBlank()) throw new IllegalStateException("GWH组装接口缺少关联项目：" + code);
            var data = projects.findRelatedPublications(project, null);
            Set<String> values = new LinkedHashSet<>(data.pmids());
            values.addAll(ResourcePublicationParser.pmids(assembly.path("publicationLink").asText("")));
            return new RelatedResourceData(List.copyOf(values), data.grants(), data.publicationRecords());
        }
        if (!body.contains("Publication(s)") && !body.contains("/bioproject/browse/"))
            throw new IllegalStateException("GWH详情页结构不匹配");
        Set<String> pmids = new LinkedHashSet<>(ResourcePublicationParser.pmids(body));
        var grants = json.createArrayNode();
        List<com.fasterxml.jackson.databind.JsonNode> references = new ArrayList<>();
        for (String project : ResourcePublicationParser.projects(body)) {
            var data = projects.findRelatedPublications(project, null);
            pmids.addAll(data.pmids());
            references.addAll(data.publicationRecords());
            if (data.grants().isArray()) data.grants().forEach(grants::add);
        }
        return new RelatedResourceData(List.copyOf(pmids), grants, List.copyOf(references));
    }
}

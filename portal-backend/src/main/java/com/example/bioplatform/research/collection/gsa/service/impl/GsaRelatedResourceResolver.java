package com.example.bioplatform.research.collection.gsa.service.impl;

import com.example.bioplatform.research.collection.common.service.RelatedResourceData;
import com.example.bioplatform.research.collection.common.service.RelatedResourceResolver;
import com.example.bioplatform.research.collection.bioproject.service.impl.BioProjectRelatedResourceResolver;
import com.example.bioplatform.research.collection.common.service.impl.RelatedResourceHttpClient;
import com.example.bioplatform.research.collection.common.service.impl.ResourcePublicationParser;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * GSA：使用资源搜索页读取本资源明确关联的BioProject，再读取项目文献与基金。
 */
@Component
@RequiredArgsConstructor
public class GsaRelatedResourceResolver implements RelatedResourceResolver {
    private final RelatedResourceHttpClient http;
    private final BioProjectRelatedResourceResolver projects;
    private final ObjectMapper json;

    @Override
    public List<String> supportedDatabaseNames() {
        return List.of("GSA", "Genome Sequence Archive");
    }

    @Override
    public RelatedResourceData findRelatedPublications(String accession, String url) throws Exception {
        String code = ResourcePublicationParser.accession(accession, url, "CRA[0-9]+");
        String body = http.get("https://ngdc.cncb.ac.cn/gsa/search?searchTerm=" + code);
        if (!body.contains(code)) throw new IllegalStateException("GSA未返回目标资源详情：" + code);
        Set<String> pmids = new LinkedHashSet<>(ResourcePublicationParser.pmids(body));
        var grants = json.createArrayNode();
        List<com.fasterxml.jackson.databind.JsonNode> references = new ArrayList<>();
        List<String> projectCodes = ResourcePublicationParser.projects(body);
        if (projectCodes.isEmpty() && pmids.isEmpty())
            throw new IllegalStateException("GSA页面未找到项目或文献，保留失败状态以便核对：" + code);
        for (String project : projectCodes) {
            var data = projects.findRelatedPublications(project, null);
            pmids.addAll(data.pmids());
            references.addAll(data.publicationRecords());
            if (data.grants().isArray()) data.grants().forEach(grants::add);
        }
        return new RelatedResourceData(List.copyOf(pmids), grants, List.copyOf(references));
    }
}

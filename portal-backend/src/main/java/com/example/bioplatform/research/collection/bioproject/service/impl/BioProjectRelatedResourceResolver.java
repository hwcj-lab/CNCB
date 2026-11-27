package com.example.bioplatform.research.collection.bioproject.service.impl;

import com.example.bioplatform.research.collection.common.service.RelatedResourceData;
import com.example.bioplatform.research.collection.common.service.RelatedResourceResolver;
import com.example.bioplatform.research.collection.common.service.impl.RelatedResourceHttpClient;
import com.example.bioplatform.research.collection.common.service.impl.ResourcePublicationParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * BioProject：使用已验证GWH公开项目接口读取listPublication和listGrants。
 */
@Component
@RequiredArgsConstructor
public class BioProjectRelatedResourceResolver implements RelatedResourceResolver {
    private final RelatedResourceHttpClient http;
    private final ObjectMapper json;

    @Override
    public List<String> supportedDatabaseNames() {
        return List.of("BioProject", "Biological Project Library");
    }

    @Override
    public RelatedResourceData findRelatedPublications(String accession, String url) throws Exception {
        String code = ResourcePublicationParser.accession(accession, url, "PRJ(?:CA|NA|EB|DA)[A-Z0-9]+");
        JsonNode root = BioProjectDocumentParser.fetch(code, json, http::get);
        JsonNode publications = root.path("listPublication");
        Set<String> pmids = new LinkedHashSet<>();
        for (JsonNode item : publications) {
            String pmid = item.path("pubmedId").asText("").trim();
            if (pmid.matches("[1-9][0-9]{5,11}")) pmids.add(pmid);
        }
        List<JsonNode> references = new ArrayList<>();
        for (JsonNode item : publications) {
            var paper = item.deepCopy();
            if (!paper.isObject()) continue;
            var object = (com.fasterxml.jackson.databind.node.ObjectNode) paper;
            object.put("proAccession", code);
            object.set("projectGrants", root.path("listGrants"));
            object.set("projectOrganization", root.path("submitter").path("organization"));
            references.add(paper);
        }
        return new RelatedResourceData(List.copyOf(pmids), root.path("listGrants"), references);
    }
}

package com.example.bioplatform.research.integration.service.impl;

import com.example.bioplatform.research.integration.dto.ResearchResourceInput;
import com.example.bioplatform.research.integration.mapper.ResearchResourceMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.List;
import java.util.Set;

/**
 * 无文献资源直接写入全局整合表；不重复保存，不运行采集或猜测PMID。
 */
@Service
@RequiredArgsConstructor
public class ResearchResourceService {
    private final ResearchResourceMapper mapper;
    private final ResearchPublicationSourceRegistry registry;
    private final ObjectMapper json;

    @Transactional
    public int observe(List<ResearchResourceInput> rows) {
        int changed = 0;
        for (var row : rows) {
            String code = registry.require(row.getDatabaseCode()).sourceCode();
            row.setDatabaseCode(code);
            row.setAccession(row.getAccession().trim());
            if (Boolean.TRUE.equals(row.getHasPublication())) {
                changed += mapper.retire(code, row.getAccession());
            } else {
                URI uri = URI.create(row.getSourceUrl());
                if (!Set.of("http", "https").contains(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null)
                    throw new IllegalArgumentException("资源来源必须是HTTP(S)详情地址");
                if (row.getTitle() == null || row.getTitle().isBlank()) row.setTitle(row.getAccession());
                if (row.getAuthorsJson() == null) row.setAuthorsJson("[]");
                if (row.getGrantsJson() == null) row.setGrantsJson("[]");
                if (row.getCncbBanksJson() == null) {
                    var banks = json.createArrayNode();
                    banks.addObject().put("db", code).put("accession", row.getAccession()).put("link", row.getSourceUrl());
                    row.setCncbBanksJson(banks.toString());
                }
                try {
                    for (String value : List.of(row.getAuthorsJson(), row.getGrantsJson(), row.getCncbBanksJson()))
                        if (!json.readTree(value).isArray())
                            throw new IllegalArgumentException("资源作者/基金/关联JSON必须是数组");
                } catch (java.io.IOException e) {
                    throw new IllegalArgumentException("资源JSON不合法", e);
                }
                if (row.getRawPayload() == null) row.setRawPayload("{}");
                try {
                    var raw = json.readTree(row.getRawPayload());
                    if (raw.path("listPublication").isArray() && !raw.path("listPublication").isEmpty() || raw.path("publications").isArray() && !raw.path("publications").isEmpty())
                        throw new IllegalArgumentException("资源有原生文章，不能按无文献资源入库");
                } catch (java.io.IOException e) {
                    throw new IllegalArgumentException("资源原始JSON不合法", e);
                }
                var old = mapper.keys(code, List.of(row.getAccession()));
                if (!old.isEmpty())
                    row.setGrantsJson(com.example.bioplatform.research.common.GrantPayloads.merge(old.get(0).getGrantsJson(), row.getGrantsJson(), json));
                changed += mapper.upsert(row, com.example.bioplatform.research.common.PublicationValues.sha(row.toString()), json.createObjectNode().put(code, "RESOURCE:" + row.getAccession()).toString());
            }
        }
        return changed;
    }

    /**
     * 已完成原生文章入库之后调用，避免资源和自己的文章双重计数。
     */
    public void retire(String source, String accession) {
        mapper.retire(source, accession);
    }
}

package com.example.bioplatform.research.collection.biocode.service.impl;

import com.example.bioplatform.research.collection.common.service.RelatedResourceData;
import com.example.bioplatform.research.collection.common.service.RelatedResourceResolver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * BioCode工具详情解析器：BT000004映射为工具接口ID 4。
 */
@Component
@RequiredArgsConstructor
public class BioCodeRelatedResourceResolver implements RelatedResourceResolver {
    /**
     * json：JSON解析与序列化组件。
     */
    private final ObjectMapper json;
    /**
     * http：共享HTTP传输组件或HTTP客户端。
     */
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    /**
     * 声明本解析器支持的cncbBanks.db名称及别名，由公共工厂匹配。
     */
    @Override
    public List<String> supportedDatabaseNames() {
        return List.of("BioCode", "Biological Tool Codes");
    }

    /**
     * 读取本资源明确关联的出版记录及基金，返回统一结构；发现结果由公共采集流程去重、入库。
     */
    @Override
    public RelatedResourceData findRelatedPublications(String accession, String officialUrl) throws Exception {
        String id = accession == null ? "" : accession.trim().replaceFirst("(?i)^BT", "");
        if (!id.matches("\\d+")) throw new IllegalArgumentException("BioCode编号格式无效: " + accession);
        String url = "https://ngdc.cncb.ac.cn/biocode/api/tool/" + id;
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(25))
                .header("Accept", "application/json").GET().build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) throw new IllegalStateException("BioCode接口HTTP " + response.statusCode());
        JsonNode body = json.readTree(response.body());
        List<String> pmids = new ArrayList<>();
        JsonNode publications = body.path("publications");
        if (publications.isArray()) for (JsonNode publication : publications) {
            String pmid = publication.path("pmid").asText("").trim();
            if (pmid.matches("[1-9][0-9]{5,11}")) pmids.add(pmid);
        }
        return new RelatedResourceData(pmids.stream().distinct().toList(), body.path("grants"));
    }
}

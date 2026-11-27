package com.example.bioplatform.research.collection.bioproject.service.impl;

import com.example.bioplatform.research.common.GrantPayloads;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BioProject元数据读取器；缓存项目响应，避免同一项目的多篇论文重复请求。
 */
@Component
@RequiredArgsConstructor
public class BioProjectMetadataClient {
    /**
     * json：JSON解析与序列化组件。
     */
    private final ObjectMapper json;
    /**
     * http：共享HTTP传输组件或HTTP客户端。
     */
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    /**
     * cache：当前组件的响应缓存或解析缓存。
     */
    private final Map<String, JsonNode> cache = new ConcurrentHashMap<>();

    /**
     * 读取GWH公开BioProject JSON；无法匹配或不可访问时返回null，不阻断论文采集。
     */
    public JsonNode fetch(String accession) {
        String code = normalize(accession);
        if (code == null) return null;
        return cache.computeIfAbsent(code, this::request);
    }

    /**
     * 保留PubMed原始grants；为空时将funding编号包装，再补充BioProject资助对象。
     */
    public ArrayNode mergedGrants(JsonNode pubmedGrants, JsonNode funding, JsonNode project) {
        String primary = GrantPayloads.normalize(pubmedGrants, funding, json);
        ArrayNode result;
        try {
            result = (ArrayNode) json.readTree(primary);
        } catch (Exception ignored) {
            result = json.createArrayNode();
        }
        appendArray(result, project == null ? null : project.path("listGrants"));
        try {
            return (ArrayNode) json.readTree(GrantPayloads.merge(result.toString(), null, json));
        } catch (Exception ignored) {
            return result;
        }
    }

    /**
     * 从BioProject响应读取公开发表信息中列出的PMID。
     */
    public List<String> publicationPmids(JsonNode project) {
        if (project == null || !project.path("listPublication").isArray()) return List.of();
        return java.util.stream.StreamSupport.stream(project.path("listPublication").spliterator(), false)
                .map(row -> row.path("pubmedId").asText("").trim())
                .filter(value -> value.matches("[1-9][0-9]{5,11}")).distinct().toList();
    }

    /**
     * 请求已确认的项目接口并返回项目元数据，不按标题猜测文献身份。
     * @param accession 资源在原始数据库中的登录号
     * @return 按上述规则计算或读取的JsonNode结果
     */
    private JsonNode request(String accession) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://ngdc.cncb.ac.cn/gwh/api/public/bioProject/" + accession))
                    .timeout(Duration.ofSeconds(20)).header("Accept", "application/json").GET().build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) return json.getNodeFactory().nullNode();
            JsonNode body = json.readTree(response.body());
            return body.path("message").asText().equalsIgnoreCase("SUCCESS") ? body : json.getNodeFactory().nullNode();
        } catch (Exception ignored) {
            return json.getNodeFactory().nullNode();
        }
    }

    /**
     * 把有效数组条目追加到目标载荷，保留原始条目字段。
     * @param target 接收合并或解析结果的目标对象
     * @param values 待遍历或去重的值集合
     */
    private void appendArray(ArrayNode target, JsonNode values) {
        if (values != null && values.isArray()) values.forEach(value -> target.add(value.deepCopy()));
    }

    /**
     * 整理当前业务字符串的空白、大小写或固定前缀，供后续匹配使用。
     * @param accession 资源在原始数据库中的登录号
     * @return 按上述规则计算或读取的String结果
     */
    private String normalize(String accession) {
        if (accession == null) return null;
        String code = accession.trim().split("[;,\\s]+", 2)[0];
        return code.matches("(?i)PRJ(CA|NA|EB|DA)[A-Z0-9]+") ? code : null;
    }
}

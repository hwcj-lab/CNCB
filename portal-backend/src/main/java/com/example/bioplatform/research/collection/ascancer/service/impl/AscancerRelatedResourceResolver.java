package com.example.bioplatform.research.collection.ascancer.service.impl;

import com.example.bioplatform.research.collection.common.service.RelatedResourceData;
import com.example.bioplatform.research.collection.common.service.RelatedResourceResolver;
import com.example.bioplatform.research.collection.common.service.impl.RelatedResourceHttpClient;


import com.fasterxml.jackson.databind.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * ASCancerAtlas：按官方前端knowledge接口及零起始分页读取事件关联论文；异常不标记完成。
 */
@Component
@RequiredArgsConstructor
public class AscancerRelatedResourceResolver implements RelatedResourceResolver {
    /**
     * http：共享HTTP传输组件或HTTP客户端。
     */
    private final RelatedResourceHttpClient http;
    /**
     * json：JSON解析与序列化组件。
     */
    private final ObjectMapper json;
    /**
     * endpoint：上游接口地址，由配置或默认值提供。
     */
    @Value("${app.ascancer.knowledge-endpoint:https://ngdc.cncb.ac.cn/ascancer/api/browse/knowledge}")
    private String endpoint;

    /**
     * 声明本解析器支持的cncbBanks.db名称及别名，由公共工厂匹配。
     */
    @Override
    public List<String> supportedDatabaseNames() {
        return List.of("ASCancerAtlas", "ASCancer Atlas");
    }

    /**
     * 读取本资源明确关联的出版记录及基金，返回统一结构；发现结果由公共采集流程去重、入库。
     */
    @Override
    public RelatedResourceData findRelatedPublications(String accession, String url) throws Exception {
        String event = accession;
        if (url != null) {
            String query = URI.create(url.replace(" ", "%20")).getRawQuery();
            if (query != null) for (String pair : query.split("&"))
                if (pair.startsWith("eventid=")) {
                    // 示例URL的裸+代表正链符号，必须转义后传给后端，不能被表单解码为空格。
                    event = URLDecoder.decode(pair.substring(8).replace("+", "%2B"), StandardCharsets.UTF_8);
                }
        }
        if (event == null || event.isBlank()) throw new IllegalArgumentException("ASCancerAtlas缺少eventid");
        Set<String> pmids = new LinkedHashSet<>();
        Set<String> pages = new HashSet<>();
        for (int page = 0; page < 100; page++) {
            JsonNode root = json.readTree(http.get(endpoint + "?eventid=" + URLEncoder.encode(event, StandardCharsets.UTF_8) + "&page=" + page + "&size=100"));
            JsonNode items = root.path("items");
            if (!items.isArray()) throw new IllegalStateException("ASCancerAtlas接口缺少items数组");
            if (items.isEmpty()) return new RelatedResourceData(List.copyOf(pmids), json.createArrayNode());
            if (!pages.add(items.toString())) throw new IllegalStateException("ASCancerAtlas分页重复，已停止以免漏采");
            for (JsonNode item : items) collect(item, pmids);
            if (items.size() < 100) return new RelatedResourceData(List.copyOf(pmids), json.createArrayNode());
        }
        throw new IllegalStateException("ASCancerAtlas超过100页限制，保留失败记录以便处理");
    }

    /**
     * 只认明确的文献标识字段，其他数字不解析为PMID。
     */
    private void collect(JsonNode node, Set<String> pmids) {
        if (node.isArray()) {
            node.forEach(child -> collect(child, pmids));
            return;
        }
        if (!node.isObject()) return;
        node.fields().forEachRemaining(e -> {
            String key = e.getKey().toLowerCase(Locale.ROOT);
            if (Set.of("pubmedid", "pmid", "pubmed_id").contains(key)) {
                for (String value : e.getValue().asText("").split("[;,\\s]+"))
                    if (value.matches("[1-9][0-9]{5,11}")) pmids.add(value);
            } else if (e.getValue().isContainerNode()) collect(e.getValue(), pmids);
        });
    }
}

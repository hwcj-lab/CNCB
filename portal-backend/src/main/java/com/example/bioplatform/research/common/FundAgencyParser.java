package com.example.bioplatform.research.common;


import com.fasterxml.jackson.databind.JsonNode;

import java.util.Locale;
import java.util.Map;

/**
 * 快照基金名称口径：优先agency，否则只接受编号在本地字典中匹配的全称。
 */
public final class FundAgencyParser {
    /**
     * 限制本工具或内部状态对象的实例化入口，具体创建方式由所属类控制。
     */
    private FundAgencyParser() {
    }

    /**
     * 只读取明确的agency，不将缩写、计划名或旧层级数组当作资助方。
     */
    public static String agency(JsonNode grant) {
        return text(grant, "agency");
    }

    /**
     * 兼容不同上游基金编号字段；BioProject原始字段名为grantID。
     */
    public static String identifier(JsonNode grant) {
        for (String key : new String[]{"grantId", "grantID", "grantNumber", "id"}) {
            String value = text(grant, key);
            if (!value.isEmpty()) return value;
        }
        return "";
    }

    /**
     * 无agency、无编号或字典无有效全称时返回空串，由统计调用方舍弃。
     */
    public static String resolve(JsonNode grant, Map<String, String> dictionary) {
        String agency = agency(grant);
        if (!agency.isEmpty()) return agency;
        String id = identifier(grant);
        return id.isEmpty() ? "" : clean(dictionary.get(id.toLowerCase(Locale.ROOT)));
    }

    /**
     * 将SQL NULL、JSON null、空白及字符串null统一当作缺失。
     */
    public static String clean(String value) {
        if (value == null || "null".equalsIgnoreCase(value.trim())) return "";
        return value.trim().replaceAll("\\s+", " ");
    }

    /**
     * Local dictionary lookup candidates; no upstream requests. Malformed JSON is rejected by preparation.
     */
    public static java.util.Set<String> missingAgencyGrantIds(String payload, com.fasterxml.jackson.databind.ObjectMapper json) {
        var result = new java.util.LinkedHashSet<String>();
        if (payload == null || payload.isBlank()) return result;
        try {
            var grants = json.readTree(payload);
            if (grants != null && grants.isArray()) for (var grant : grants)
                if (agency(grant).isEmpty() && !identifier(grant).isEmpty()) result.add(identifier(grant));
        } catch (Exception ignored) {
        }
        return result;
    }

    /**
     * 读取指定节点中的文本字段，按本类空值约定返回结果。
     * @param node 当前JSON、XML或队列节点
     * @param key 当前对象的业务匹配键
     * @return 按上述规则计算或读取的String结果
     */
    private static String text(JsonNode node, String key) {
        if (node == null || !node.isObject()) return "";
        JsonNode value = node.get(key);
        return value == null || value.isNull() || !value.isValueNode() ? "" : clean(value.asText());
    }
}

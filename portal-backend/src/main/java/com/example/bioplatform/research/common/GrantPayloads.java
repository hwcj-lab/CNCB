package com.example.bioplatform.research.common;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * 规范化采集端资助原始JSON；优先保存grants，缺失时将funding编号包装成grantId对象。
 */
public final class GrantPayloads {
    private GrantPayloads() {
    }

    /**
     * 保留grants原始对象结构；只有grants为空时才使用funding字段。
     */
    public static String normalize(JsonNode grants, JsonNode funding, ObjectMapper mapper) {
        try {
            if (hasValues(grants)) return asArray(grants, mapper).toString();
            ArrayNode fallback = mapper.createArrayNode();
            appendFunding(fallback, funding, mapper);
            return fallback.toString();
        } catch (Exception ignored) {
            return "[]";
        }
    }

    /**
     * 批量合并不同来源原始基金记录，按规范化JSON对象去重。
     */
    public static String merge(String left, String right, ObjectMapper mapper) {
        ArrayNode result = mapper.createArrayNode();
        appendJson(result, left, mapper);
        appendJson(result, right, mapper);
        return result.toString();
    }

    private static boolean hasValues(JsonNode node) {
        return node != null && !node.isNull() && !(node.isArray() && node.isEmpty())
                && !(node.isTextual() && node.asText().isBlank());
    }

    private static ArrayNode asArray(JsonNode node, ObjectMapper mapper) {
        if (node.isArray()) return (ArrayNode) node.deepCopy();
        ArrayNode array = mapper.createArrayNode();
        array.add(node.deepCopy());
        return array;
    }

    private static void appendFunding(ArrayNode target, JsonNode funding, ObjectMapper mapper) {
        if (!hasValues(funding)) return;
        if (funding.isArray()) {
            for (JsonNode value : funding) appendFundingValue(target, value, mapper);
        } else if (funding.isTextual()) {
            for (String value : funding.asText().split("[;,\\n]"))
                appendFundingValue(target, mapper.getNodeFactory().textNode(value), mapper);
        } else appendFundingValue(target, funding, mapper);
    }

    private static void appendFundingValue(ArrayNode target, JsonNode value, ObjectMapper mapper) {
        if (value == null || value.isNull()) return;
        if (value.isObject() && value.has("grantId")) {
            target.add(value.deepCopy());
            return;
        }
        String id = value.isValueNode() ? value.asText().trim() : "";
        if (id.isBlank()) return;
        ObjectNode item = mapper.createObjectNode();
        item.put("grantId", id);
        target.add(item);
    }

    private static void appendJson(ArrayNode target, String value, ObjectMapper mapper) {
        if (value == null || value.isBlank()) return;
        try {
            JsonNode node = mapper.readTree(value);
            if (node == null || node.isNull()) return;
            if (node.isArray()) node.forEach(item -> addUnique(target, item));
            else addUnique(target, node);
        } catch (Exception ignored) {
        }
    }

    private static void addUnique(ArrayNode target, JsonNode candidate) {
        String key = grantKey(candidate);
        for (JsonNode existing : target) {
            if (!grantKey(existing).equals(key)) continue;
            if (existing instanceof ObjectNode existingObject && candidate != null && candidate.isObject()) {
                candidate.fields().forEachRemaining(field -> {
                    JsonNode current = existingObject.get(field.getKey());
                    JsonNode incoming = field.getValue();
                    if ((current == null || current.isNull() || current.asText("").isBlank())
                            && incoming != null && !incoming.isNull() && !incoming.asText("").isBlank())
                        existingObject.set(field.getKey(), incoming.deepCopy());
                });
            }
            return;
        }
        target.add(candidate.deepCopy());
    }

    /**
     * 同一编号优先按grantId去重；没有编号时保留完整对象并按JSON内容去重。
     */
    private static String grantKey(JsonNode value) {
        if (value != null && value.isObject()) {
            for (String field : new String[]{"grantId", "grantID", "grantNumber", "id"}) {
                JsonNode id = value.get(field);
                if (id != null && !id.isNull() && !id.asText().isBlank())
                    return (field.equals("id") ? "id:" : "grant:") + id.asText().trim().toLowerCase(java.util.Locale.ROOT);
            }
        }
        return value == null ? "null" : value.toString();
    }
}

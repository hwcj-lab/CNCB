package com.example.bioplatform.research.common;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * 规范化采集端资助原始JSON；优先保存grants，缺失时将funding编号包装成grantId对象。
 */
public final class GrantPayloads {
    /**
     * 限制本工具或内部状态对象的实例化入口，具体创建方式由所属类控制。
     */
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

    /**
     * 判断资助载荷是否真正包含可用值，空容器不视为已有基金。
     * @param node 当前JSON、XML或队列节点
     * @return true表示满足上述条件，false表示不满足
     */
    private static boolean hasValues(JsonNode node) {
        return node != null && !node.isNull() && !(node.isArray() && node.isEmpty())
                && !(node.isTextual() && node.asText().isBlank());
    }

    /**
     * 把单个对象或数组转换为统一数组形态，方便后续无损合并。
     * @param node 当前JSON、XML或队列节点
     * @param mapper 本组件使用的持久层接口，SQL由对应XML维护
     * @return 按上述规则计算或读取的ArrayNode结果
     */
    private static ArrayNode asArray(JsonNode node, ObjectMapper mapper) {
        if (node.isArray()) return (ArrayNode) node.deepCopy();
        ArrayNode array = mapper.createArrayNode();
        array.add(node.deepCopy());
        return array;
    }

    /**
     * 把funding载荷中的编号或对象转换为统一资助条目并追加。
     * @param target 接收合并或解析结果的目标对象
     * @param funding 上游funding载荷，可能只包含资助编号
     * @param mapper 本组件使用的持久层接口，SQL由对应XML维护
     */
    private static void appendFunding(ArrayNode target, JsonNode funding, ObjectMapper mapper) {
        if (!hasValues(funding)) return;
        if (funding.isArray()) {
            for (JsonNode value : funding) appendFundingValue(target, value, mapper);
        } else if (funding.isTextual()) {
            for (String value : funding.asText().split("[;,\\n]"))
                appendFundingValue(target, mapper.getNodeFactory().textNode(value), mapper);
        } else appendFundingValue(target, funding, mapper);
    }

    /**
     * 把单个funding编号包装为grantId对象；已有对象按原结构保留有效字段。
     * @param target 接收合并或解析结果的目标对象
     * @param value 待校验、转换或合并的值
     * @param mapper 本组件使用的持久层接口，SQL由对应XML维护
     */
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

    /**
     * 从JSON载荷追加可用资助记录，兼容不同上游字段外形。
     * @param target 接收合并或解析结果的目标对象
     * @param value 待校验、转换或合并的值
     * @param mapper 本组件使用的持久层接口，SQL由对应XML维护
     */
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

    /**
     * 按业务摘要向目标数组追加尚未存在的资助条目。
     * @param target 接收合并或解析结果的目标对象
     * @param candidate 准备追加或合并的一条候选资助记录
     */
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

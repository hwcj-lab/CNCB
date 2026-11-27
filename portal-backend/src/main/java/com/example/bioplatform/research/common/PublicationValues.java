package com.example.bioplatform.research.common;

import com.fasterxml.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;

/**
 * 共享字段读取、DOI规范化和来源摘要；不访问数据库或网络。
 */
public final class PublicationValues {
    /**
     * 限制本工具或内部状态对象的实例化入口，具体创建方式由所属类控制。
     */
    private PublicationValues() {
    }

    /**
     * 读取指定节点中的文本字段，按本类空值约定返回结果。
     * @param row 本次处理的一条记录
     * @param field 要读取或匹配的字段名
     * @return 按上述规则计算或读取的String结果
     */
    public static String text(JsonNode row, String field) {
        JsonNode n = row.get(field);
        String v = n == null || n.isNull() ? "" : n.asText("").trim();
        return "null".equalsIgnoreCase(v) ? "" : v;
    }

    /**
     * 清理DOI前缀及大小写，形成用于匹配的规范化值；不是联网解析DOI。
     */
    public static String normalizeDoi(String doi) {
        return doi.trim().replaceFirst("(?i)^https?://(?:dx\\.)?doi\\.org/+", "").toLowerCase(Locale.ROOT);
    }

    /**
     * 为业务字符串生成稳定摘要，便于索引和比较；摘要不能替代原始业务含义。
     */
    public static String sha(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}

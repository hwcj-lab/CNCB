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
    private PublicationValues() {
    }

    public static String text(JsonNode row, String field) {
        JsonNode n = row.get(field);
        String v = n == null || n.isNull() ? "" : n.asText("").trim();
        return "null".equalsIgnoreCase(v) ? "" : v;
    }

    public static String normalizeDoi(String doi) {
        return doi.trim().replaceFirst("(?i)^https?://(?:dx\\.)?doi\\.org/+", "").toLowerCase(Locale.ROOT);
    }

    public static String sha(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}

package com.example.bioplatform.research.collection.gsa.service.impl;

import com.example.bioplatform.research.collection.gsa.dto.GsaPublicationSeed;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GSA支持文章行解析：保留页面全部支持文章，包括HRA/OMIX，不按CRA过滤。
 */
public final class GsaPublicationParser {
    private GsaPublicationParser() {
    }

    private static final Pattern RESOURCE = Pattern.compile("(?i)\\b(?:CRA|HRA|OMIX)[0-9]+\\b");
    private static final Pattern PROJECT = Pattern.compile("(?i)\\bPRJ(?:CA|NA|EB|DA)[A-Z0-9]+\\b");

    public static GsaPublicationSeed parse(JsonNode row, ObjectMapper json) {
        GsaPublicationSeed seed = new GsaPublicationSeed();
        String pmid = text(row, "pubmedId");
        if (!pmid.matches("[1-9][0-9]{5,11}")) pmid = null;
        String doi = normalizeDoi(text(row, "doi"));
        if (doi.isEmpty()) doi = null;
        String title = plain(text(row, "articleTitle"));
        if (pmid == null && doi == null && title.isBlank())
            throw new IllegalArgumentException("支持文章缺少PMID、DOI和标题，不能可靠去重");
        seed.setRecordKey(pmid != null ? "PMID:" + pmid : doi != null ? "DOI:" + doi : "TITLE:" + sha(title.toLowerCase(Locale.ROOT)));
        seed.setPmid(pmid);
        seed.setDoi(doi);
        seed.setTitle(title);
        seed.setJournalTitle(plain(text(row, "journal")));
        Matcher year = Pattern.compile("^(\\d{4})").matcher(text(row, "pDate"));
        if (year.find()) {
            int y = Integer.parseInt(year.group(1));
            if (y >= 1500 && y <= 2200) seed.setPubYear(y);
        }
        List<String> resources = matches(RESOURCE, text(row, "craAccession"));
        List<String> projects = matches(PROJECT, text(row, "proAccession"));
        seed.setAccessions(String.join(",", resources));
        seed.setBioprojectAccession(String.join(",", projects));
        var banks = json.createArrayNode();
        for (String value : resources) {
            if (value.startsWith("CRA"))
                banks.addObject().put("db", "GSA").put("accession", value).put("link", "https://ngdc.cncb.ac.cn/gsa/search?searchTerm=" + value);
        }
        for (String value : projects)
            banks.addObject().put("db", "BioProject").put("accession", value).put("link", "https://ngdc.cncb.ac.cn/bioproject/browse/" + value);
        seed.setCncbBanksJson(banks.toString());
        seed.setSourceUrl(pmid != null ? "https://pubmed.ncbi.nlm.nih.gov/" + pmid + "/" : doi != null ? "https://doi.org/" + doi : resources.stream().filter(value -> value.startsWith("CRA")).findFirst().map(value -> "https://ngdc.cncb.ac.cn/gsa/browse/" + value).orElse(projects.isEmpty() ? "https://ngdc.cncb.ac.cn/gsa/statistics" : "https://ngdc.cncb.ac.cn/bioproject/browse/" + projects.get(0)));
        seed.setRawPayload(row.toString());
        seed.setSourceHash(sha(row.toString()));
        return seed;
    }

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

    private static String plain(String value) {
        return value.replaceAll("<[^>]+>", " ").replace("&amp;", "&").replace("&nbsp;", " ").replaceAll("\\s+", " ").trim();
    }

    private static List<String> matches(Pattern p, String value) {
        Set<String> result = new LinkedHashSet<>();
        Matcher m = p.matcher(plain(value));
        while (m.find()) result.add(m.group().toUpperCase(Locale.ROOT));
        return List.copyOf(result);
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

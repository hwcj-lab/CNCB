package com.example.bioplatform.research.collection.common.service.impl;


import java.util.*;
import java.util.regex.*;

/**
 * 从官方详情正文的明确文献链接提取PMID，不把资源编号、年份或基金编号当PMID。
 */
public final class ResourcePublicationParser {
    private ResourcePublicationParser() {
    }

    private static final Pattern PMID = Pattern.compile("(?i)(?:pubmed\\.ncbi\\.nlm\\.nih\\.gov/|ncbi\\.nlm\\.nih\\.gov/pubmed/|openlb/publication/OLB-PM-)([1-9][0-9]{5,11})(?![0-9])");
    private static final Pattern PROJECT = Pattern.compile("(?i)/bioproject/browse/(PRJ(?:CA|NA|EB|DA)[A-Z0-9]+)");

    /**
     * 剔除页脚，避免把全站通用的数据库引用论文计入每个资源。
     */
    public static String content(String html) {
        String lower = html.toLowerCase(Locale.ROOT);
        int end = lower.indexOf("<footer");
        if (end < 0) end = lower.indexOf("id=\"footer\"");
        return end < 0 ? html : html.substring(0, end);
    }

    public static List<String> pmids(String html) {
        return matches(PMID, content(html));
    }

    public static List<String> projects(String html) {
        return matches(PROJECT, content(html));
    }

    private static List<String> matches(Pattern pattern, String value) {
        Set<String> values = new LinkedHashSet<>();
        Matcher m = pattern.matcher(value);
        while (m.find()) values.add(m.group(1));
        return List.copyOf(values);
    }

    /**
     * 从资源官方链接回退识别稳定登录号；accession有效时由调用者优先使用。
     */
    public static String accession(String value, String url, String expression) {
        Pattern p = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
        if (value != null && p.matcher(value.trim()).matches()) return value.trim();
        Matcher m = p.matcher(url == null ? "" : url);
        if (m.find()) return m.group();
        throw new IllegalArgumentException("无法从资源编号或链接提取登录号");
    }
}

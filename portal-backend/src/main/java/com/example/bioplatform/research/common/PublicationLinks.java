package com.example.bioplatform.research.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 论文外链优先PMID/DOI，无标识时定位本篇记录明确关联的GSA或BioProject资源。
 */
public final class PublicationLinks {
    /**
     * GSA：GSA编号匹配规则，或对应来源类型枚举值。
     * PROJECT：项目编号匹配规则，或对应资源类型枚举值。
     */
    private static final Pattern GSA = Pattern.compile("(?i)\\bCRA[0-9]+\\b"), PROJECT = Pattern.compile("(?i)\\bPRJ(?:CA|NA|EB|DA)[A-Z0-9]+\\b");

    /**
     * 限制本工具或内部状态对象的实例化入口，具体创建方式由所属类控制。
     */
    private PublicationLinks() {
    }

    /**
     * 按已有PMID、DOI及明确资源编号选择外部跳转地址；没有可靠地址时不拼造论文链接。
     * @param pmid 已有PubMed标识；为空时不通过标题猜补
     * @param doi DOI标识；匹配前按调用链统一规范化
     * @param gsa GSA来源的持久化或元数据访问组件
     * @param projects BioProject元数据访问组件或项目结果集合
     * @param banks 原始或合并后的cncbBanks资源条目
     * @param json JSON解析与序列化组件
     * @return 按上述规则计算或读取的String结果
     */
    public static String external(String pmid, String doi, String gsa, String projects, String banks, ObjectMapper json) {
        if (pmid != null && pmid.trim().matches("[0-9]+"))
            return "https://pubmed.ncbi.nlm.nih.gov/" + pmid.trim() + "/";
        if (doi != null) {
            String clean = doi.trim().replaceFirst("(?i)^https?://(?:dx\\.)?doi\\.org/", "");
            if (clean.matches("(?i)10\\.[0-9]{4,9}/\\S+")) return "https://doi.org/" + clean;
        }
        String resource = resource(gsa, projects);
        if (resource != null) return resource;
        if (banks != null && !banks.isBlank()) try {
            JsonNode array = json.readTree(banks);
            if (array.isArray()) for (JsonNode bank : array) {
                String url = resource(bank.path("accession").asText(""), bank.path("accession").asText(""));
                if (url == null) url = resource(bank.path("link").asText(""), bank.path("link").asText(""));
                if (url != null) return url;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * 根据明确的GSA或BioProject编号构建官方资源详情地址。
     * @param gsa GSA来源的持久化或元数据访问组件
     * @param projects BioProject元数据访问组件或项目结果集合
     * @return 按上述规则计算或读取的String结果
     */
    private static String resource(String gsa, String projects) {
        Matcher m = GSA.matcher(gsa == null ? "" : gsa);
        if (m.find()) return "https://ngdc.cncb.ac.cn/gsa/browse/" + m.group().toUpperCase(Locale.ROOT);
        m = PROJECT.matcher(projects == null ? "" : projects);
        return m.find() ? "https://ngdc.cncb.ac.cn/bioproject/browse/" + m.group().toUpperCase(Locale.ROOT) : null;
    }
}

package com.example.bioplatform.research.collection.bioproject.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 已验证公开JSON接口优先；缺少PMID时读项目出版信息表补充，绝不遍历其他资源的任意链接。
 */
public final class BioProjectDocumentParser {
    /**
     * 项目正文读取回调，便于替换网络实现或在测试中提供固定响应。
     */
    @FunctionalInterface
    public interface Fetcher {
        /**
         * 读取指定地址的上游正文；网络、状态码及解析相关异常由调用方处理。
         * @param url 当前请求或资源的访问地址
         * @return 按上述规则计算或读取的String结果
         */
        String get(String url) throws Exception;
    }

    /**
     * 优先读取项目公开接口，必要时解析对应项目详情页；不遍历任意外部资源。
     * @param code 当前来源、策略或业务响应使用的编码
     * @param json JSON解析与序列化组件
     * @param fetch 读取资源正文或项目详情的回调
     * @return 按上述规则计算或读取的JsonNode结果
     */
    public static JsonNode fetch(String code, ObjectMapper json, Fetcher fetch) throws Exception {
        if (!code.matches("PRJ(?:CA|NA|EB|DA)[A-Z0-9]+")) throw new IllegalArgumentException("无效项目编号");
        ObjectNode project = null;
        Exception apiError = null;
        try {
            JsonNode result = json.readTree(fetch.get("https://ngdc.cncb.ac.cn/gwh/api/public/bioProject/" + code));
            if ("SUCCESS".equalsIgnoreCase(result.path("message").asText()) && result.path("listPublication").isArray())
                project = (ObjectNode) result;
            else throw new IllegalStateException("BioProject公开接口结构异常");
        } catch (Exception e) {
            if (e instanceof InterruptedException) throw e;
            apiError = e;
        }
        if (project != null) {
            ArrayNode actual = json.createArrayNode();
            for (JsonNode paper : project.path("listPublication"))
                if (!com.example.bioplatform.research.common.PublicationValues.text(paper, "articleTitle").isBlank() || !com.example.bioplatform.research.common.PublicationValues.text(paper, "doi").isBlank() || com.example.bioplatform.research.common.PublicationValues.text(paper, "pubmedId").matches("[1-9][0-9]{5,11}"))
                    actual.add(paper);
            project.set("listPublication", actual);
        }
        // 有效原库API空数组就是无原生文献；缺少PMID不会触发额外检索或补写。
        boolean needHtml = project == null;
        if (!needHtml) return project;
        ObjectNode page;
        try {
            page = html(fetch.get("https://ngdc.cncb.ac.cn/bioproject/browse/" + code), json);
        } catch (Exception e) {
            if (apiError != null) e.addSuppressed(apiError);
            throw e;
        }
        if (project == null) return page;
        ArrayNode papers = (ArrayNode) project.path("listPublication");
        for (JsonNode paper : page.path("listPublication")) {
            ObjectNode match = null;
            for (JsonNode old : papers) {
                String p = com.example.bioplatform.research.common.PublicationValues.text(paper, "pubmedId"), d = com.example.bioplatform.research.common.PublicationValues.normalizeDoi(com.example.bioplatform.research.common.PublicationValues.text(paper, "doi"));
                if (!p.isBlank() && p.equals(com.example.bioplatform.research.common.PublicationValues.text(old, "pubmedId")) || !d.isBlank() && d.equals(com.example.bioplatform.research.common.PublicationValues.normalizeDoi(com.example.bioplatform.research.common.PublicationValues.text(old, "doi"))) || !com.example.bioplatform.research.common.PublicationValues.text(paper, "articleTitle").isBlank() && com.example.bioplatform.research.common.PublicationValues.text(paper, "articleTitle").equalsIgnoreCase(com.example.bioplatform.research.common.PublicationValues.text(old, "articleTitle"))) {
                    match = (ObjectNode) old;
                    break;
                }
            }
            if (match == null) papers.add(paper);
            else {
                var fields = paper.fields();
                while (fields.hasNext()) {
                    var entry = fields.next();
                    if (com.example.bioplatform.research.common.PublicationValues.text(match, entry.getKey()).isBlank())
                        match.set(entry.getKey(), entry.getValue());
                }
            }
        }
        if (!project.path("listGrants").isArray() || project.path("listGrants").isEmpty())
            project.set("listGrants", page.path("listGrants"));
        if (project.path("submitter").path("organization").asText("").isBlank())
            project.set("submitter", page.path("submitter"));
        return project;
    }

    /**
     * 从官方项目详情HTML提取本项目的出版、机构和资助信息。
     * @param html 官方资源页面HTML正文
     * @param json JSON解析与序列化组件
     * @return 按上述规则计算或读取的ObjectNode结果
     */
    public static ObjectNode html(String html, ObjectMapper json) {
        ObjectNode root = json.createObjectNode();
        ArrayNode papers = root.putArray("listPublication"), grants = root.putArray("listGrants");
        String publicationTable = table(html, "PubMed ID");
        if (publicationTable == null) {
            if (html.contains("出版信息") && html.matches("(?is).*(?:出版信息|Publication).*?</th>\\s*<td[^>]*>\\s*(?:-|None|N/A|无|暂无)\\s*</td>.*"))
                publicationTable = "";
            else throw new IllegalStateException("BioProject页面缺少可识别的出版信息，可能安全验证或结构变化");
        }
        for (List<String> cells : rows(publicationTable))
            if (cells.size() >= 5) {
                String pmid = plain(cells.get(0)), title = plain(cells.get(1)), doi = plain(cells.get(3));
                if (!pmid.matches("[1-9][0-9]{5,11}")) pmid = "";
                Matcher doiLink = Pattern.compile("(?i)https?://(?:dx\\.)?doi\\.org/([^\\\"'<>\\s]+)").matcher(cells.get(3));
                if (doiLink.find()) doi = doiLink.group(1);
                if (pmid.isBlank() && title.isBlank() && doi.isBlank()) continue;
                papers.addObject().put("pubmedId", pmid).put("articleTitle", title).put("journalTitle", plain(cells.get(2))).put("doi", doi).put("year", plain(cells.get(4)));
            }
        String fundTable = table(html, "授权项目ID");
        if (fundTable == null) fundTable = table(html, "Grant ID");
        if (fundTable != null) for (List<String> cells : rows(fundTable))
            if (cells.size() >= 4) {
                String agency = plain(cells.get(0)), id = plain(cells.get(2));
                if (!agency.isBlank() || !id.isBlank())
                    grants.addObject().put("agency", agency).put("program", plain(cells.get(1))).put("grantId", id).put("grantTitle", plain(cells.get(3)));
            }
        Matcher organization = Pattern.compile("(?is)<th[^>]*>\\s*(?:提交单位|Organization)\\s*</th>\\s*<td[^>]*>(.*?)</td>").matcher(html);
        root.putObject("submitter").put("organization", organization.find() ? plain(organization.group(1)) : "");
        return root;
    }

    /**
     * 提取页面中的目标表格或将受支持维度映射成白名单表名，按当前类职责处理。
     * @param html 官方资源页面HTML正文
     * @param marker 用于定位正文区域的标记
     * @return 按上述规则计算或读取的String结果
     */
    private static String table(String html, String marker) {
        int at = html.indexOf(marker);
        if (at < 0) return null;
        int start = html.lastIndexOf("<table", at), end = html.indexOf("</table>", at);
        return start < 0 || end < 0 ? null : html.substring(start, end + 8);
    }

    /**
     * 把目标HTML表格分解为可逐行解析的记录。
     * @param html 官方资源页面HTML正文
     * @return 按上述规则计算或读取的List<List<String>>结果
     */
    private static List<List<String>> rows(String html) {
        List<List<String>> result = new ArrayList<>();
        Matcher rows = Pattern.compile("(?is)<tr\\b[^>]*>(.*?)</tr>").matcher(html);
        while (rows.find()) {
            List<String> cells = new ArrayList<>();
            Matcher cell = Pattern.compile("(?is)<td\\b[^>]*>(.*?)</td>").matcher(rows.group(1));
            while (cell.find()) cells.add(cell.group(1));
            if (!cells.isEmpty()) result.add(cells);
        }
        return result;
    }

    /**
     * 去除HTML标记并整理空白，得到可保存或比较的正文文本。
     * @param value 待校验、转换或合并的值
     * @return 按上述规则计算或读取的String结果
     */
    private static String plain(String value) {
        return value.replaceAll("(?is)<[^>]+>", " ").replace("&amp;", "&").replace("&nbsp;", " ").replace("&#39;", "'").replace("&quot;", "\"").replaceAll("\\s+", " ").trim();
    }
}

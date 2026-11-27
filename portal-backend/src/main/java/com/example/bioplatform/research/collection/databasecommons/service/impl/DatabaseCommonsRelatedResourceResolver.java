package com.example.bioplatform.research.collection.databasecommons.service.impl;

import com.example.bioplatform.research.collection.common.service.RelatedResourceData;
import com.example.bioplatform.research.collection.common.service.RelatedResourceResolver;
import com.example.bioplatform.research.collection.common.service.relation.RelationPageClient;
import com.example.bioplatform.research.collection.common.service.relation.RelationRunCache;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.net.URI;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Resolve only the requested dbId. Other profiles warm the bounded run-local index.
 */
@Component
@RequiredArgsConstructor
public class DatabaseCommonsRelatedResourceResolver implements RelatedResourceResolver {
    private static final Pattern PROFILE_PATH = Pattern.compile("^/databasecommons/database/id/(\\d+)/?$");
    private final ObjectMapper json;
    private final RelationPageClient pages;

    @Override
    public List<String> supportedDatabaseNames() {
        return List.of("Database Commons", "DatabaseCommons");
    }

    @Override
    public RelatedResourceData findRelatedPublications(String accession, String officialUrl) throws Exception {
        String id = databaseId(accession, officialUrl);
        return RelationRunCache.resource("databasecommons-profile:" + id, () -> loadProfile(id));
    }

    private RelatedResourceData loadProfile(String id) throws Exception {
        String url = "https://ngdc.cncb.ac.cn/databasecommons/api/biodb/cited?dbId=" + id;
        JsonNode response = json.readTree(pages.get(url));
        JsonNode profiles = response;
        if (response != null && response.isObject() && !response.has("publicationList")) {
            String message = response.path("message").asText("");
            if (!message.isBlank() && !List.of("SUCCESS", "OK", "成功").contains(message.toUpperCase(java.util.Locale.ROOT)))
                throw new IllegalStateException("Database Commons接口返回失败: " + message);
            profiles = response.path("data");
        }
        if (profiles == null || (!profiles.isArray() && !(profiles.isObject() && profiles.has("publicationList"))))
            throw new IllegalStateException("Database Commons返回结构异常，应为数据库列表，dbId=" + id);
        RelatedResourceData target = null;
        Iterable<JsonNode> entries = profiles.isArray() ? profiles : List.of(profiles);
        for (JsonNode profile : entries) {
            String profileId = normalizeId(profile.path("dbId").asText(profile.path("id").asText("")));
            if (profileId == null) continue;
            JsonNode publications = profile.path("publicationList");
            if (!publications.isArray()) {
                if (!id.equals(profileId)) continue;
                throw new IllegalStateException("Database Commons返回条目缺少publicationList数组，dbId=" + id);
            }
            var pmids = new LinkedHashSet<String>();
            var grants = json.createArrayNode();
            for (JsonNode publication : publications) {
                if (!publication.isObject())
                    throw new IllegalStateException("Database Commons出版记录结构异常，dbId=" + id);
                for (String field : List.of("pmid", "pubmedId")) {
                    JsonNode value = publication.get(field);
                    if (value == null || !value.isValueNode()) continue;
                    String pmid = value.asText("").trim();
                    if (pmid.matches("[1-9][0-9]{5,11}")) pmids.add(pmid);
                }
            }
            JsonNode funding = profile.path("funding");
            if (funding.isArray()) funding.forEach(value -> grants.add(value.deepCopy()));
            else if (!funding.isMissingNode() && !funding.isNull()) grants.add(funding.deepCopy());
            var data = new RelatedResourceData(List.copyOf(pmids), grants);
            if (id.equals(profileId)) target = data;
            else RelationRunCache.remember("databasecommons-profile:" + profileId, data);
        }
        if (target != null) return target;
        // Cited is a neighbourhood, not a full catalogue; never substitute other IDs for a missing target.
        String html = pages.get("https://ngdc.cncb.ac.cn/databasecommons/database/id/" + id);
        var table = Pattern.compile("(?is)<table\\b[^>]*\\bid\\s*=\\s*['\"]pubTable['\"][^>]*>(.*?)</table>").matcher(html);
        if (!table.find()) throw new IllegalStateException("Database Commons详情缺少Publications表，dbId=" + id);
        var pmids = new LinkedHashSet<String>();
        var links = Pattern.compile("(?i)(?:https?:)?//(?:www\\.)?ncbi\\.nlm\\.nih\\.gov/pubmed/([1-9][0-9]{5,11})|(?:https?:)?//pubmed\\.ncbi\\.nlm\\.nih\\.gov/([1-9][0-9]{5,11})").matcher(table.group(1));
        while (links.find()) pmids.add(links.group(1) == null ? links.group(2) : links.group(1));
        return new RelatedResourceData(List.copyOf(pmids), json.createArrayNode());
    }

    private String databaseId(String accession, String officialUrl) {
        String id = normalizeId(accession == null ? null : accession.trim().replaceFirst("(?i)^DBC", ""));
        String linkId = null;
        if (officialUrl != null && !officialUrl.isBlank()) {
            URI uri = URI.create(officialUrl);
            var match = PROFILE_PATH.matcher(uri.getPath());
            if ("ngdc.cncb.ac.cn".equalsIgnoreCase(uri.getHost()) && match.matches()) {
                linkId = normalizeId(match.group(1));
            }
        }
        if (id != null && linkId != null && !id.equals(linkId)) {
            throw new IllegalArgumentException("Database Commons accession与链接的数据库编号不一致");
        }
        if (id == null) id = linkId;
        if (id == null) throw new IllegalArgumentException("Database Commons编号格式无效: " + accession);
        return id;
    }

    private String normalizeId(String value) {
        if (value == null || !value.matches("[0-9]+")) return null;
        BigInteger id = new BigInteger(value);
        return id.signum() > 0 ? id.toString() : null;
    }

}

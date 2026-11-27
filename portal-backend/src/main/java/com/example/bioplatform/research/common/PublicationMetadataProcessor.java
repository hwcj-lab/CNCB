package com.example.bioplatform.research.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

/**
 * 纯元数据处理：固定结构输入/输出，无数据库和网络访问，不持有跨文献状态。
 */
public final class PublicationMetadataProcessor {
    private final ObjectMapper objectMapper;

    public PublicationMetadataProcessor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public record Input(String authorsJson, String affiliationInfo, String grantsJson, String keywordList,
                        String meshHeadingsJson, String cncbBanksJson) {
    }

    public record AuthorIdentity(String name, String institution) {
    }

    public record Output(Set<AuthorIdentity> authors, Set<String> institutions, Set<String> funds, Set<String> topics) {
    }

    /**
     * 从统一输入独立提取作者机构、资助方、主题及来源信息；纯本地转换，不访问网络或数据库。
     */
    public Output parse(Input input, Map<String, String> dictionary) {
        return new Output(ordered(authorWithInstitutions(input)), ordered(values(input, "INSTITUTION", dictionary)), ordered(values(input, "FUND", dictionary)), ordered(publicationTopics(input)));
    }

    /**
     * 采集阶段只合并原始JSON，不运行机构归一化或基金名称解析。
     */
    public Input merge(Input existing, Input incoming) {
        return new Input(ResearchPublicationJsonMerger.mergeAuthors(existing.authorsJson, incoming.authorsJson, objectMapper),
                present(incoming.affiliationInfo) ? incoming.affiliationInfo : existing.affiliationInfo,
                GrantPayloads.merge(existing.grantsJson, incoming.grantsJson, objectMapper),
                present(incoming.keywordList) ? incoming.keywordList : existing.keywordList,
                present(incoming.meshHeadingsJson) ? incoming.meshHeadingsJson : existing.meshHeadingsJson,
                ResearchPublicationJsonMerger.mergeBanks(existing.cncbBanksJson, incoming.cncbBanksJson, objectMapper));
    }

    public String normalizeGrants(JsonNode grants, JsonNode funding) {
        return GrantPayloads.normalize(grants, funding, objectMapper);
    }

    private static boolean present(String value) {
        return value != null && !value.isBlank();
    }

    private static <T> Set<T> ordered(Set<T> values) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(values));
    }

    private Set<String> values(Input p, String type, Map<String, String> dictionary) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if ("INSTITUTION".equals(type)) {
            collectJsonValues(p.authorsJson, Set.of("institutionname"), result);
            if (result.isEmpty()) {
                collectJsonValues(p.authorsJson, Set.of("affiliation", "institution", "organization", "affiliationname"), result);
                collectJsonValues(p.affiliationInfo, Set.of("affiliation", "institution", "organization", "name"), result);
                if (result.isEmpty() && notBlank(p.affiliationInfo) && !looksJson(p.affiliationInfo))
                    splitAffiliations(p.affiliationInfo, result);
                Set<String> normalized = normalizeInstitutions(result);
                result.clear();
                result.addAll(normalized);
            }
        } else if ("FUND".equals(type)) {
            collectGrantValues(p.grantsJson, dictionary, result);
        }
        result.removeIf(v -> v.length() < 2 || "null".equalsIgnoreCase(v));
        return result;
    }

    /**
     * 把单个作者对应的机构拆成作者机构身份；机构归属保持在作者层级，不按整篇机构列表交叉组合。
     */
    private Set<AuthorIdentity> authorWithInstitutions(Input publication) {
        LinkedHashSet<AuthorIdentity> result = new LinkedHashSet<>();
        if (!notBlank(publication.authorsJson)) return result;
        try {
            JsonNode root = objectMapper.readTree(publication.authorsJson);
            List<JsonNode> authors = new ArrayList<>();
            collectAuthorNodes(root, authors);
            List<AuthorIdentity> parsed = new ArrayList<>();
            Set<String> knownAffiliations = new LinkedHashSet<>();
            for (JsonNode author : authors) {
                String name = authorName(author);
                if (!notBlank(name)) continue;
                LinkedHashSet<String> rawAffiliations = new LinkedHashSet<>();
                String preNormalized = firstText(author, "institutionName");
                Set<String> affiliations;
                if (notBlank(preNormalized)) affiliations = Set.of(preNormalized.trim());
                else {
                    collectNode(author, Set.of("affiliation", "institution", "organization", "affiliationname"), rawAffiliations);
                    affiliations = normalizeInstitutions(rawAffiliations);
                }
                knownAffiliations.addAll(affiliations);
                parsed.add(new AuthorIdentity(name, affiliations.isEmpty() ? null : String.join("; ", affiliations)));
            }
            String commonAffiliation = knownAffiliations.size() == 1
                    ? knownAffiliations.iterator().next() : null;
            for (AuthorIdentity author : parsed) {
                // 文章级机构汇总无法可靠对应到个人；仅在其他作者机构唯一时继承。
                result.add(new AuthorIdentity(author.name,
                        notBlank(author.institution) ? author.institution : commonAffiliation));
            }
        } catch (Exception ignored) {
        }

        // 少数来源只有姓名而没有标准作者数组，仍保留作者但明确机构未知。
        if (result.isEmpty()) {
            LinkedHashSet<String> names = new LinkedHashSet<>();
            collectJsonValues(publication.authorsJson, Set.of("fullname", "collectivename"), names);
            collectComposedNames(publication.authorsJson, names);
            names.forEach(name -> result.add(new AuthorIdentity(name, null)));
        }
        return result;
    }

    private void collectAuthorNodes(JsonNode node, List<JsonNode> out) {
        if (node == null) return;
        if (node.isArray()) {
            node.forEach(child -> collectAuthorNodes(child, out));
            return;
        }
        if (!node.isObject()) return;
        if (notBlank(authorName(node))) {
            out.add(node);
            return;
        }
        node.fields().forEachRemaining(entry -> collectAuthorNodes(entry.getValue(), out));
    }

    private String authorName(JsonNode node) {
        String direct = firstText(node, "fullName", "collectiveName");
        if (notBlank(direct)) return direct.trim().replaceAll("\\s+", " ");
        String first = firstText(node, "firstName", "foreName", "givenName");
        String last = firstText(node, "lastName", "familyName");
        String composed = (first + " " + last).trim().replaceAll("\\s+", " ");
        return composed;
    }

    private void collectJsonValues(String json, Set<String> keys, Set<String> out) {
        if (!notBlank(json)) return;
        try {
            collectNode(objectMapper.readTree(json), keys, out);
        } catch (Exception ignored) {
        }
    }

    private void collectGrantValues(String json, Map<String, String> dictionary, Set<String> out) {
        if (!notBlank(json)) return;
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isArray()) return;
            for (JsonNode grant : root) {
                String name = FundAgencyParser.resolve(grant, dictionary);
                if (!name.isEmpty()) addText(out, name);
            }
        } catch (Exception ignored) {
        }
    }

    private void collectNode(JsonNode node, Set<String> keys, Set<String> out) {
        if (node == null) return;
        if (node.isArray()) node.forEach(child -> collectNode(child, keys, out));
        else if (node.isObject()) node.fields().forEachRemaining(e -> {
            if (keys.contains(e.getKey().toLowerCase(Locale.ROOT)) && e.getValue().isValueNode())
                addText(out, e.getValue().asText());
            else collectNode(e.getValue(), keys, out);
        });
    }

    private void collectComposedNames(String json, Set<String> out) {
        if (!notBlank(json)) return;
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isArray()) return;
            root.forEach(n -> {
                String first = firstText(n, "firstName", "foreName", "givenName");
                String last = firstText(n, "lastName", "familyName");
                if (notBlank(first) || notBlank(last)) addText(out, (first + " " + last).trim());
            });
        } catch (Exception ignored) {
        }
    }

    private String firstText(JsonNode node, String... names) {
        for (String name : names) if (node.hasNonNull(name)) return node.get(name).asText("");
        return "";
    }

    private void addText(Set<String> out, String value) {
        if (value == null) return;
        String clean = value.trim().replaceAll("\\s+", " ");
        if (!clean.isEmpty() && !"null".equalsIgnoreCase(clean)) out.add(clean);
    }

    private void splitAffiliations(String value, Set<String> out) {
        Arrays.stream(value.split("[;\\n]")).forEach(v -> addText(out, v));
    }

    private Set<String> normalizeInstitutions(Collection<String> affiliations) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (affiliations == null) return result;
        for (String affiliation : affiliations) {
            if (!notBlank(affiliation)) continue;
            String clean = affiliation.replaceAll("(?i)\\b[\\w.+-]+@[\\w.-]+\\.[A-Z]{2,}\\b", "")
                    .replaceAll("https?://\\S+", "").trim();
            for (String segment : clean.split("[;\\n]")) {
                String selected = null;
                String[] tokens = segment.split(",");
                // 常见署名格式为“科室/学院, 机构, 城市, 国家”，第二段若像机构名则优先采用。
                if (tokens.length > 1) {
                    String second = tokens[1].trim().replaceAll("^[\\d\\s.]+", "").replaceAll("[.\\s]+$", "");
                    boolean secondIsGenericUnit = second.matches("(?i)^(department|dept\\.?|division|faculty|school)\\b.*");
                    boolean secondIsOrganization = second.matches("(?i).*(university|universit[eé]|institute|institut|hospital|medical center|medical centre|\\bcenter\\b|\\bcentre\\b|academy|laboratory|laboratories|\\blab\\b|college|clinic|health system).*");
                    if (secondIsOrganization && !secondIsGenericUnit) selected = second;
                }
                for (String token : selected == null ? tokens : new String[0]) {
                    String candidate = token.trim().replaceAll("^[\\d\\s.]+", "").replaceAll("[.\\s]+$", "");
                    if (candidate.isEmpty()) continue;
                    boolean genericUnit = candidate.matches("(?i)^(department|dept\\.?|division|faculty|school)\\b.*");
                    boolean organization = candidate.matches("(?i).*(university|universit[eé]|institute|institut|hospital|medical center|medical centre|\\bcenter\\b|\\bcentre\\b|academy|laboratory|laboratories|\\blab\\b|college|clinic|health system).*");
                    if (organization && !genericUnit) selected = candidate;
                }
                result.add(notBlank(selected) ? selected : segment.replaceAll("\\s+", " ").trim());
            }
        }
        result.removeIf(String::isBlank);
        return result;
    }

    private boolean looksJson(String value) {
        String v = value.trim();
        return v.startsWith("[") || v.startsWith("{");
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private Set<String> publicationTopics(Input publication) {
        LinkedHashSet<String> topics = new LinkedHashSet<>();
        collectTopicField(publication.keywordList, topics);
        collectTopicField(publication.meshHeadingsJson, topics);
        topics.removeIf(value -> value.length() < 2 || value.length() > 200 || "null".equalsIgnoreCase(value));
        return topics;
    }

    private void collectTopicField(String raw, Set<String> out) {
        if (!notBlank(raw)) return;
        try {
            JsonNode node = objectMapper.readTree(raw);
            if (node.isArray()) {
                for (JsonNode item : node) {
                    String value = item.isTextual() ? item.asText() : firstText(item, "descriptorName", "name", "term", "keyword", "value");
                    if (notBlank(value)) out.add(topicDisplayName(value));
                }
                return;
            }
        } catch (Exception ignored) {
        }
        for (String value : raw.split("[;,；]")) if (notBlank(value)) out.add(topicDisplayName(value));
    }

    private String topicDisplayName(String value) {
        String clean = value.trim();
        if (clean.regionMatches(true, 0, "Databases, ", 0, "Databases, ".length()))
            return clean.substring("Databases, ".length()).trim();
        int comma = clean.indexOf(", ");
        if (comma > 0 && clean.indexOf(',', comma + 2) < 0)
            return clean.substring(comma + 2).trim() + " " + clean.substring(0, comma).trim();
        return clean;
    }

    public String relationAffiliationSummary(JsonNode row, String authorsJson) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        try {
            JsonNode authors = objectMapper.readTree(authorsJson);
            if (authors.isArray()) for (JsonNode a : authors) {
                String v = text(a, "institutionName");
                if (v != null && !v.isBlank()) values.add(v);
            }
        } catch (Exception ignored) {
        }
        if (values.isEmpty()) for (JsonNode a : row.path("authors")) {
            String v = text(a, "affiliation");
            if (v != null && !v.isBlank()) values.add(v.trim().replaceAll("\\s+", " "));
        }
        return values.isEmpty() ? text(row, "affiliationInfo") : String.join("; ", values);
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}

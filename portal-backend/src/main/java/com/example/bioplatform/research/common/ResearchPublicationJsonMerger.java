package com.example.bioplatform.research.common;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.*;

/**
 * 合并跨来源文献JSON时按业务对象补充信息，避免用“较长字符串”覆盖有用字段。
 */
public final class ResearchPublicationJsonMerger {
    private ResearchPublicationJsonMerger() {
    }

    /**
     * 按作者标识或姓名合并作者数组；相同作者从另一来源得到的署名机构会补入，
     * 不同作者仍按来源顺序保留。无法识别为数组的内容采用非空值优先策略。
     */
    public static String mergeAuthors(String left, String right, ObjectMapper mapper) {
        if (blank(left)) return right;
        if (blank(right)) return left;
        try {
            JsonNode a = mapper.readTree(left), b = mapper.readTree(right);
            if (!a.isArray() || !b.isArray()) return longer(left, right);
            List<ObjectNode> authors = new ArrayList<>();
            Map<String, List<Integer>> indexesByName = new LinkedHashMap<>();
            appendAuthors(a, authors, indexesByName, mapper);
            appendAuthors(b, authors, indexesByName, mapper);
            ArrayNode result = mapper.createArrayNode();
            authors.forEach(result::add);
            return mapper.writeValueAsString(result);
        } catch (Exception ignored) {
            return longer(left, right);
        }
    }

    /**
     * 合并资源关联JSON数组，按数据库、资源编号、关联 PMID 去重。
     */
    public static String mergeBanks(String left, String right, ObjectMapper mapper) {
        if (blank(left)) return right;
        if (blank(right)) return left;
        try {
            JsonNode a = mapper.readTree(left), b = mapper.readTree(right);
            if (!a.isArray() || !b.isArray()) return longer(left, right);
            List<ObjectNode> banks = new ArrayList<>();
            Map<String, Integer> indexByIdentity = new LinkedHashMap<>();
            appendBanks(a, banks, indexByIdentity, mapper);
            appendBanks(b, banks, indexByIdentity, mapper);
            ArrayNode result = mapper.createArrayNode();
            banks.forEach(result::add);
            return mapper.writeValueAsString(result);
        } catch (Exception ignored) {
            return longer(left, right);
        }
    }

    private static void appendAuthors(JsonNode source, List<ObjectNode> authors,
                                      Map<String, List<Integer>> indexes, ObjectMapper mapper) {
        for (JsonNode node : source) {
            if (!node.isObject()) continue;
            ObjectNode candidate = (ObjectNode) node.deepCopy();
            String name = authorNameKey(candidate);
            List<Integer> sameName = name.isEmpty() ? List.of() : indexes.getOrDefault(name, List.of());
            String incomingInstitution = institutionIdentity(candidate);
            Integer match = sameName.stream().filter(index -> {
                String existingInstitution = institutionIdentity(authors.get(index));
                return !incomingInstitution.isEmpty() && incomingInstitution.equals(existingInstitution);
            }).findFirst().orElse(null);
            if (match == null && sameName.size() == 1) {
                String existingInstitution = institutionIdentity(authors.get(sameName.get(0)));
                if (incomingInstitution.isEmpty() || existingInstitution.isEmpty()) match = sameName.get(0);
            }
            if (match != null) {
                mergeAuthorFields(authors.get(match), candidate);
                continue;
            }
            int newIndex = authors.size();
            authors.add(candidate);
            if (!name.isEmpty()) indexes.computeIfAbsent(name, ignored -> new ArrayList<>()).add(newIndex);
        }
    }

    private static void appendBanks(JsonNode source, List<ObjectNode> banks,
                                    Map<String, Integer> indexes, ObjectMapper mapper) {
        for (JsonNode node : source) {
            if (!node.isObject()) continue;
            ObjectNode candidate = (ObjectNode) node.deepCopy();
            String identity = first(candidate, "db") + "|" + first(candidate, "accession") + "|"
                    + first(candidate, "pmid") + "|" + first(candidate, "link");
            identity = normalize(identity);
            Integer index = identity.equals("|||") ? null : indexes.get(identity);
            if (index == null) {
                if (!identity.equals("|||")) indexes.put(identity, banks.size());
                banks.add(candidate);
            } else {
                mergeMissingFields(banks.get(index), candidate);
            }
        }
    }

    private static void mergeAuthorFields(ObjectNode target, ObjectNode source) {
        source.fields().forEachRemaining(entry -> {
            String field = entry.getKey();
            JsonNode incoming = entry.getValue();
            JsonNode existing = target.get(field);
            if (isAffiliationField(field) && incoming.isTextual() && !blank(incoming.asText())) {
                String merged = mergeText(existing == null ? null : existing.asText(), incoming.asText());
                target.put(field, merged);
            } else if (existing == null || existing.isNull() || (existing.isTextual() && blank(existing.asText()))) {
                target.set(field, incoming.deepCopy());
            }
        });
    }

    private static void mergeMissingFields(ObjectNode target, ObjectNode source) {
        source.fields().forEachRemaining(entry -> {
            JsonNode existing = target.get(entry.getKey());
            if (existing == null || existing.isNull() || (existing.isTextual() && blank(existing.asText())))
                target.set(entry.getKey(), entry.getValue().deepCopy());
        });
    }

    private static String authorNameKey(ObjectNode author) {
        String identifier = first(author, "identifier");
        if (!identifier.isEmpty()) return "id:" + normalize(first(author, "identifierSource") + ":" + identifier);
        String direct = first(author, "fullName", "collectiveName");
        if (direct.isEmpty()) direct = first(author, "firstName", "foreName", "givenName") + " "
                + first(author, "lastName", "familyName");
        return normalize(direct);
    }

    /**
     * 将可识别机构纳入作者身份，避免同名但不同机构的作者被合成一人。
     */
    private static String institutionIdentity(ObjectNode author) {
        String institution = first(author, "institutionName");
        if (institution.isEmpty())
            institution = first(author, "affiliation", "institution", "organization", "affiliationRaw");
        return normalize(institution);
    }

    private static String first(ObjectNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value != null && value.isValueNode() && !value.isNull() && !blank(value.asText()))
                return value.asText().trim();
        }
        return "";
    }

    private static boolean isAffiliationField(String field) {
        String normalized = field.toLowerCase(Locale.ROOT);
        return normalized.equals("affiliation") || normalized.equals("institution")
                || normalized.equals("organization") || normalized.equals("affiliationname");
    }

    private static String mergeText(String left, String right) {
        if (blank(left)) return right.trim();
        if (blank(right) || normalize(left).equals(normalize(right))) return left.trim();
        return left.trim() + "; " + right.trim();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static String longer(String left, String right) {
        return right == null || right.length() <= left.length() ? left : right;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}

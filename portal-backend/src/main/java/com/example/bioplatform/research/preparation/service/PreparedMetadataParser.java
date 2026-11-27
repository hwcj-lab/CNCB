package com.example.bioplatform.research.preparation.service;

import com.example.bioplatform.research.collection.common.dto.SourcePublicationRecord;
import com.example.bioplatform.research.common.PublicationMetadataProcessor;
import com.example.bioplatform.research.common.PublicationValues;
import com.example.bioplatform.research.preparation.model.PreparedMember;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Pure, bounded per-document normalization. No upstream requests or persistent person entity.
 */
@Component
public class PreparedMetadataParser {
    public static final List<String> TYPES = List.of("AUTHOR", "INSTITUTION", "FUND", "TOPIC", "YEAR");
    private final ObjectMapper json;
    private final PublicationMetadataProcessor processor;

    public PreparedMetadataParser(ObjectMapper json) {
        this.json = json;
        processor = new PublicationMetadataProcessor(json);
    }

    public static String table(String type) {
        if (!TYPES.contains(type)) throw new IllegalArgumentException("Unknown member type");
        return "research_prepared_" + type.toLowerCase(Locale.ROOT);
    }

    public record Parsed(Map<String, List<PreparedMember>> members, String contentHash) {
    }

    public Parsed parse(SourcePublicationRecord row, Map<String, String> dictionary) {
        validate(row.getAuthorsJson());
        validate(row.getGrantsJson());
        validate(row.getMeshHeadingsJson());
        validate(row.getKeywordList());
        validate(row.getAffiliationInfo());
        var input = new PublicationMetadataProcessor.Input(row.getAuthorsJson(), row.getAffiliationInfo(), row.getGrantsJson(), row.getKeywordList(), row.getMeshHeadingsJson(), row.getCncbBanksJson());
        var output = processor.parse(input, dictionary);
        Map<String, LinkedHashMap<String, PreparedMember>> values = new LinkedHashMap<>();
        TYPES.forEach(t -> values.put(t, new LinkedHashMap<>()));
        List<JsonNode> nodes = new ArrayList<>();
        try {
            if (row.getAuthorsJson() != null) authorNodes(json.readTree(row.getAuthorsJson()), nodes);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid authors", ex);
        }
        List<PublicationMetadataProcessor.AuthorIdentity> authors = new ArrayList<>();
        Set<String> known = new LinkedHashSet<>();
        for (var node : nodes) {
            var single = processor.parse(new PublicationMetadataProcessor.Input("[" + node.toString() + "]", null, null, null, null, null), Map.of());
            for (var author : single.authors()) {
                authors.add(author);
                if (author.institution() != null)
                    for (String v : author.institution().split(";\\s*")) if (!v.isBlank()) known.add(v);
            }
        }
        int order = 0;
        for (var author : authors) {
            order++;
            String selected = author.institution() == null && known.size() == 1 ? known.iterator().next() : author.institution();
            String[] institutions = selected == null ? new String[]{
                    ""
            }
                    : selected.split(";\\s*");
            for (String institution : institutions) {
                var m = member(author.name() + "\u001f" + institution, author.name() + (institution.isBlank() ? "" : " · " + institution));
                m.setAuthorName(author.name());
                m.setInstitutionName(institution.isBlank() ? null : institution);
                m.setAuthorOrder(order);
                values.get("AUTHOR").putIfAbsent(m.getMemberKey(), m);
                if (!institution.isBlank()) add(values, "INSTITUTION", institution);
            }
        }
        // Include every author's affiliation, including when other authors have pre-normalized names.
        output.institutions().forEach(v -> {
            for (String name : v.split(";\\s*")) if (!name.isBlank()) add(values, "INSTITUTION", name);
        });
        output.funds().forEach(v -> add(values, "FUND", v));
        output.topics().forEach(v -> add(values, "TOPIC", v));
        if (row.getPubYear() != null && row.getPubYear() > 0) {
            var m = member(row.getPubYear().toString(), row.getPubYear().toString());
            m.setStatYear(row.getPubYear());
            values.get("YEAR").put(m.getMemberKey(), m);
        }
        Map<String, List<PreparedMember>> result = new LinkedHashMap<>();
        values.forEach((t, v) -> result.put(t, new ArrayList<>(v.values())));
        try {
            return new Parsed(result, PublicationValues.sha("preparation-v1|" + json.writeValueAsString(result)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void validate(String value) {
        if (value == null || value.isBlank()) return;
        String v = value.trim();
        if (v.startsWith("[") || v.startsWith("{")) try {
            json.readTree(v);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Malformed metadata JSON; preparation batch rolled back", ex);
        }
    }

    private void authorNodes(JsonNode node, List<JsonNode> out) {
        if (node == null || node.isNull()) return;
        if (node.isTextual()) {
            if (!node.asText().isBlank()) {
                var author = json.createObjectNode();
                author.put("fullName", node.asText());
                out.add(author);
            }
            return;
        }
        if (node.isArray()) {
            node.forEach(n -> authorNodes(n, out));
            return;
        }
        if (!node.isObject()) return;
        if (node.hasNonNull("fullName") || node.hasNonNull("collectiveName") || node.hasNonNull("firstName") || node.hasNonNull("foreName") || node.hasNonNull("givenName") || node.hasNonNull("lastName") || node.hasNonNull("familyName")) {
            out.add(node);
            return;
        }
        node.elements().forEachRemaining(n -> {
            if (n.isContainerNode()) authorNodes(n, out);
        });
    }

    private static PreparedMember member(String key, String label) {
        var m = new PreparedMember();
        m.setMemberKey(PublicationValues.sha(key.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ")));
        m.setDisplayName(label);
        return m;
    }

    private static void add(Map<String, LinkedHashMap<String, PreparedMember>> values, String type, String label) {
        var m = member(label, label);
        values.get(type).putIfAbsent(m.getMemberKey(), m);
    }
}

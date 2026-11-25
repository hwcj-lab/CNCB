package com.example.bioplatform.research.common;

import com.fasterxml.jackson.databind.*;

import java.util.*;
import java.util.regex.*;

/**
 * 论文外链优先PMID/DOI，无标识时定位本篇记录明确关联的GSA或BioProject资源。
 */
public final class PublicationLinks {
    private static final Pattern GSA = Pattern.compile("(?i)\\bCRA[0-9]+\\b"), PROJECT = Pattern.compile("(?i)\\bPRJ(?:CA|NA|EB|DA)[A-Z0-9]+\\b");

    private PublicationLinks() {
    }

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

    private static String resource(String gsa, String projects) {
        Matcher m = GSA.matcher(gsa == null ? "" : gsa);
        if (m.find()) return "https://ngdc.cncb.ac.cn/gsa/browse/" + m.group().toUpperCase(Locale.ROOT);
        m = PROJECT.matcher(projects == null ? "" : projects);
        return m.find() ? "https://ngdc.cncb.ac.cn/bioproject/browse/" + m.group().toUpperCase(Locale.ROOT) : null;
    }
}

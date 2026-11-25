package com.example.bioplatform.research.collection.common.service.relation;

import com.example.bioplatform.research.collection.bioproject.service.impl.BioProjectPublicationParser;
import com.example.bioplatform.research.collection.gsa.service.impl.GsaPublicationParser;
import com.fasterxml.jackson.databind.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.*;

/**
 * 路径一：使用PMID详情已返回的cncbBanks（或资源原有链接），按db工厂解析链接。
 */
@Component
@RequiredArgsConstructor
public class CncbBanksDiscoveryStrategy implements RelationDiscoveryStrategy {
    private final RelatedResourceResolverFactory resolvers;
    private final RelationPageClient pages;
    private final ObjectMapper json;
    private static final Pattern PMID = Pattern.compile("(?i)(?:pubmed(?:[.]ncbi[.]nlm[.]nih[.]gov)?[/=:? ]+|pmid[^0-9]{0,20})([1-9][0-9]{5,11})");

    public String code() {
        return "CNCB_BANKS";
    }

    public int priority() {
        return 100;
    }

    public boolean supports(RelationDiscoveryInput input) {
        return !links(input.cncbBanksJson()).isEmpty();
    }

    public RelationDiscoveryResult discover(RelationDiscoveryInput input, BeforeRequest control) throws Exception {
        Map<String, String> discovered = new LinkedHashMap<>();
        Map<String, JsonNode> references = new LinkedHashMap<>();
        var grants = json.createArrayNode();
        for (Link link : links(input.cncbBanksJson())) {
            control.run();
            var resolver = resolvers.find(link.databaseName());
            List<String> found;
            if (resolver != null) {
                var data = resolver.findRelatedPublications(link.accession(), link.url());
                found = data.pmids();
                if (input.acceptsPublicationReferences()) for (JsonNode reference : data.publicationRecords()) {
                    if (com.example.bioplatform.research.common.PublicationValues.text(reference, "pubmedId").matches("[1-9][0-9]{5,11}"))
                        continue;
                    var seed = BioProjectPublicationParser.parse(reference, json);
                    String current = input.identifierType() + ":" + input.identifierValue();
                    if (seed.getRecordKey().equals(current) || input.identifierType().equals("SOURCE_KEY") && seed.getRecordKey().equals(input.identifierValue()))
                        continue;
                    references.putIfAbsent(seed.getRecordKey(), reference);
                }
                JsonNode funding = data.grants();
                if (funding != null && funding.isArray()) funding.forEach(item -> grants.add(item.deepCopy()));
                else if (funding != null && !funding.isNull() && !funding.isMissingNode())
                    grants.add(funding.deepCopy());
            } else {
                Set<String> values = new LinkedHashSet<>();
                Matcher match = PMID.matcher(pages.get(link.url()));
                while (match.find()) values.add(match.group(1));
                found = List.copyOf(values);
            }
            for (String pmid : found) if (!pmid.equals(input.pmid())) discovered.putIfAbsent(pmid, link.url());
        }
        return new RelationDiscoveryResult(code(), discovered, List.copyOf(references.values()), grants);
    }

    private List<Link> links(String value) {
        if (value == null || value.isBlank()) return List.of();
        try {
            List<Link> links = new ArrayList<>();
            for (JsonNode item : json.readTree(value)) {
                String url = text(item, "link");
                if (url != null && (url.startsWith("https://") || url.startsWith("http://")))
                    links.add(new Link(text(item, "db"), text(item, "accession"), url));
            }
            return links;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private static String text(JsonNode node, String field) {
        var value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private record Link(String databaseName, String accession, String url) {
    }
}

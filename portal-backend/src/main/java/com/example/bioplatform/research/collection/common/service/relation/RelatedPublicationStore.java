package com.example.bioplatform.research.collection.common.service.relation;

import com.example.bioplatform.research.common.GrantPayloads;
import com.example.bioplatform.research.common.ResearchPublicationJsonMerger;
import com.example.bioplatform.research.collection.common.dto.RelatedPublicationWriteRow;
import com.example.bioplatform.research.integration.service.ResearchPublicationIntegrationService;
import com.example.bioplatform.research.collection.gsa.service.impl.GsaPublicationParser;
import com.example.bioplatform.research.integration.service.impl.ResearchPublicationSourceRegistry;
import com.example.bioplatform.research.integration.service.impl.ResearchPublicationSourceRegistry;
import com.example.bioplatform.research.integration.service.ResearchPublicationIntegrationService;
import com.example.bioplatform.research.collection.common.dto.RelatedPublicationWriteRow;
import com.example.bioplatform.research.common.GrantPayloads;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 来源写入工厂：先合并已有原始元数据，再调用专用写入器，最后增量同步整合表。
 */
@Component
public class RelatedPublicationStore {
    private final ResearchPublicationSourceRegistry sourceRegistry;
    private final ResearchPublicationIntegrationService publicationIntegration;
    private final ObjectMapper json;
    private final List<RelatedPublicationWriter> writers;
    private final com.example.bioplatform.research.collection.openlb.mapper.OpenLbRoutingMapper routes;
    private final com.example.bioplatform.research.collection.common.service.SourceMembership membership;

    public RelatedPublicationStore(ResearchPublicationSourceRegistry sources, ResearchPublicationIntegrationService integration, ObjectMapper json, List<RelatedPublicationWriter> writers, com.example.bioplatform.research.collection.openlb.mapper.OpenLbRoutingMapper routes, com.example.bioplatform.research.collection.common.service.SourceMembership membership) {
        this.routes=routes;this.membership=membership;
        this.sourceRegistry = sources;
        this.publicationIntegration = integration;
        this.json = json;
        this.writers = writers.stream().sorted(Comparator.comparingInt(RelatedPublicationWriter::priority)).toList();
    }

    public void write(String databaseCode, List<RelatedPublicationWriteRow> rows) {
        // 刷新/关联发现不能清空主采集已经保存的项目链接、署名机构和基金。
        var prior = sourceRegistry.require(databaseCode).loadRecordsByIdentifiers(rows.stream().map(RelatedPublicationWriteRow::pmid).filter(Objects::nonNull).toList(), rows.stream().map(RelatedPublicationWriteRow::doi).filter(Objects::nonNull).toList());
        List<RelatedPublicationWriteRow> merged = new ArrayList<>();
        for (var row : rows) {
            var old = prior.stream().filter(p -> row.pmid() != null && row.pmid().equals(p.getPmid()) || row.doi() != null && row.doi().equalsIgnoreCase(Objects.toString(p.getDoi(), ""))).findFirst().orElse(null);
            if (old == null) {
                merged.add(row);
                continue;
            }
            String authors = com.example.bioplatform.research.common.ResearchPublicationJsonMerger.mergeAuthors(old.getAuthorsJson(), row.authorsJson(), json);
            String grants = GrantPayloads.merge(old.getGrantsJson(), row.grantsJson(), json);
            String banks = com.example.bioplatform.research.common.ResearchPublicationJsonMerger.mergeBanks(old.getCncbBanksJson(), row.cncbBanksJson(), json);
            merged.add(new RelatedPublicationWriteRow(row.pmid(), row.doi(), row.pubYear(), row.journalTitle(), row.title(), row.abstractText(), row.affiliationInfo(), row.keywordList(), authors, row.meshHeadingsJson(), grants, banks, row.rawPayload(), row.sourceUrl(), sha256(row.sourceHash() + authors + grants + banks), row.related()));
        }
        writers.stream().filter(writer -> writer.supports(databaseCode)).findFirst().orElseThrow(() -> new IllegalArgumentException("未配置来源写入器：" + databaseCode)).write(databaseCode, merged);
        Map<String,String> bankRoutes=new HashMap<>();
        for(var route:routes.routes())bankRoutes.put(route.get("bank_db").toString().trim().toLowerCase(Locale.ROOT),route.get("source_code").toString());
        var owned=merged.stream().filter(row->membership.belongs(databaseCode,row.cncbBanksJson(),bankRoutes)).toList();
        if(!owned.isEmpty())routes.promotePrimary(com.example.bioplatform.research.collection.openlb.service.impl.OpenLbRoutingService.table(databaseCode),owned.stream().map(RelatedPublicationWriteRow::pmid).filter(Objects::nonNull).toList(),owned.stream().map(RelatedPublicationWriteRow::doi).filter(Objects::nonNull).toList());
        publicationIntegration.syncIdentifiers(rows.stream().map(RelatedPublicationWriteRow::pmid).filter(Objects::nonNull).toList(),
                rows.stream().map(RelatedPublicationWriteRow::doi).filter(Objects::nonNull).toList());
    }

    private static String sha256(String value) {
        return com.example.bioplatform.research.common.PublicationValues.sha(value);
    }
}

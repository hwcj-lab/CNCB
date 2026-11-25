package com.example.bioplatform.research.collection.bioproject.service.relation;

import com.example.bioplatform.research.collection.bioproject.service.impl.BioProjectMetadataClient;
import com.example.bioplatform.research.collection.common.service.relation.RelationDiscoveryInput;
import com.example.bioplatform.research.collection.common.service.relation.RelationDiscoveryResult;
import com.example.bioplatform.research.collection.common.service.relation.RelationDiscoveryStrategy;
import com.example.bioplatform.research.collection.bioproject.service.impl.BioProjectMetadataClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 路径二：无cncbBanks链接时读取来源已有的BioProject；保留原项目回退的结果投影。
 */
@Component
@RequiredArgsConstructor
public class BioProjectDiscoveryStrategy implements RelationDiscoveryStrategy {
    private final BioProjectMetadataClient projects;
    private final ObjectMapper json;

    public String code() {
        return "BIOPROJECT";
    }

    public int priority() {
        return 200;
    }

    public boolean supports(RelationDiscoveryInput input) {
        return input.bioprojectAccession() != null && !input.bioprojectAccession().isBlank();
    }

    public RelationDiscoveryResult discover(RelationDiscoveryInput input, BeforeRequest control) {
        var project = projects.fetch(input.bioprojectAccession());
        Map<String, String> found = new LinkedHashMap<>();
        for (String pmid : projects.publicationPmids(project))
            if (!pmid.equals(input.pmid()))
                found.putIfAbsent(pmid, "https://ngdc.cncb.ac.cn/bioproject/browse/" + input.bioprojectAccession());
        // 旧回退只返回关联PMID；基金及完整出版记录由BioProject资源Resolver/主采集负责，避免重构时改变结果。
        return new RelationDiscoveryResult(code(), found, List.of(), json.createArrayNode());
    }
}

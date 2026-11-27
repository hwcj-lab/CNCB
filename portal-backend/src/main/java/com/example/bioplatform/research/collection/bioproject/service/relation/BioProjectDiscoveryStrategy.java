package com.example.bioplatform.research.collection.bioproject.service.relation;

import com.example.bioplatform.research.collection.bioproject.service.impl.BioProjectMetadataClient;
import com.example.bioplatform.research.collection.common.service.relation.RelationDiscoveryInput;
import com.example.bioplatform.research.collection.common.service.relation.RelationDiscoveryResult;
import com.example.bioplatform.research.collection.common.service.relation.RelationDiscoveryStrategy;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 路径二：无cncbBanks链接时读取来源已有的BioProject；保留原项目回退的结果投影。
 */
@Component
@RequiredArgsConstructor
public class BioProjectDiscoveryStrategy implements RelationDiscoveryStrategy {
    /**
     * projects：BioProject元数据访问组件或项目结果集合。
     */
    private final BioProjectMetadataClient projects;
    /**
     * json：JSON解析与序列化组件。
     */
    private final ObjectMapper json;

    /**
     * 返回当前发现策略的稳定编码，用于选择路径和记录发现来源。
     * @return 按上述规则计算或读取的String结果
     */
    public String code() {
        return "BIOPROJECT";
    }

    /**
     * 返回策略选择优先级，用于在来源专用实现和通用实现之间确定处理顺序。
     */
    public int priority() {
        return 200;
    }

    /**
     * 判断当前来源或输入能否交给本实现处理；专用实现优先于通用回退实现。
     */
    public boolean supports(RelationDiscoveryInput input) {
        return input.bioprojectAccession() != null && !input.bioprojectAccession().isBlank();
    }

    /**
     * 按本策略从已有资源元数据发现关联目标，返回目标及证据；不自行展开后续递归。
     * @param input 本次处理的统一输入
     * @param control 每次请求前执行的限速或停止检查回调
     * @return 按上述规则计算或读取的RelationDiscoveryResult结果
     */
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

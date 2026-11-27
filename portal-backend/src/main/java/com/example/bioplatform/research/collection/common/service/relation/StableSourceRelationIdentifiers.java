package com.example.bioplatform.research.collection.common.service.relation;

import com.example.bioplatform.common.BusinessException;
import com.example.bioplatform.research.collection.genbase.service.relation.GenBaseRelationSupport;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * 已有GSA/BioProject稳定来源键适配器；新增不同库时新增resolver，不扩展队列分支。
 */
@Component
public class StableSourceRelationIdentifiers implements RelationIdentifierResolver {
    /**
     * 判断当前来源或输入能否交给本实现处理；专用实现优先于通用回退实现。
     */
    public boolean supports(String database) {
        return Set.of("GSA", "BIOPROJECT").contains(database);
    }

    /**
     * 返回策略选择优先级，用于在来源专用实现和通用实现之间确定处理顺序。
     */
    public int priority() {
        return 20;
    }

    public Identifier resolve(String database, Map<String, String> ids) {
        if (ids.containsKey("pmid") && ids.get("pmid").matches("[1-9][0-9]+"))
            return new Identifier("PMID", ids.get("pmid"));
        if (ids.containsKey("doi")) return new Identifier("DOI", GenBaseRelationSupport.doi(ids.get("doi")));
        if (ids.containsKey("sourcekey")) return new Identifier("SOURCE_KEY", ids.get("sourcekey"));
        throw new BusinessException(HttpStatus.BAD_REQUEST, "GSA_IDENTIFIER_REQUIRED", "GSA请填写pmid、doi或sourceKey之一");
    }
}

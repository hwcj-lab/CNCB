package com.example.bioplatform.research.collection.common.service.relation;

import com.example.bioplatform.common.BusinessException;
import com.example.bioplatform.research.collection.common.dto.RelatedPublicationWriteRow;
import com.example.bioplatform.research.collection.common.mapper.PublicationRelationCollectionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 同结构来源的默认PMID写入和唯一值规则；专用适配器优先于此兜底。
 */
@Component
@RequiredArgsConstructor
public class RoutedRelationSupport implements RelatedPublicationWriter, RelationIdentifierResolver {
    private final PublicationRelationCollectionMapper mapper;

    public boolean supports(String database) {
        return true;
    }

    public int priority() {
        return 1000;
    }

    public void write(String database, List<RelatedPublicationWriteRow> rows) {
        mapper.upsertOpenLbRelated(database, rows);
    }

    public Identifier resolve(String database, Map<String, String> ids) {
        String pmid = ids.get("pmid");
        if (pmid == null || !pmid.matches("[0-9]+"))
            throw new BusinessException(HttpStatus.BAD_REQUEST, "OPENLB_PMID_REQUIRED", "OpenLB唯一值必须填写数字pmid");
        return new Identifier("PMID", pmid);
    }
}

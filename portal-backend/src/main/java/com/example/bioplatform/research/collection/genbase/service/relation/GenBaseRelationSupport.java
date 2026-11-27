package com.example.bioplatform.research.collection.genbase.service.relation;

import com.example.bioplatform.common.BusinessException;
import com.example.bioplatform.research.collection.common.dto.RelatedPublicationWriteRow;
import com.example.bioplatform.research.collection.common.mapper.PublicationRelationCollectionMapper;
import com.example.bioplatform.research.collection.common.service.relation.RelatedPublicationWriter;
import com.example.bioplatform.research.collection.common.service.relation.RelationIdentifierResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * GenBase自己的唯一值和表结构写入放在一起；公共队列不识别paper_id字段。
 */
@Component
@RequiredArgsConstructor
public class GenBaseRelationSupport implements RelatedPublicationWriter, RelationIdentifierResolver {
    private final PublicationRelationCollectionMapper mapper;

    /**
     * 判断当前来源或输入能否交给本实现处理；专用实现优先于通用回退实现。
     */
    public boolean supports(String database) {
        return "GENBASE".equals(database);
    }

    /**
     * 返回策略选择优先级，用于在来源专用实现和通用实现之间确定处理顺序。
     */
    public int priority() {
        return 10;
    }

    public void write(String database, List<RelatedPublicationWriteRow> rows) {
        mapper.upsertGenBaseRelated(rows);
    }

    public Identifier resolve(String database, Map<String, String> ids) {
        if (ids.containsKey("paperid") && ids.get("paperid").matches("[0-9]+"))
            return new Identifier("PAPER_ID", ids.get("paperid"));
        if (ids.containsKey("pmid") && ids.get("pmid").matches("[0-9]+"))
            return new Identifier("PMID", ids.get("pmid"));
        if (ids.containsKey("doi")) return new Identifier("DOI", doi(ids.get("doi")));
        throw new BusinessException(HttpStatus.BAD_REQUEST, "GENBASE_IDENTIFIER_REQUIRED", "GenBase请填写paperId、pmid或doi之一");
    }

    public static String doi(String value) {
        return value.trim().replaceFirst("(?i)^https?://(?:dx\\.)?doi\\.org/", "").toLowerCase(Locale.ROOT);
    }
}

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
    /**
     * mapper：本组件使用的持久层接口，SQL由对应XML维护。
     */
    private final PublicationRelationCollectionMapper mapper;

    /**
     * 判断当前来源或输入能否交给本实现处理；专用实现优先于通用回退实现。
     */
    public boolean supports(String database) {
        return true;
    }

    /**
     * 返回策略选择优先级，用于在来源专用实现和通用实现之间确定处理顺序。
     */
    public int priority() {
        return 1000;
    }

    /**
     * 把统一文献记录写入指定来源；来源结构差异由本实现处理，不在公共关联队列中硬编码。
     * @param database 本次处理的数据库来源编码
     * @param rows 本批待处理记录
     */
    public void write(String database, List<RelatedPublicationWriteRow> rows) {
        mapper.upsertOpenLbRelated(database, rows);
    }

    /**
     * 把原始标识解析成规范的类型与值，或建立当前上下文的身份对应关系。
     * @param database 本次处理的数据库来源编码
     * @param ids 待检索主键集合
     * @return 按上述规则计算或读取的Identifier结果
     */
    public Identifier resolve(String database, Map<String, String> ids) {
        String pmid = ids.get("pmid");
        if (pmid == null || !pmid.matches("[0-9]+"))
            throw new BusinessException(HttpStatus.BAD_REQUEST, "OPENLB_PMID_REQUIRED", "OpenLB唯一值必须填写数字pmid");
        return new Identifier("PMID", pmid);
    }
}

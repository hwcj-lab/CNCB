package com.example.bioplatform.research.collection.common.service.relation;

import com.example.bioplatform.research.collection.common.dto.RelatedPublicationWriteRow;

import java.util.List;

/**
 * 原库专用写入SPI；新数据库结构不同，新增实现即可，不修改关联队列。
 */
public interface RelatedPublicationWriter {
    boolean supports(String database);

    int priority();

    void write(String database, List<RelatedPublicationWriteRow> rows);
}

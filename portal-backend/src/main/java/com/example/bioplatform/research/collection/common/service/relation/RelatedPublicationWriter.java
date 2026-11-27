package com.example.bioplatform.research.collection.common.service.relation;

import com.example.bioplatform.research.collection.common.dto.RelatedPublicationWriteRow;

import java.util.List;

/**
 * 原库专用写入SPI；新数据库结构不同，新增实现即可，不修改关联队列。
 */
public interface RelatedPublicationWriter {
    /**
     * 判断此写入器是否支持database来源编码；写入工厂只会从匹配实现中选择执行者。
     * @param database 本次处理的数据库来源编码
     * @return true表示满足上述条件，false表示不满足
     */
    boolean supports(String database);

    /**
     * 返回写入器选择优先级；工厂按升序选择首个匹配实现，专用实现应排在通用回退之前。
     * @return 选择优先级，数值越小越先匹配
     */
    int priority();

    /**
     * 将统一记录批量写入database指定的来源表；实现只处理本来源结构差异，公共Store负责合并与后续整合同步。
     * @param database 本次处理的数据库来源编码
     * @param rows 本批待处理记录
     */
    void write(String database, List<RelatedPublicationWriteRow> rows);
}

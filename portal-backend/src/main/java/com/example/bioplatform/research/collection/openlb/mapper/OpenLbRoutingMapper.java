package com.example.bioplatform.research.collection.openlb.mapper;

import com.example.bioplatform.research.collection.openlb.dto.OpenLbPublicationWriteRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * OpenLB 来源盘点、本地分流及目标库批量写入。
 */
@Mapper
public interface OpenLbRoutingMapper {

    /**
     * 查询本方法指定的记录；涉及openlb_pmid_task表。
     * SQL定义见 OpenLbRoutingMapper.xml 中同名语句。
     */
    List<String> inventoryPage(String after);


    /**
     * 查询本方法指定的记录；涉及openlb_bank_inventory_job表。
     * SQL定义见 OpenLbRoutingMapper.xml 中同名语句。
     */
    String cursor();


    /**
     * 更新本方法指定的记录；涉及openlb_bank_inventory_job表。
     * SQL定义见 OpenLbRoutingMapper.xml 中同名语句。
     */
    int progress(@Param("cursor") String cursor, @Param("message") String message);


    /**
     * 查询本方法指定的记录；涉及openlb_bank_route表。
     * SQL定义见 OpenLbRoutingMapper.xml 中同名语句。
     */
    List<Map<String, Object>> routes();


    /**
     * 写入本方法指定的记录；涉及openlb_bank_inventory表。唯一键冲突按SQL合并已有字段。
     * SQL定义见 OpenLbRoutingMapper.xml 中同名语句。
     */
    int inventory(@Param("pmid") String pmid, @Param("idx") int idx, @Param("name") String name, @Param("accession") String accession, @Param("link") String link, @Param("db") String db, @Param("bankPmid") String bankPmid);


    /**
     * 查询本方法指定的记录；涉及openlb_bank_inventory表。按指定业务键去重计数。
     * SQL定义见 OpenLbRoutingMapper.xml 中同名语句。
     */
    List<Map<String, Object>> summary();


    /**
     * 查询本方法指定的记录；涉及openlb_bank_inventory表。
     * SQL定义见 OpenLbRoutingMapper.xml 中同名语句。
     */
    List<Map<String, Object>> inventoryRows(@Param("offset") int offset, @Param("size") int size);


    /**
     * 查询本方法指定的记录；涉及openlb_publication表。
     * SQL定义见 OpenLbRoutingMapper.xml 中同名语句。
     */
    List<OpenLbPublicationWriteRow> legacy(long after);


    /**
     * 写入本方法指定的记录；涉及research_publication_relation表。已有唯一键冲突时跳过重复插入。
     * SQL定义见 OpenLbRoutingMapper.xml 中同名语句。
     */
    int copyRelations(@Param("code") String code, @Param("pmids") List<String> pmids);

    /**
     * 更新本方法指定的记录；涉及${table}表。动态表标识只能来自服务端白名单。
     * SQL定义见 OpenLbRoutingMapper.xml 中同名语句。
     */
    int promotePrimary(@Param("table") String table, @Param("pmids") List<String> pmids, @Param("dois") List<String> dois);

    /**
     * 写入本方法指定的记录；涉及genbase_publication表。唯一键冲突按SQL合并已有字段。
     * SQL定义见 OpenLbRoutingMapper.xml 中同名语句。
     */
    int writeGenBase(@Param("rows") List<OpenLbPublicationWriteRow> rows);

    /**
     * 写入本方法指定的记录；涉及${table}表。唯一键冲突按SQL合并已有字段。动态表标识只能来自服务端白名单。
     * SQL定义见 OpenLbRoutingMapper.xml 中同名语句。
     */
    int write(@Param("table") String table, @Param("rows") List<OpenLbPublicationWriteRow> rows);
}

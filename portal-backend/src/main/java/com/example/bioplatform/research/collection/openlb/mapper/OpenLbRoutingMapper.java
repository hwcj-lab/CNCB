package com.example.bioplatform.research.collection.openlb.mapper;

import com.example.bioplatform.research.collection.openlb.dto.OpenLbPublicationWriteRow;

import java.util.*;

import org.apache.ibatis.annotations.*;

/**
 * OpenLB 来源盘点、本地分流及目标库批量写入。
 */
@Mapper
public interface OpenLbRoutingMapper {

    List<String> inventoryPage(String after);


    String cursor();


    int progress(@Param("cursor") String cursor, @Param("message") String message);


    List<Map<String, Object>> routes();


    int inventory(@Param("pmid") String pmid, @Param("idx") int idx, @Param("name") String name, @Param("accession") String accession, @Param("link") String link, @Param("db") String db, @Param("bankPmid") String bankPmid);


    List<Map<String, Object>> summary();


    List<Map<String, Object>> inventoryRows(@Param("offset") int offset, @Param("size") int size);


    List<OpenLbPublicationWriteRow> legacy(long after);


    int copyRelations(@Param("code") String code, @Param("pmids") List<String> pmids);

    int promotePrimary(@Param("table") String table, @Param("pmids") List<String> pmids, @Param("dois") List<String> dois);

    int writeGenBase(@Param("rows") List<OpenLbPublicationWriteRow> rows);

    int write(@Param("table") String table, @Param("rows") List<OpenLbPublicationWriteRow> rows);
}

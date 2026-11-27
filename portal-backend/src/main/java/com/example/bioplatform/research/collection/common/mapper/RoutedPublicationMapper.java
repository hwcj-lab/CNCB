package com.example.bioplatform.research.collection.common.mapper;

import com.example.bioplatform.research.collection.common.dto.SourcePublicationRecord;

import java.util.*;

import org.apache.ibatis.annotations.*;
import com.example.bioplatform.research.collection.common.dto.SourcePublicationRecord;

/**
 * 使用已校验配置表名访问与OpenLB相同结构的各库文献表。
 */
@Mapper
public interface RoutedPublicationMapper {
    List<SourcePublicationRecord> page(@Param("table") String table, @Param("related") boolean related, @Param("after") long after, @Param("size") int size);

    long count(@Param("table") String table, @Param("related") boolean related);

    List<Integer> years(@Param("table") String table, @Param("related") boolean related);

    List<SourcePublicationRecord> identifiers(@Param("table") String table, @Param("pmids") List<String> pmids, @Param("dois") List<String> dois);
    List<SourcePublicationRecord> changedPage(@Param("table") String table, @Param("related") boolean related, @Param("time") java.time.LocalDateTime time, @Param("after") long after, @Param("until") java.time.LocalDateTime until, @Param("size") int size);
}

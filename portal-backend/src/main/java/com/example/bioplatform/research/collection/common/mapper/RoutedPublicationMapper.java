package com.example.bioplatform.research.collection.common.mapper;

import com.example.bioplatform.research.collection.common.dto.SourcePublicationRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 使用已校验配置表名访问与OpenLB相同结构的各库文献表。
 */
@Mapper
public interface RoutedPublicationMapper {
    /**
     * 查询本方法指定的记录；涉及${table}表。动态表标识只能来自服务端白名单。
     * SQL定义见 RoutedPublicationMapper.xml 中同名语句。
     */
    List<SourcePublicationRecord> page(@Param("table") String table, @Param("related") boolean related, @Param("after") long after, @Param("size") int size);

    /**
     * 查询本方法指定的记录；涉及${table}表。动态表标识只能来自服务端白名单。
     * SQL定义见 RoutedPublicationMapper.xml 中同名语句。
     */
    long count(@Param("table") String table, @Param("related") boolean related);

    /**
     * 查询本方法指定的记录；涉及${table}表。动态表标识只能来自服务端白名单。
     * SQL定义见 RoutedPublicationMapper.xml 中同名语句。
     */
    List<Integer> years(@Param("table") String table, @Param("related") boolean related);

    /**
     * 查询本方法指定的记录；涉及${table}表。动态表标识只能来自服务端白名单。
     * SQL定义见 RoutedPublicationMapper.xml 中同名语句。
     */
    List<SourcePublicationRecord> identifiers(@Param("table") String table, @Param("pmids") List<String> pmids, @Param("dois") List<String> dois);

    /**
     * 查询本方法指定的记录；涉及${table}表。动态表标识只能来自服务端白名单。
     * SQL定义见 RoutedPublicationMapper.xml 中同名语句。
     */
    List<SourcePublicationRecord> changedPage(@Param("table") String table, @Param("related") boolean related, @Param("time") java.time.LocalDateTime time, @Param("after") long after, @Param("until") java.time.LocalDateTime until, @Param("size") int size);
}

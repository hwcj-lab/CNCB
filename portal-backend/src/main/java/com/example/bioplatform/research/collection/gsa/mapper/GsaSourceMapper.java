package com.example.bioplatform.research.collection.gsa.mapper;

import com.example.bioplatform.research.collection.common.dto.SourcePublicationRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * GSA来源统计与穿透读取；原始列表稳定键优先，兼容OpenLB分流的PMID键。
 */
@Mapper
public interface GsaSourceMapper {
    /**
     * 查询本方法指定的记录；涉及gsa_publication表。
     * SQL定义见 GsaSourceMapper.xml 中同名语句。
     */
    List<SourcePublicationRecord> page(@Param("related") boolean related, @Param("after") long after, @Param("size") int size);

    /**
     * 查询本方法指定的记录；涉及gsa_publication表。
     * SQL定义见 GsaSourceMapper.xml 中同名语句。
     */
    long count(boolean related);

    /**
     * 查询本方法指定的记录；涉及gsa_publication表。
     * SQL定义见 GsaSourceMapper.xml 中同名语句。
     */
    List<Integer> years(boolean related);

    /**
     * 查询本方法指定的记录；涉及gsa_publication表。
     * SQL定义见 GsaSourceMapper.xml 中同名语句。
     */
    List<SourcePublicationRecord> identifiers(@Param("pmids") List<String> pmids, @Param("dois") List<String> dois);

    /**
     * 查询本方法指定的记录；涉及gsa_publication表。
     * SQL定义见 GsaSourceMapper.xml 中同名语句。
     */
    List<SourcePublicationRecord> keys(@Param("keys") List<String> keys);

    /**
     * 查询本方法指定的记录；涉及gsa_publication表。
     * SQL定义见 GsaSourceMapper.xml 中同名语句。
     */
    List<SourcePublicationRecord> changedPage(@Param("related") boolean related, @Param("time") java.time.LocalDateTime time, @Param("after") long after, @Param("until") java.time.LocalDateTime until, @Param("size") int size);
}

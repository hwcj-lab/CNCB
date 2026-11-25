package com.example.bioplatform.research.collection.bioproject.mapper;
import com.example.bioplatform.research.collection.common.dto.SourcePublicationRecord;

import java.util.*;

import org.apache.ibatis.annotations.*;

/**
 * BIOPROJECT来源统计与穿透读取；原始列表稳定键优先，兼容OpenLB分流的PMID键。
 */
@Mapper
public interface BioProjectSourceMapper {

    
    List<SourcePublicationRecord> page(@Param("related") boolean related, @Param("after") long after, @Param("size") int size);

    
    long count(boolean related);

    
    List<Integer> years(boolean related);

    
    List<SourcePublicationRecord> identifiers(@Param("pmids") List<String> pmids, @Param("dois") List<String> dois);

    
    List<SourcePublicationRecord> keys(@Param("keys") List<String> keys);
}

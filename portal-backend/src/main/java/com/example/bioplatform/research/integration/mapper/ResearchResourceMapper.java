package com.example.bioplatform.research.integration.mapper;

import com.example.bioplatform.research.collection.common.dto.PendingRelationRoot;
import com.example.bioplatform.research.collection.common.dto.RelationSourceRecord;
import com.example.bioplatform.research.integration.dto.ResearchResourceInput;
import com.example.bioplatform.research.collection.common.dto.SourcePublicationRecord;
import org.apache.ibatis.annotations.*;

import java.util.*;

/**
 * 只保存无原生文献的资源；有文献时停用既有资源行，保留旧快照引用。
 */
@Mapper
public interface ResearchResourceMapper {
    List<SourcePublicationRecord> page(@Param("code") String code, @Param("related") boolean related, @Param("after") long after, @Param("size") int size);

    long count(@Param("code") String code, @Param("related") boolean related);

    List<Integer> years(@Param("code") String code, @Param("related") boolean related);

    List<SourcePublicationRecord> keys(@Param("code") String code, @Param("ids") List<String> ids);

    int upsert(@Param("r") ResearchResourceInput input, @Param("hash") String hash, @Param("keys") String keys);

    int retire(@Param("code") String code, @Param("id") String id);

    List<PendingRelationRoot> pending(@Param("code") String code, @Param("failed") boolean failed, @Param("size") int size);

    RelationSourceRecord relationSource(@Param("code") String code, @Param("id") String id);

    int relationStatus(@Param("code") String code, @Param("id") String id, @Param("status") String status, @Param("error") String error);

    int grants(@Param("code") String code, @Param("id") String id, @Param("grants") String grants);
    List<SourcePublicationRecord> changedPage(@Param("code") String code, @Param("related") boolean related, @Param("time") java.time.LocalDateTime time, @Param("after") long after, @Param("until") java.time.LocalDateTime until, @Param("size") int size);
}

package com.example.bioplatform.research.integration.mapper;

import com.example.bioplatform.research.collection.common.dto.PendingRelationRoot;
import com.example.bioplatform.research.collection.common.dto.RelationSourceRecord;
import com.example.bioplatform.research.collection.common.dto.SourcePublicationRecord;
import com.example.bioplatform.research.integration.dto.ResearchResourceInput;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 只保存无原生文献的资源；有文献时停用既有资源行，保留旧快照引用。
 */
@Mapper
public interface ResearchResourceMapper {
    /**
     * 查询本方法指定的记录；涉及research_stat_publication表。
     * SQL定义见 ResearchResourceMapper.xml 中同名语句。
     */
    List<SourcePublicationRecord> page(@Param("code") String code, @Param("related") boolean related, @Param("after") long after, @Param("size") int size);

    /**
     * 查询本方法指定的记录；涉及research_stat_publication表。
     * SQL定义见 ResearchResourceMapper.xml 中同名语句。
     */
    long count(@Param("code") String code, @Param("related") boolean related);

    /**
     * 查询本方法指定的记录；涉及research_stat_publication表。
     * SQL定义见 ResearchResourceMapper.xml 中同名语句。
     */
    List<Integer> years(@Param("code") String code, @Param("related") boolean related);

    /**
     * 查询本方法指定的记录；涉及research_stat_publication表。
     * SQL定义见 ResearchResourceMapper.xml 中同名语句。
     */
    List<SourcePublicationRecord> keys(@Param("code") String code, @Param("ids") List<String> ids);

    /**
     * 写入本方法指定的记录；涉及research_stat_publication表。唯一键冲突按SQL合并已有字段。
     * SQL定义见 ResearchResourceMapper.xml 中同名语句。
     */
    int upsert(@Param("r") ResearchResourceInput input, @Param("hash") String hash, @Param("keys") String keys);

    /**
     * 更新本方法指定的记录；涉及research_stat_publication表。
     * SQL定义见 ResearchResourceMapper.xml 中同名语句。
     */
    int retire(@Param("code") String code, @Param("id") String id);

    /**
     * 查询本方法指定的记录；涉及research_stat_publication表。
     * SQL定义见 ResearchResourceMapper.xml 中同名语句。
     */
    List<PendingRelationRoot> pending(@Param("code") String code, @Param("failed") boolean failed, @Param("size") int size);

    /**
     * 查询本方法指定的记录；涉及research_stat_publication表。
     * SQL定义见 ResearchResourceMapper.xml 中同名语句。
     */
    RelationSourceRecord relationSource(@Param("code") String code, @Param("id") String id);

    /**
     * 更新本方法指定的记录；涉及research_stat_publication表。
     * SQL定义见 ResearchResourceMapper.xml 中同名语句。
     */
    int relationStatus(@Param("code") String code, @Param("id") String id, @Param("status") String status, @Param("error") String error);

    /**
     * 更新本方法指定的记录；涉及research_stat_publication表。
     * SQL定义见 ResearchResourceMapper.xml 中同名语句。
     */
    int grants(@Param("code") String code, @Param("id") String id, @Param("grants") String grants);

    /**
     * 查询本方法指定的记录；涉及research_stat_publication表。
     * SQL定义见 ResearchResourceMapper.xml 中同名语句。
     */
    List<SourcePublicationRecord> changedPage(@Param("code") String code, @Param("related") boolean related, @Param("time") java.time.LocalDateTime time, @Param("after") long after, @Param("until") java.time.LocalDateTime until, @Param("size") int size);
}

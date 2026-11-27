package com.example.bioplatform.research.collection.gsa.mapper;

import com.example.bioplatform.research.collection.gsa.dto.GsaEnrichmentTask;
import com.example.bioplatform.research.collection.gsa.dto.GsaEnrichmentWrite;
import com.example.bioplatform.research.collection.gsa.dto.GsaPublicationSeed;
import com.example.bioplatform.research.collection.gsa.vo.GsaCollectionJob;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * GSA专用持久化SQL：任务断点、列表UPSERT、补全批量写入及来源适配读取。
 */
@Mapper
public interface GsaCollectionMapper {
    /**
     * 更新本方法指定的记录；涉及gsa_collection_job表。
     * SQL定义见 GsaCollectionMapper.xml 中同名语句。
     */
    int recover();

    /**
     * 查询本方法指定的记录；涉及gsa_collection_job表。
     * SQL定义见 GsaCollectionMapper.xml 中同名语句。
     */
    GsaCollectionJob resumable();

    /**
     * 查询本方法指定的记录；涉及gsa_collection_job表。
     * SQL定义见 GsaCollectionMapper.xml 中同名语句。
     */
    GsaCollectionJob latest();

    /**
     * 写入gsa_collection_job。
     * SQL定义见 GsaCollectionMapper.xml 中同名语句。
     */
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int create(GsaCollectionJob job);

    /**
     * 更新本方法指定的记录；涉及gsa_collection_job表。
     * SQL定义见 GsaCollectionMapper.xml 中同名语句。
     */
    int resume(long id);

    /**
     * 更新本方法指定的记录；涉及gsa_collection_job表。
     * SQL定义见 GsaCollectionMapper.xml 中同名语句。
     */
    int phase(@Param("id") long id, @Param("phase") String phase);

    /**
     * 更新本方法指定的记录；涉及gsa_collection_job表。
     * SQL定义见 GsaCollectionMapper.xml 中同名语句。
     */
    int advance(@Param("id") long id, @Param("next") long next, @Param("size") int size);

    /**
     * 更新本方法指定的记录；涉及gsa_collection_job表。
     * SQL定义见 GsaCollectionMapper.xml 中同名语句。
     */
    int progress(@Param("id") long id, @Param("size") int size);

    /**
     * 更新本方法指定的记录；涉及gsa_collection_job表。
     * SQL定义见 GsaCollectionMapper.xml 中同名语句。
     */
    int finish(@Param("id") long id, @Param("status") String status, @Param("error") String error);

    /**
     * 更新本方法指定的记录；涉及gsa_publication表。
     * SQL定义见 GsaCollectionMapper.xml 中同名语句。
     */
    int retryFailed();

    /**
     * 更新本方法指定的记录；涉及gsa_publication表。
     * SQL定义见 GsaCollectionMapper.xml 中同名语句。
     */
    int resetEnrichment();

    /**
     * 查询本方法指定的记录；涉及gsa_publication表。
     * SQL定义见 GsaCollectionMapper.xml 中同名语句。
     */
    List<Map<String, Object>> counts();

    /**
     * 查询本方法指定的记录；涉及gsa_publication表。
     * SQL定义见 GsaCollectionMapper.xml 中同名语句。
     */
    List<GsaEnrichmentTask> pending(int limit);

    /**
     * 补全批次提交前锁定并读取最新JSON，避免合并时覆盖关联采集刚补充的基金。
     */
    List<GsaEnrichmentTask> lockMetadata(@Param("ids") List<Long> ids);

    /**
     * 更新本方法指定的记录；涉及gsa_publication表。
     * SQL定义见 GsaCollectionMapper.xml 中同名语句。
     */
    int markRelated(@Param("id") long id, @Param("grants") String grants, @Param("banks") String banks);

    /**
     * 写入本方法指定的记录；涉及gsa_publication表。唯一键冲突按SQL合并已有字段。
     * SQL定义见 GsaCollectionMapper.xml 中同名语句。
     */
    int upsertSeeds(@Param("rows") List<GsaPublicationSeed> rows);

    /**
     * 更新本方法指定的记录；涉及gsa_publication表。
     * SQL定义见 GsaCollectionMapper.xml 中同名语句。
     */
    int updateMetadata(@Param("rows") List<GsaEnrichmentWrite> rows);

    /**
     * 查询本方法指定的记录；涉及gsa_publication表。
     * SQL定义见 GsaCollectionMapper.xml 中同名语句。
     */
    List<GsaEnrichmentTask> existing(@Param("rows") List<GsaPublicationSeed> rows);
}

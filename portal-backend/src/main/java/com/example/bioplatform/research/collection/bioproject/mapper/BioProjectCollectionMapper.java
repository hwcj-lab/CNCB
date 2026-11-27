package com.example.bioplatform.research.collection.bioproject.mapper;

import com.example.bioplatform.research.collection.bioproject.dto.BioProjectEnrichmentTask;
import com.example.bioplatform.research.collection.bioproject.dto.BioProjectEnrichmentWrite;
import com.example.bioplatform.research.collection.bioproject.dto.BioProjectPublicationSeed;
import com.example.bioplatform.research.collection.bioproject.vo.BioProjectCollectionJob;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;


/**
 * BIOPROJECT专用持久化SQL：任务断点、列表UPSERT、补全批量写入及来源适配读取。
 */
@Mapper
public interface BioProjectCollectionMapper {

    /**
     * 更新本方法指定的记录；涉及bioproject_collection_job表。
     * SQL定义见 BioProjectCollectionMapper.xml 中同名语句。
     */
    int recover();


    /**
     * 查询本方法指定的记录；涉及bioproject_collection_job表。
     * SQL定义见 BioProjectCollectionMapper.xml 中同名语句。
     */
    BioProjectCollectionJob resumable();


    /**
     * 查询本方法指定的记录；涉及bioproject_collection_job表。
     * SQL定义见 BioProjectCollectionMapper.xml 中同名语句。
     */
    BioProjectCollectionJob latest();


    /**
     * 写入bioproject_collection_job。
     * SQL定义见 BioProjectCollectionMapper.xml 中同名语句。
     */
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int create(BioProjectCollectionJob job);


    /**
     * 更新本方法指定的记录；涉及bioproject_collection_job表。
     * SQL定义见 BioProjectCollectionMapper.xml 中同名语句。
     */
    int resume(long id);


    /**
     * 更新本方法指定的记录；涉及bioproject_collection_job表。
     * SQL定义见 BioProjectCollectionMapper.xml 中同名语句。
     */
    int phase(@Param("id") long id, @Param("phase") String phase);


    /**
     * 更新本方法指定的记录；涉及bioproject_collection_job表。
     * SQL定义见 BioProjectCollectionMapper.xml 中同名语句。
     */
    int advance(@Param("id") long id, @Param("next") long next, @Param("size") int size);


    /**
     * 更新本方法指定的记录；涉及bioproject_collection_job表。
     * SQL定义见 BioProjectCollectionMapper.xml 中同名语句。
     */
    int progress(@Param("id") long id, @Param("size") int size);


    /**
     * 更新本方法指定的记录；涉及bioproject_collection_job表。
     * SQL定义见 BioProjectCollectionMapper.xml 中同名语句。
     */
    int finish(@Param("id") long id, @Param("status") String status, @Param("error") String error);


    /**
     * 更新本方法指定的记录；涉及bioproject_publication表。
     * SQL定义见 BioProjectCollectionMapper.xml 中同名语句。
     */
    int retryFailed();


    /**
     * 更新本方法指定的记录；涉及bioproject_publication表。
     * SQL定义见 BioProjectCollectionMapper.xml 中同名语句。
     */
    int resetEnrichment();


    /**
     * 查询本方法指定的记录；涉及bioproject_publication表。
     * SQL定义见 BioProjectCollectionMapper.xml 中同名语句。
     */
    List<Map<String, Object>> counts();


    /**
     * 查询本方法指定的记录；涉及bioproject_publication表。
     * SQL定义见 BioProjectCollectionMapper.xml 中同名语句。
     */
    List<BioProjectEnrichmentTask> pending(int limit);

    /**
     * 补全批次提交前锁定并读取最新JSON，避免合并时覆盖关联采集刚补充的基金。
     */

    List<BioProjectEnrichmentTask> lockMetadata(@Param("ids") List<Long> ids);


    /**
     * 更新本方法指定的记录；涉及bioproject_publication表。
     * SQL定义见 BioProjectCollectionMapper.xml 中同名语句。
     */
    int markRelated(@Param("id") long id, @Param("grants") String grants, @Param("banks") String banks);

    /**
     * 写入本方法指定的记录；涉及bioproject_publication表。唯一键冲突按SQL合并已有字段。
     * SQL定义见 BioProjectCollectionMapper.xml 中同名语句。
     */
    int upsertSeeds(@Param("rows") List<BioProjectPublicationSeed> rows);

    /**
     * 更新本方法指定的记录；涉及bioproject_publication表。
     * SQL定义见 BioProjectCollectionMapper.xml 中同名语句。
     */
    int updateMetadata(@Param("rows") List<BioProjectEnrichmentWrite> rows);

    /**
     * 查询本方法指定的记录；涉及bioproject_publication表。
     * SQL定义见 BioProjectCollectionMapper.xml 中同名语句。
     */
    List<BioProjectEnrichmentTask> existing(@Param("rows") List<BioProjectPublicationSeed> rows);
}

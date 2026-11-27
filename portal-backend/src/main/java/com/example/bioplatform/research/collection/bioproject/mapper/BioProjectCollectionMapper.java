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

    int recover();


    BioProjectCollectionJob resumable();


    BioProjectCollectionJob latest();


    @Options(useGeneratedKeys = true, keyProperty = "id")
    int create(BioProjectCollectionJob job);


    int resume(long id);


    int phase(@Param("id") long id, @Param("phase") String phase);


    int advance(@Param("id") long id, @Param("next") long next, @Param("size") int size);


    int progress(@Param("id") long id, @Param("size") int size);


    int finish(@Param("id") long id, @Param("status") String status, @Param("error") String error);


    int retryFailed();


    int resetEnrichment();


    List<Map<String, Object>> counts();


    List<BioProjectEnrichmentTask> pending(int limit);

    /**
     * 补全批次提交前锁定并读取最新JSON，避免合并时覆盖关联采集刚补充的基金。
     */

    List<BioProjectEnrichmentTask> lockMetadata(@Param("ids") List<Long> ids);


    int markRelated(@Param("id") long id, @Param("grants") String grants, @Param("banks") String banks);

    int upsertSeeds(@Param("rows") List<BioProjectPublicationSeed> rows);

    int updateMetadata(@Param("rows") List<BioProjectEnrichmentWrite> rows);

    List<BioProjectEnrichmentTask> existing(@Param("rows") List<BioProjectPublicationSeed> rows);
}

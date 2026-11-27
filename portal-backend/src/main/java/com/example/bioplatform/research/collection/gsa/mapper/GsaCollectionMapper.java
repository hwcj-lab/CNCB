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
    int recover();

    GsaCollectionJob resumable();

    GsaCollectionJob latest();

    @Options(useGeneratedKeys = true, keyProperty = "id")
    int create(GsaCollectionJob job);

    int resume(long id);

    int phase(@Param("id") long id, @Param("phase") String phase);

    int advance(@Param("id") long id, @Param("next") long next, @Param("size") int size);

    int progress(@Param("id") long id, @Param("size") int size);

    int finish(@Param("id") long id, @Param("status") String status, @Param("error") String error);

    int retryFailed();

    int resetEnrichment();

    List<Map<String, Object>> counts();

    List<GsaEnrichmentTask> pending(int limit);

    /**
     * 补全批次提交前锁定并读取最新JSON，避免合并时覆盖关联采集刚补充的基金。
     */
    List<GsaEnrichmentTask> lockMetadata(@Param("ids") List<Long> ids);

    int markRelated(@Param("id") long id, @Param("grants") String grants, @Param("banks") String banks);

    int upsertSeeds(@Param("rows") List<GsaPublicationSeed> rows);

    int updateMetadata(@Param("rows") List<GsaEnrichmentWrite> rows);

    List<GsaEnrichmentTask> existing(@Param("rows") List<GsaPublicationSeed> rows);
}

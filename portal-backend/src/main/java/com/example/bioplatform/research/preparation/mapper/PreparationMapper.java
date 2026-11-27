package com.example.bioplatform.research.preparation.mapper;

import com.example.bioplatform.research.preparation.model.*;
import org.apache.ibatis.annotations.*;

import java.util.*;

@Mapper
public interface PreparationMapper {
    PreparationJob latestJob();

    Long readyRevision();

    List<String> coveredSources();

    int createJob(PreparationJob job);

    int updateJob(PreparationJob job);

    List<PreparedDocument> activeDocuments(@Param("source") String source, @Param("hashes") List<String> hashes, @Param("ids") List<Long> ids);

    int touch(@Param("id") long id, @Param("revision") long revision, @Param("sourceRowId") long sourceRowId);

    int close(@Param("id") long id, @Param("revision") long revision);

    int insertDocument(PreparedDocument row);

    int insertMembers(@Param("table") String table, @Param("rows") List<PreparedMember> rows);

    int expireMissing(@Param("source") String source, @Param("revision") long revision);

    int expireDisabled(@Param("sources") List<String> sources, @Param("revision") long revision);

    int prepareIdentities(@Param("revision") long revision);

    int deleteRevisionMembers(@Param("table") String table, @Param("revision") long revision);

    int deleteRevisionDocuments(@Param("revision") long revision);

    int reopenRevision(@Param("revision") long revision);

    int deleteIdentities(@Param("revision") long revision);

    long invalidMembers(@Param("table") String table);

    long invalidRevisionMembers(@Param("table") String table, @Param("revision") long revision);
    java.time.LocalDateTime databaseTime();
    PreparationSourceCursor sourceCursor(@Param("jobId") long jobId, @Param("source") String source);
    List<PreparationSourceCursor> sourceCursors(@Param("jobId") long jobId);
    PreparationSourceCursor successfulCursor(@Param("source") String source);
    int insertCursor(PreparationSourceCursor cursor);
    int updateCursor(PreparationSourceCursor cursor);
    int deleteDocumentMembers(@Param("table") String table, @Param("id") long id);
    int deleteDocument(@Param("id") long id);
    long unmappedActive(@Param("source") String source);
}

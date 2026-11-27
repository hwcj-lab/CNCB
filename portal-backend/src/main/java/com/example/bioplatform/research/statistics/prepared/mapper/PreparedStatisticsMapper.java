package com.example.bioplatform.research.statistics.prepared.mapper;

import com.example.bioplatform.research.statistics.prepared.model.PreparedScope;
import com.example.bioplatform.research.statistics.prepared.model.PreparedSnapshotConfig;
import com.example.bioplatform.research.statistics.vo.ResearchSnapshotDetail;
import com.example.bioplatform.research.statistics.vo.ResearchSnapshotDimension;
import com.example.bioplatform.research.statistics.vo.ResearchSnapshotPublication;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PreparedStatisticsMapper {
    int saveConfig(PreparedSnapshotConfig config);

    PreparedSnapshotConfig config(@Param("id") long id);

    List<ResearchSnapshotDetail> versions();

    ResearchSnapshotDetail latestSuccess();

    List<ResearchSnapshotDimension> rank(@Param("q") PreparedScope scope, @Param("table") String table);

    long count(@Param("q") PreparedScope scope);

    long resources(@Param("q") PreparedScope scope);

    long coverage(@Param("q") PreparedScope scope, @Param("table") String table);

    int insertDimension(@Param("snapshotId") long snapshotId, @Param("row") ResearchSnapshotDimension row);

    int insertDimensions(@Param("snapshotId") long snapshotId, @Param("rows") List<ResearchSnapshotDimension> rows);

    ResearchSnapshotDimension dimension(@Param("snapshotId") long snapshotId, @Param("id") long id);

    List<ResearchSnapshotPublication> publications(@Param("q") PreparedScope scope, @Param("table") String table, @Param("offset") long offset, @Param("size") int size);

    int deleteConfig(@Param("id") long id);

    int lock();

    int unlock();
}

package com.example.bioplatform.research.preparation.mapper;

import com.example.bioplatform.research.preparation.model.PreparationJob;
import com.example.bioplatform.research.preparation.model.PreparationSourceCursor;
import com.example.bioplatform.research.preparation.model.PreparedDocument;
import com.example.bioplatform.research.preparation.model.PreparedMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 整理任务、来源游标、文献版本和五类成员关系的持久层；SQL集中在PreparationMapper.xml。
 */
@Mapper
public interface PreparationMapper {
    /**
     * 读取最近一轮整理任务，供运行状态判断、失败续传及中断恢复使用。
     * SQL定义见 PreparationMapper.xml 中同名语句。
     */
    PreparationJob latestJob();

    /**
     * 返回最近成功发布的整理修订号；没有成功整理时为空。
     */
    Long readyRevision();

    /**
     * 返回成功整理任务的来源列表，用于检查首次整理覆盖是否齐全。
     */
    List<String> coveredSources();

    /**
     * 写入本方法指定的记录；涉及research_preparation_job表。
     * SQL定义见 PreparationMapper.xml 中同名语句。
     */
    int createJob(PreparationJob job);

    /**
     * 更新本方法指定的记录；涉及research_preparation_job表。
     * SQL定义见 PreparationMapper.xml 中同名语句。
     */
    int updateJob(PreparationJob job);

    /**
     * 按来源业务摘要或扫描行ID批量读取当前有效版本，兼容原始业务键发生变化的记录。
     */
    List<PreparedDocument> activeDocuments(@Param("source") String source, @Param("hashes") List<String> hashes, @Param("ids") List<Long> ids);

    /**
     * 内容未变时仅更新本轮扫描标记和来源行ID，不重复写维度成员。
     */
    int touch(@Param("id") long id, @Param("revision") long revision, @Param("sourceRowId") long sourceRowId);

    /**
     * 关闭旧版本的有效区间；历史快照仍可读取失效边界之前的数据。
     */
    int close(@Param("id") long id, @Param("revision") long revision);

    /**
     * 写入本方法指定的记录；涉及research_prepared_document表。
     * SQL定义见 PreparationMapper.xml 中同名语句。
     */
    int insertDocument(PreparedDocument row);

    /**
     * 写入本方法指定的记录；涉及${table}表。动态表标识只能来自服务端白名单。
     * SQL定义见 PreparationMapper.xml 中同名语句。
     */
    int insertMembers(@Param("table") String table, @Param("rows") List<PreparedMember> rows);

    /**
     * 全量来源扫描结束后，关闭本轮没有再次出现的旧记录；增量扫描不能调用。
     */
    int expireMissing(@Param("source") String source, @Param("revision") long revision);

    /**
     * 全来源扫描结束后关闭已停用来源的当前版本，不物理删除历史版本。
     */
    int expireDisabled(@Param("sources") List<String> sources, @Param("revision") long revision);

    /**
     * 按指定修订内全部有效文献的DOI分组；只有唯一非空PMID时才建立桥接，冲突和无PMID保留为空。
     */
    int prepareIdentities(@Param("revision") long revision);

    /**
     * 只删除指定未发布修订对应的维度成员；JOIN document用于限定修订，不删除document本身。
     * SQL定义见 PreparationMapper.xml 中同名语句。
     */
    int deleteRevisionMembers(@Param("table") String table, @Param("revision") long revision);

    /**
     * 丢弃任务时删除该未发布修订新建的document记录；成员应先清理。
     * SQL定义见 PreparationMapper.xml 中同名语句。
     */
    int deleteRevisionDocuments(@Param("revision") long revision);

    /**
     * 丢弃未发布修订时恢复被该修订关闭的旧版本。
     */
    int reopenRevision(@Param("revision") long revision);

    /**
     * 删除符合条件的目标记录；涉及research_prepared_identity表，关联表仅用于筛选，不表示同时删除。
     * SQL定义见 PreparationMapper.xml 中同名语句。
     */
    int deleteIdentities(@Param("revision") long revision);

    /**
     * 检查成员是否缺少document，或成员来源/文献键与document不一致；正常结果应为0。
     * SQL定义见 PreparationMapper.xml 中同名语句。
     */
    long invalidMembers(@Param("table") String table);

    /**
     * 仅校验本修订涉及的成员与document是否一致，避免增量收尾扫描全部历史成员。
     * SQL定义见 PreparationMapper.xml 中同名语句。
     */
    long invalidRevisionMembers(@Param("table") String table, @Param("revision") long revision);

    /**
     * 读取数据库当前时间，避免应用机时钟与来源update_time不一致影响增量扫描边界。
     * SQL定义见 PreparationMapper.xml 中同名语句。
     */
    java.time.LocalDateTime databaseTime();

    /**
     * 查询本方法指定的记录；涉及research_preparation_source_cursor表。
     * SQL定义见 PreparationMapper.xml 中同名语句。
     */
    PreparationSourceCursor sourceCursor(@Param("jobId") long jobId, @Param("source") String source);

    /**
     * 查询本方法指定的记录；涉及research_preparation_source_cursor表。
     * SQL定义见 PreparationMapper.xml 中同名语句。
     */
    List<PreparationSourceCursor> sourceCursors(@Param("jobId") long jobId);

    /**
     * 仅从成功任务且来源已完成的扫描计划中选择增量基线，失败和停止任务不推进基线。
     */
    PreparationSourceCursor successfulCursor(@Param("source") String source);

    /**
     * 写入本方法指定的记录；涉及research_preparation_source_cursor表。
     * SQL定义见 PreparationMapper.xml 中同名语句。
     */
    int insertCursor(PreparationSourceCursor cursor);

    /**
     * 与本批文献成员写入放在同一事务提交，失败时数据和游标一起回滚。
     */
    int updateCursor(PreparationSourceCursor cursor);

    /**
     * 删除符合条件的目标记录；涉及${table}表，关联表仅用于筛选，不表示同时删除。动态表标识只能来自服务端白名单。
     * SQL定义见 PreparationMapper.xml 中同名语句。
     */
    int deleteDocumentMembers(@Param("table") String table, @Param("id") long id);

    /**
     * 删除符合条件的目标记录；涉及research_prepared_document表，关联表仅用于筛选，不表示同时删除。
     * SQL定义见 PreparationMapper.xml 中同名语句。
     */
    int deleteDocument(@Param("id") long id);

    /**
     * 统计当前有效记录中缺少来源扫描行ID的数量；存在此类旧数据时增量模式需先建立全量基线。
     * SQL定义见 PreparationMapper.xml 中同名语句。
     */
    long unmappedActive(@Param("source") String source);
}

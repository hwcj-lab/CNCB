package com.example.bioplatform.catalog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.bioplatform.catalog.entity.*;
import com.example.bioplatform.catalog.vo.DatabaseTypeRelation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 数据库目录及其数据类型、在线工具关系的持久层接口。
 */
@Mapper
public interface DatabaseMapper extends BaseMapper<DatabaseResource> {
    /**
     * 按类型和关键词查询数据库，并装配基础关联信息。
     */
    List<DatabaseResource> findResources(@Param("type") String type, @Param("keyword") String keyword);

    /**
     * 按 CNCB 数据库业务标识批量查询，避免逐条检索。
     */
    List<DatabaseResource> findResourcesByDbIds(@Param("type") String type,
                                                @Param("dbIds") List<String> dbIds);

    /**
     * 查询一个数据库的全部数据类型标签。
     */
    List<DataType> findTypes(@Param("id") Long id);

    /**
     * 查询一个数据库的全部关联工具。
     */
    List<OnlineTool> findTools(@Param("id") Long id);

    /**
     * 批量查询数据库与数据类型关系，用于内存分组。
     */
    List<DatabaseTypeRelation> findTypeRelationsByDatabaseIds(@Param("databaseIds") List<Long> databaseIds);

    /**
     * 批量查询多个数据库关联的工具，避免 N+1 查询。
     */
    List<OnlineTool> findToolsByDatabaseIds(@Param("databaseIds") List<Long> databaseIds);
}

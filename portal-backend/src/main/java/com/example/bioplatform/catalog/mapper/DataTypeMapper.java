package com.example.bioplatform.catalog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.bioplatform.catalog.entity.DataType;
import org.apache.ibatis.annotations.Mapper;

/** 数据类型基础 CRUD 持久层。 */
@Mapper
public interface DataTypeMapper extends BaseMapper<DataType> {
}

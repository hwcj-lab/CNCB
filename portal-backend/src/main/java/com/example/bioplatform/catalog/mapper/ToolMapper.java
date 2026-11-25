package com.example.bioplatform.catalog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.bioplatform.catalog.entity.OnlineTool;
import org.apache.ibatis.annotations.Mapper;

/** 在线工具目录的基础 CRUD 持久层。 */
@Mapper
public interface ToolMapper extends BaseMapper<OnlineTool> {
}

package com.supos.camunda.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supos.camunda.po.ProcessDefinitionPo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProcessDefinitionMapper extends BaseMapper<ProcessDefinitionPo> {
}

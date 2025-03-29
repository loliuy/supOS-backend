package com.supos.adpter.nodered.dao.mapper;

import com.supos.adpter.nodered.dao.po.NodeFlowModelPO;
import com.supos.adpter.nodered.dao.po.NodeFlowPO;
import org.apache.ibatis.annotations.*;

import java.util.Collection;
import java.util.List;

@Mapper
public interface NodeFlowModelMapper {

    @Select("<script>" +
            "select distinct parent_id from supos_node_flow_models where topic in " +
            "<foreach collection=\"topics\" item=\"topic\" open=\"(\" close=\")\" separator=\",\">" +
            "        #{topic} " +
            "</foreach>" +
            "</script>")
    List<Long> selectByTopics(@Param("topics") Collection<String> topics);

    @Select("select parent_id from supos_node_flow_models where topic=#{topic}")
    List<Long> queryByTopic(@Param("topic") String topic);

    @Insert("<script>" +
            "insert into supos_node_flow_models " +
            "(parent_id, topic) values " +
            "<foreach collection=\"flowModels\" item=\"fm\" separator=\",\">" +
            "      (#{fm.parentId}, #{fm.topic}) " +
            "</foreach>" +
            "</script>")
    int batchInsert(@Param("flowModels") Collection<NodeFlowModelPO> flowModels);

    @Delete("delete from supos_node_flow_models where parent_id=#{parentId} ")
    int deleteById(@Param("parentId") long parentId);

    @Delete("<script>" +
            "delete from supos_node_flow_models where topic in " +
            "<foreach collection=\"topics\" item=\"topic\" open=\"(\" close=\")\" separator=\",\">" +
            "        #{topic} " +
            "</foreach>" +
            "</script>")
    int deleteByTopics(@Param("topics") Collection<String> topics);
}

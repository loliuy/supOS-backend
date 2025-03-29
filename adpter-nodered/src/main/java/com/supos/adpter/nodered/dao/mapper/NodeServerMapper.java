package com.supos.adpter.nodered.dao.mapper;

import com.supos.adpter.nodered.dao.po.NodeServerPO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface NodeServerMapper {

    @Select("<script>" +
            "select * from supos_node_server where 1=1 " +
            "<if test='protocol != null'> and protocol_name=#{protocol} </if>" +
            "order by create_time desc " +
            "</script>")
    List<NodeServerPO> selectList(@Param("protocol") String protocol);

    @Insert("insert into supos_node_server " +
            "(id, server_name, protocol_name, config_json) values " +
            "(#{server.id}, #{server.serverName}, #{server.protocolName}, #{server.configJson})")
    int insert(@Param("server") NodeServerPO server);

    @Delete("delete from supos_node_server where id=#{id}")
    int deleteById(@Param("id") String id);

    @Select("<script>" +
            "select * from supos_node_server " +
            "where server_name=#{serverName} " +
            "<if test='protocol != null'> and protocol_name=#{protocol} </if>" +
            "</script>")
    NodeServerPO selectByName(@Param("serverName") String serverName, @Param("protocol") String protocol);

    @Select("select * from supos_node_server where id=#{id}")
    NodeServerPO selectById(@Param("id") String id);
}

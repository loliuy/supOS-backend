package com.supos.adpter.nodered.dao.mapper;

import com.supos.adpter.nodered.dao.po.IOTProtocolPO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface IOTProtocolMapper {

    @Insert("insert into supos_node_protocol " +
            "(name, description, client_config_json, server_config_json, server_conn, custom) " +
            " values " +
            "(#{name}, #{description}, #{clientConfigJson}, #{serverConfigJson}, #{serverConn}, #{custom})")
    int create(IOTProtocolPO protocol);

    @Select("select name from supos_node_protocol order by create_time desc")
    List<String> selectNames();

    @Select("select name, client_config_json, server_config_json, server_conn, custom from supos_node_protocol where name=#{name} order by create_time desc")
    IOTProtocolPO queryByName(@Param("name") String name);

    @Delete("delete from supos_node_protocol where name=#{name}")
    int deleteByName(@Param("name") String name);
}

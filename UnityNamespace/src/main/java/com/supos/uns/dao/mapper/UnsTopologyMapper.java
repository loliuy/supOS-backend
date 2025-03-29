package com.supos.uns.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supos.common.Constants;
import com.supos.uns.bo.PathTypeCount;
import com.supos.uns.bo.ProtocolCount;
import com.supos.uns.dao.po.UnsPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UnsTopologyMapper extends BaseMapper<UnsPo> {

    @Select("SELECT path_type as pathType, COUNT(*) as count FROM " + UnsPo.TABLE_NAME + " where status=1 and (path_type=0 or path_type=2) and (data_type !=" + Constants.ALARM_RULE_TYPE+" or data_type is null) GROUP BY path_type order by path_type ")
    List<PathTypeCount> selectGroupByPathType();

    @Select("SELECT protocol_type as protocol, COUNT(*) as count FROM " + UnsPo.TABLE_NAME +
            " where path_type=2 and protocol_type is not null GROUP BY protocol_type order by protocol_type ")
    List<ProtocolCount> selectGroupByProtocolType();
}

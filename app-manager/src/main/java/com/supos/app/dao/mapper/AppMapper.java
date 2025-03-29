package com.supos.app.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supos.app.dao.po.AppPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AppMapper extends BaseMapper<AppPO> {

    int insertApp(AppPO app);

    List<AppPO> selectApps(@Param("name") String name);

    int deleteApp(@Param("name") String name);

}

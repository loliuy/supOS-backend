package com.supos.uns.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supos.common.enums.SysModuleEnum;
import com.supos.common.vo.UserInfoVo;
import com.supos.uns.dao.po.TodoPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TodoMapper extends BaseMapper<TodoPo> {

    @Update("update " + TodoPo.TABLE_NAME + " set status = #{status} ,handler_user_id = #{userId}, " +
            "handler_username = #{username} , handler_time = now() " +
            "where module_code = #{moduleCode} and business_id = #{businessId} and link = #{link}")
    int updateTodoStatus(@Param("moduleCode") String moduleCode, @Param("businessId")String businessId,
                         @Param("status")int status, @Param("username")String username,
                         @Param("userId")String userId,@Param("link")String link);



}

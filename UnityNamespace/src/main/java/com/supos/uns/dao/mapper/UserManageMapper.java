package com.supos.uns.dao.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supos.common.dto.auth.RoleDto;
import com.supos.common.vo.UserManageVo;
import com.supos.uns.dao.po.UnsPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @author xinwangji@supos.com
 * @date 2024/11/27 20:59
 * @description
 */
@DS("keycloak")
@Mapper
public interface UserManageMapper extends BaseMapper {

    IPage<UserManageVo> userManageList(Page<?> page,@Param("realm") String realm);

    /**
     * 通过用户名获取用户的角色列表
     * @param realm
     * @param userId
     * @return
     */
    List<RoleDto> roleListByUserId(@Param("realm") String realm, @Param("userId") String userId);

    List<UserManageVo> userList();

    @Select("select ue.id,ue.username AS preferredUsername from user_entity ue where ue.id =#{userId}")
    UserManageVo getById(@Param("userId") String userId);



    @Select("<script> select ue.id,ue.username AS preferredUsername from user_entity ue " +
            " where ue.id in " +
            "  <foreach collection=\"ids\" item=\"id\" index=\"index\" open=\"(\" close=\")\" separator=\",\"> " +
            "      #{id}" +
            "  </foreach> " +
            "</script>")
    List<UserManageVo> listUserById(@Param("ids") List<String> ids);
}


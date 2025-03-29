package com.supos.uns.service;

import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supos.common.config.OAuthKeyCloakConfig;
import com.supos.common.dto.PageResultDTO;
import com.supos.common.dto.PaginationDTO;
import com.supos.common.dto.auth.*;
import com.supos.common.enums.RoleEnum;
import com.supos.common.exception.vo.ResultVO;
import com.supos.common.utils.I18nUtils;
import com.supos.common.utils.KeycloakUtil;
import com.supos.common.utils.UserContext;
import com.supos.common.vo.UserInfoVo;
import com.supos.common.vo.UserManageVo;
import com.supos.gateway.dao.mapper.AuthMapper;
import com.supos.uns.dao.mapper.UserManageMapper;
import com.supos.uns.vo.RoleVo;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author xinwangji@supos.com
 * @date 2024/11/27 20:54
 * @description
 */
@Service
public class UserManageService {

    @Resource
    private UserManageMapper userMapper;
    @Resource
    private KeycloakUtil keycloakUtil;
    @Resource
    private OAuthKeyCloakConfig keyCloakConfig;
    @Resource
    private AuthMapper authMapper;
    @Resource
    private TimedCache<String, UserInfoVo> userInfoCache;


    public PageResultDTO<UserManageVo> userManageList(PaginationDTO params){
        Page<UserManageVo> page = new Page<>(params.getPageNo(), params.getPageSize());
        IPage<UserManageVo> iPage = userMapper.userManageList(page,keyCloakConfig.getRealm());
        iPage.getRecords().forEach(user ->{
            List<RoleDto>  roleList = userMapper.roleListByUserId(keyCloakConfig.getRealm(),user.getId());
            if (CollectionUtil.isNotEmpty(roleList)) {
                roleList = roleList.stream().filter(r -> {
                    RoleEnum roleEnum = RoleEnum.parse(r.getRoleId());
                    return ObjectUtil.isNotNull(roleEnum);
                }).peek(r ->{
                    RoleEnum roleEnum = RoleEnum.parse(r.getRoleId());
                    r.setRoleName(I18nUtils.getMessage(roleEnum.getI18nCode()));
                }).collect(Collectors.toList());
                user.setRoleList(roleList);
            }
        });
        PageResultDTO.PageResultDTOBuilder<UserManageVo> pageBuilder = PageResultDTO.<UserManageVo>builder()
                .total(iPage.getTotal()).pageNo(params.getPageNo()).pageSize(params.getPageSize());
        return pageBuilder.code(0).data(iPage.getRecords()).build();
    }

    public ResultVO delete(String id){
        return new ResultVO(keycloakUtil.deleteUser(id));
    }

    public ResultVO resetPwd(String userId,String password){
        return new ResultVO(keycloakUtil.resetPwd(userId,password));
    }

    public ResultVO userResetPwd(ResetPasswordDto resetPasswordDto){
        AccessTokenDto accessTokenDto = keycloakUtil.login(resetPasswordDto.getUsername(),resetPasswordDto.getPassword());
        if (null == accessTokenDto){
            return ResultVO.fail(I18nUtils.getMessage("user.login.password.error"));
        }
        return new ResultVO(keycloakUtil.resetPwd(resetPasswordDto.getUserId(),resetPasswordDto.getNewPassword()));
    }

    public ResultVO updateUser(UpdateUserDto updateUserDto){
        JSONObject params = new JSONObject();
        if (ObjectUtil.isNotNull(updateUserDto.getEnabled())){
            params.put("enabled",updateUserDto.getEnabled());
        }
        if (ObjectUtil.isNotNull(updateUserDto.getFirstName())){
            params.put("firstName",updateUserDto.getFirstName());
        }
        if (ObjectUtil.isNotNull(updateUserDto.getEmail())){
            params.put("email",updateUserDto.getEmail());
        }
        boolean isUpdate = keycloakUtil.updateUser(updateUserDto.getUserId(),params);
        if (!isUpdate){
            return ResultVO.fail(I18nUtils.getMessage("user.update.failed"));
        }

        List<RoleDto> roleList = updateUserDto.getRoleList();
        List<RoleDto> currentRoles = userMapper.roleListByUserId(keyCloakConfig.getRealm(),updateUserDto.getUserId());
        if (CollectionUtil.isNotEmpty(currentRoles)){
            //先删除原有角色
            UpdateRoleDto updateRole = new UpdateRoleDto();
            updateRole.setUserId(updateUserDto.getUserId());
            updateRole.setRoleList(currentRoles);
            updateRole.setType(2);
            setRole(updateRole);
        }

        if (CollectionUtil.isNotEmpty(roleList)){
            //设置新的角色
            UpdateRoleDto updateRole = new UpdateRoleDto();
            updateRole.setUserId(updateUserDto.getUserId());
            updateRole.setRoleList(roleList);
            updateRole.setType(1);
            setRole(updateRole);
        }
        return ResultVO.success("ok");
    }

    public ResultVO setRole(UpdateRoleDto updateRoleDto){
        JSONArray array = new JSONArray();
        if (CollectionUtil.isNotEmpty(updateRoleDto.getRoleList())){
            for (RoleDto roleDto : updateRoleDto.getRoleList()) {
                RoleEnum roleEnum = RoleEnum.parse(roleDto.getRoleId());
                if (null == roleEnum){
                    continue;
                }
                JSONObject role = new JSONObject();
                role.put("id",roleDto.getRoleId());
                role.put("name", roleEnum.getName());
                array.add(role);
            }
        }
        return new ResultVO(keycloakUtil.setRole(updateRoleDto.getUserId(),updateRoleDto.getType(),array));
    }

    public ResultVO createUser(AddUserDto addUserDto){
        KeycloakCreateUserDto createUserDto = BeanUtil.copyProperties(addUserDto, KeycloakCreateUserDto.class);
        String userId = keycloakUtil.createUser(JSONObject.toJSONString(createUserDto));
        addUserDto.setId(userId);
        boolean isReset = keycloakUtil.resetPwd(userId,addUserDto.getPassword());
        if (!isReset){
            return ResultVO.fail(I18nUtils.getMessage("user.set.password.failed"));
        }
        UpdateRoleDto updateRoleDto = new UpdateRoleDto();
        updateRoleDto.setUserId(userId);
        updateRoleDto.setType(1);
        updateRoleDto.setRoleList(addUserDto.getRoleList());
        return setRole(updateRoleDto);
    }

    public ResultVO<List<RoleVo>> getRoleList(){
        List<ResourceDto> defResources = new ArrayList<>();
        List<String> defPolicyIds = authMapper.getPolicyIdsByRoleId(authMapper.getDefaultRoleId());
        if (CollectionUtils.isNotEmpty(defPolicyIds)){
            defResources = authMapper.getResourceListByPolicyIds(defPolicyIds);
        }

        List<ResourceDto> finalDefResources = defResources;
        List<RoleVo> list = Arrays.stream(RoleEnum.values()).map(r ->{
            RoleVo role = new RoleVo();
            role.setRoleId(r.getId());
            role.setRoleName(I18nUtils.getMessage(r.getI18nCode()));
            List<String> policyIds = authMapper.getPolicyIdsByRoleId(r.getId());
            if (CollectionUtils.isEmpty(policyIds)){
                role.setResourceList(finalDefResources);
            } else {
                List<ResourceDto> roleResources = authMapper.getResourceListByPolicyIds(policyIds);
                role.setResourceList(unionResources(finalDefResources,roleResources));
            }
            return role;
        }).collect(Collectors.toList());
        return ResultVO.successWithData(list);
    }

    public ResultVO setTipsEnable(int tipsEnable){
        UserInfoVo userInfoVo = UserContext.get();
        JSONObject params = new JSONObject();
        params.put("firstTimeLogin",userInfoVo.getFirstTimeLogin());
        params.put("tipsEnable",tipsEnable);
        JSONObject attributes = new JSONObject();
        attributes.put("attributes",params);
        boolean flag = keycloakUtil.updateUser(userInfoVo.getSub(), attributes);
        if (!flag){
            ResultVO.fail("set user tips enable failed");
        }
        userInfoVo.setTipsEnable(tipsEnable);
        userInfoCache.put(userInfoVo.getSub(),userInfoVo);
        return ResultVO.success("ok");
    }

    private static List<ResourceDto> unionResources(List<ResourceDto> defResources,List<ResourceDto> roleResources) {
        return new ArrayList<>(Stream.concat(defResources.stream(), roleResources.stream())
                .collect(Collectors.toMap(ResourceDto::getUri, dto -> dto, (existing, replacement) -> existing))
                .values());
    }

    public List<UserManageVo> getUserList(){
        return userMapper.userList();
    }
}

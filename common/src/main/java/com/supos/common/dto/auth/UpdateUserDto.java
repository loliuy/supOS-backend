package com.supos.common.dto.auth;

import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * @author xinwangji@supos.com
 * @date 2024/11/28 9:04
 * @description
 */
@Data
public class UpdateUserDto {

    /**
     * 用户ID
     */
    @NotEmpty(message = "userId can't be empty")
    private String userId;

    /**
     * 密码
     */
    private String password;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 名字
     */
    private String firstName;

    /**
     * 角色列表
     */
    private List<RoleDto> roleList;
}

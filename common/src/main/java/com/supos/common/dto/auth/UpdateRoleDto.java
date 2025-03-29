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
public class UpdateRoleDto {

    /**
     * 用户ID
     */
    @NotEmpty(message = "userId can't be empty")
    private String userId;

    /**
     * 操作类型
     * 1：设置
     * 2：取消
     */
    private Integer type;

    /**
     * 角色列表
     */
    private List<RoleDto> roleList;
}

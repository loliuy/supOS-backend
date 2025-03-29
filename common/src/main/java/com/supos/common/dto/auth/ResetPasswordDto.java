package com.supos.common.dto.auth;

import lombok.Data;

import jakarta.validation.constraints.NotEmpty;

@Data
public class ResetPasswordDto {

    /**
     * 用户ID
     */
    @NotEmpty(message = "userId can't be empty")
    private String userId;

    @NotEmpty(message = "username can't be empty")
    private String username;

    /**
     * 当前密码
     */
    @NotEmpty(message = "current password can't be empty")
    private String password;

    /**
     * 新密码
     */
    @NotEmpty(message = "password can't be empty")
    private String newPassword;

}

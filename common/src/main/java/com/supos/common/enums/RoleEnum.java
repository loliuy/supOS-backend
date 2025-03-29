package com.supos.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public enum RoleEnum {

    /**
     * 1, 待激活
     */
    SUPER_ADMIN("7ca9f922-0d35-44cf-8747-8dcfd5e66f8e","super-admin", "user.role.supAdmin","超级管理员"),
    ADMIN("a22ce15f-7bef-4e2e-9909-78f51b91c799", "admin","user.role.admin","管理员"),
    NORMAL_USER("71dd6dc2-6b12-4273-9ec0-b44b86e5b500", "normal-user","user.role.normalUser","普通用户"),
    ;

    /**
     * 角色ID
     */
    private String id;

    private String name;

    private String i18nCode;

    /**
     * 注释
     */
    private String comment;

    public static RoleEnum parse(String id) {
        for (RoleEnum each : values()) {
            if (id.equals(each.id)) {
                return each;
            }
        }
        return null;
    }

}


package com.supos.common.utils;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.supos.common.vo.UserInfoVo;

public class UserContext {

    private static final TransmittableThreadLocal<UserInfoVo> USER_HOLDER = new TransmittableThreadLocal<>();

    /**
     * 设置上下文用户信息
     *
     * @param user 用户信息
     */
    public static void set(UserInfoVo user) {
        USER_HOLDER.set(user);
    }

    /**
     * 获取上下文用户信息
     */
    public static UserInfoVo get() {
        return USER_HOLDER.get();
    }

    /**
     * 清除上下文用户信息
     */
    public static void clear() {
        USER_HOLDER.remove();
    }
}
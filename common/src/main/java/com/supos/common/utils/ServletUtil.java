package com.supos.common.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;

public class ServletUtil {

    public static Cookie getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        Cookie cookie = ArrayUtils.isEmpty(cookies) ? null : Arrays.stream(cookies).filter(c ->  c.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
        return cookie;
    }
}

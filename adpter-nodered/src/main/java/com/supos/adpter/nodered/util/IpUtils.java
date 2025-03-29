package com.supos.adpter.nodered.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IpUtils {

    /**
     * 判断字符串是否包含ip
     * @param str
     * @return
     */
    public static boolean containsIP(String str) {

        String regex = "\\b([01]?[0-9][0-9]?|2[0-4][0-9]|25[0-5])\\.(?!$)\\b";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(str);

        return matcher.find();
    }

    public static void main(String[] args) {
        System.out.println(containsIP("192.168.12.110"));
    }
}

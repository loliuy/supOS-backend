package com.supos.app.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegUtils {

    public static boolean validate(String regEx, String str) {
        Pattern p = Pattern.compile(regEx);
        Matcher matcher = p.matcher(str);
        return matcher.matches();
    }
}

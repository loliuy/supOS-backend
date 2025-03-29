package com.supos.common.utils;

import cn.hutool.core.lang.UUID;
import cn.hutool.extra.pinyin.PinyinUtil;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.supos.common.Constants;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * @author sunlifang
 * @version 1.0
 * @description: topic、path、alias通用处理工具
 * @date 2025/2/14 9:01
 */
public class PathUtil {
    static final Pattern TOPIC_PATTERN = Pattern.compile(Constants.TOPIC_REG);

    static final Pattern ALIAS_PATTERN = Pattern.compile(Constants.ALIAS_REG);

    /**
     * 校验topic格式
     *
     * @param topic
     * @return
     */
    public static boolean validTopicFormate(String topic, Integer dataType) {
        if (dataType != null && Constants.ALARM_RULE_TYPE == dataType) {
            return true;
        }

        if (StringUtils.startsWith(topic, "/") || StringUtils.contains(topic, "//")) {
            return false;
        }
        if (!StringUtils.contains(topic, "/")) {
            return TOPIC_PATTERN.matcher(topic).matches();
        }

        String[] names = StringUtils.split(topic, '/');
        for (String name : names) {
            if (!TOPIC_PATTERN.matcher(name).matches()) {
                return false;
            }
        }
        return true;
    }

    public static final boolean isAliasFormatOk(String alias) {
        if (ALIAS_PATTERN.matcher(alias).matches()) {
            return true;
        }
        return false;
    }

    /**
     * 获取path的名称
     *
     * @param path
     * @return
     */
    public static String getName(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }
        int ed = path.length() - 1;
        if (path.charAt(ed) == '/') {
            ed--;
        }
        int x = path.lastIndexOf('/', ed);
        path = path.substring(x + 1, ed + 1);
        return path;
    }

    public static String cleanPath(String path) {
        int ed = path.length() - 1;
        if (ed > 0 && path.charAt(ed) == '/') {
            path = path.substring(0, ed);
        }
        return path;
    }

    public static String generateFileAlias(String path) {
        String aliasPath = path;
        final int LEN = path.length();
        if (LEN > 20) {
            int startPo = StringUtils.lastOrdinalIndexOf(path, "/", 2);
            aliasPath = startPo >= 0 ? path.substring(startPo + 1) : path.substring(LEN - 20, LEN);
        }
        aliasPath = aliasPath.replace("/", "_");
        aliasPath = aliasPath.replace("-", "_");
        aliasPath = PinyinUtil.getPinyin(aliasPath, "");
        if (!Character.isLetter(aliasPath.charAt(0))) {
            aliasPath = "_" + aliasPath;
        }

        if (LEN < 20) {
            return aliasPath;
        }
        HashFunction hf = Hashing.md5();
        HashCode hc = hf.newHasher().putBytes(path.getBytes(StandardCharsets.UTF_8)).hash();
        aliasPath = aliasPath.substring(0, Math.min(4, aliasPath.length())) + "_" + hc;
        return aliasPath;
    }

    public static String generateAlias(String path, int pathType) {
        if (pathType == 2) {
            return generateFileAlias(path);
        }
        String aliasPath = "";
        if (pathType == 0) {
            // folder:folder1/、folder1/folder2/
            if (StringUtils.countMatches(path, '/') > 1) {
                // folder:folder1/folder2/
                aliasPath = StringUtils.substring(path, StringUtils.lastOrdinalIndexOf(path, "/", 2));
            } else {
                // folder:folder1/
                aliasPath = path;
            }
        } else {
            aliasPath = path;
        }
        aliasPath = aliasPath.replace("/", "_");
        aliasPath = aliasPath.replace("-", "_");
        aliasPath = PinyinUtil.getPinyin(aliasPath, "");
        if (aliasPath.length() > 20) {
            aliasPath = aliasPath.substring(0, 20);
        }
        String uuid = UUID.randomUUID().toString(true).substring(0, 20);
        return aliasPath + "_" + uuid;
    }

    public static String subParentPath(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        int ed = path.length() - 1;
        if (path.charAt(ed) == '/') {
            ed--;
        }
        int x = path.lastIndexOf('/', ed);
        return x > 0 ? path.substring(0, x + 1) : null;
    }
}

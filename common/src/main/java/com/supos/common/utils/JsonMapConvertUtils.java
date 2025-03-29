package com.supos.common.utils;

import com.alibaba.fastjson.JSON;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonMapConvertUtils {
    public static Map<String, Object> convertMap(Map<String, Object> source) {
        return convertMap(source, true);
    }

    public static Map<String, Object> convertMap(Map<String, Object> source, final boolean ignoreEmpty) {
        Map<String, Object> rs = new HashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String val;
            if (value == null || (val = value.toString().trim()).isEmpty()) {
                if (ignoreEmpty) {
                    continue;
                } else {
                    rs.put(key, value);
                    continue;
                }
            }

            if (val.startsWith("[") && val.endsWith("]")) {
                value = JSON.parseArray(val);
            } else if (value instanceof String) {
                value = val; // has trim
            }
            String[] slices = key.split("[.]");
            Map<String, Object> prev = rs;
            List prevList = null;
            int prevIndex = -1;
            String prevProp = null;
            for (String child : slices) {
                if (prevProp != null) {
                    if (prevIndex >= 0) {
                        Object oldV = prevList.get(prevIndex);
                        if (oldV == null) {
                            prevList.set(prevIndex, prev = new HashMap<>());
                        } else {
                            prev = (Map<String, Object>) oldV;
                        }
                    } else {
                        prev = (Map<String, Object>) prev.computeIfAbsent(prevProp, k -> new HashMap<>());
                    }
                }
                int qt = child.indexOf('[');
                int ed;
                String prop;
                boolean isArray = false;
                if (qt > 0 && (ed = child.indexOf(']', qt + 1)) > 0) {
                    isArray = true;
                    prop = child.substring(0, qt);
                    if (ed > qt + 1) {
                        Integer index = IntegerUtils.parseInt(child.substring(qt + 1, ed).trim());
                        prevIndex = index != null ? index.intValue() : 0;
                    } else {
                        prevIndex = 0;
                    }
                } else {
                    prop = child;
                }
                prevProp = prop;
                if (isArray) {
                    prevList = (List) prev.get(prop);
                    if (prevList == null) {
                        prev.put(prop, prevList = new ArrayList());
                    }
                    while (prevList.size() <= prevIndex) {
                        prevList.add(null);
                    }
                } else {
                    prevIndex = -1;
                }
            }
            if (prevList != null && prevIndex >= 0) {
                prevList.set(prevIndex, value);
            } else {
                prev.put(prevProp, value);
            }
        }
        return rs;
    }

}

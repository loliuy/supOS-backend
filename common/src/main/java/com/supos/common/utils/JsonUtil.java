package com.supos.common.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.gson.Gson;

import java.lang.reflect.Type;

public class JsonUtil {

    static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false); // 忽略未知字段
        objectMapper.configure(DeserializationFeature.READ_ENUMS_USING_TO_STRING, true);
        objectMapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true);
    }

    private static final Gson gson = new Gson();

    /**
     * 读取字段来做 json 序列化的方法，不会因为调用 getter 方法异常而报错
     *
     * @param obj
     * @return
     */
    public static String toJsonUseFields(Object obj) {
        return gson.toJson(obj);
    }

    public static String jackToJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    // 对象序列化为 JSON 字符串
    public static String toJson(Object obj) {
        return JSON.toJSONString(obj, JSONWriter.Feature.NotWriteDefaultValue, JSONWriter.Feature.WriteEnumUsingToString);
    }


    public static byte[] toJsonBytes(Object obj) {
        return JSON.toJSONBytes(obj, JSONWriter.Feature.NotWriteDefaultValue, JSONWriter.Feature.WriteEnumUsingToString);
    }

    // JSON 字符串反序列化为对象
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            return JSON.parseObject(json, clazz);
        }
    }

    public static Object fromJson(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        char ft = json.charAt(0), ed = json.charAt(json.length() - 1);
        if ((ft == '{' && ed != '}') || (ft == '[' && ed != ']')) {
            return null;
        } else if (ft != '{' && ft != '[') {
            if ("true".equalsIgnoreCase(json)) {
                return Boolean.TRUE;
            } else if ("false".equalsIgnoreCase(json)) {
                return Boolean.FALSE;
            }
        }
        try {
            return JSON.parse(json);
        } catch (Exception ex) {
            return gson.fromJson(json, Object.class);
        }
    }

    public static <T> T fromJson(String json, Type valueTypeRef) {
        try {
            return objectMapper.readValue(json, new TypeReference<T>() {
                @Override
                public Type getType() {
                    return valueTypeRef;
                }
            });
        } catch (JsonProcessingException e) {
            return JSON.parseObject(json, valueTypeRef);
        }
    }
}
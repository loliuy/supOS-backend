package com.supos.uns.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.supos.common.dto.FieldDefine;
import com.supos.uns.service.UnsQueryService;
import com.supos.uns.vo.TreeOuterStructureVo;

import java.util.ArrayList;
import java.util.List;

public class ParserUtil {

    public static List<TreeOuterStructureVo> parserJson2Tree(String jsonStr) {
        if (!JSON.isValid(jsonStr)) {
            return null;
        }

        // 创建 default 作为根节点
        TreeOuterStructureVo defaultNode = new TreeOuterStructureVo();
        defaultNode.setName("default");
        defaultNode.setDataPath("default");
        defaultNode.setFields(new ArrayList<>());
        defaultNode.setChildren(new ArrayList<>());

        Object json = JSON.parse(jsonStr);

        if (json instanceof JSONObject) {
            // 直接解析 JSON 对象
            parseJson((JSONObject) json, "default", defaultNode);
        } else if (json instanceof JSONArray) {
            JSONArray jsonArray = (JSONArray) json;
            if (!jsonArray.isEmpty() && jsonArray.get(0) instanceof JSONObject) {
                // 取数组第一个元素进行解析
                parseJson(jsonArray.getJSONObject(0), "default", defaultNode);
            }
        }

        // 结果只返回 default 这个根节点
        List<TreeOuterStructureVo> result = new ArrayList<>();
        result.add(defaultNode);
        return result;
    }

    private static void parseJson(JSONObject json, String parentPath, TreeOuterStructureVo parentNode) {
        List<TreeOuterStructureVo> children = new ArrayList<>();
        List<FieldDefine> fields = new ArrayList<>();

        for (String key : json.keySet()) {
            Object value = json.get(key);
            String currentPath = parentPath + "." + key;
            //如果节点是数组，取数组第一个元素，判断数组的元素类型是JSON对象
            if (value instanceof JSONArray valueArray && valueArray.size() > 0){
                if (valueArray.get(0) instanceof JSONObject){
                    value = ((JSONArray) value).getJSONObject(0);
                }
            }

            if (value instanceof JSONObject) {
                // 处理子对象
                TreeOuterStructureVo childNode = new TreeOuterStructureVo();
                childNode.setName(key);
                childNode.setDataPath(currentPath);
                childNode.setFields(new ArrayList<>());
                childNode.setChildren(new ArrayList<>());

                parseJson((JSONObject) value, currentPath, childNode);
                children.add(childNode);
            }  else {
                // 处理普通字段
                fields.add(new FieldDefine(key,UnsQueryService.guessType(value)));
            }
        }

        parentNode.setChildren(children);
        parentNode.setFields(fields);
    }
}

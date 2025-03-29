package com.supos.adpter.nodered.service.parse;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.supos.adpter.nodered.util.IDGenerator;
import com.supos.adpter.nodered.vo.BatchImportRequestVO;
import com.supos.common.dto.FieldDefine;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 解析关系模型对应的node-red模版文件
 */
@Service("relationalParser")
public class RelationalParser extends ParserApi {

    private String tplFile = "/relational.json.tpl";

    @Override
    public String readTplFromCache(BatchImportRequestVO.UnsVO uns) {
        return readFromTpl(tplFile);
    }

    @Override
    public void parse(String tpl, BatchImportRequestVO.UnsVO uns, JSONArray fullNodes) {

        int maxHeight = super.getMaxHeight(fullNodes);

        String injectNodeId = IDGenerator.generate();
        String selectModelNodeId = IDGenerator.generate();
        String mqttNodeId = IDGenerator.generate();
        String funcNodeId = IDGenerator.generate();
        // 替换节点id
        String jsonFlowStr = tpl.replaceAll("\\$id_inject", injectNodeId)
                .replaceAll("\\$id_model_selector", selectModelNodeId)
                .replaceAll("\\$id_func", funcNodeId)
                .replaceAll("\\$id_mqtt", mqttNodeId);
        // 替换模型topic
        jsonFlowStr = jsonFlowStr.replace("$model_topic", uns.getUnsTopic());
        // 替换mock数据
        jsonFlowStr = jsonFlowStr.replace("$payload", uns.getJsonExample());

        JSONArray jsonArr = JSON.parseArray(jsonFlowStr);
        // 设置节点高度
        for (int i = 0; i < jsonArr.size(); i++) {
            jsonArr.getJSONObject(i).put("y", maxHeight + intervalHeight);
        }
        fullNodes.addAll(jsonArr);
    }

    @Override
    public Map<String, ?> buildMapping(List<FieldDefine> fields, String topic, boolean isArray) {
        return null;
    }
}

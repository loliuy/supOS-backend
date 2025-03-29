package com.supos.adpter.nodered.service.parse;

import cn.hutool.core.lang.hash.Hash;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.supos.adpter.nodered.dao.mapper.NodeServerMapper;
import com.supos.adpter.nodered.util.IDGenerator;
import com.supos.adpter.nodered.vo.BatchImportRequestVO;
import com.supos.common.dto.FieldDefine;
import com.supos.common.dto.protocol.OpcUAConfigDTO;
import com.supos.common.enums.IOTProtocol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 解析时序模型对应的node-red模版文件
 */
@Service("opcuaBatchParser")
public class OpcUABatchParser extends ParserApi {

    private String tplFile = "/opcua-batch-subscribe.json.tpl";

    private int intervalHeight = 40;

    @Autowired
    private NodeServerMapper nodeServerMapper;

    @Override
    public String readTplFromCache(BatchImportRequestVO.UnsVO uns) {
        return readFromTpl(tplFile);
    }

    @Override
    public void parse(String tpl, BatchImportRequestVO.UnsVO uns, JSONArray fullNodes) {
        // 将opcua的配置由key-value转成对象，方便读取
        OpcUAConfigDTO opcuaConfig = JSON.parseObject(JSON.toJSONString(uns.getConfig()), OpcUAConfigDTO.class, Feature.TrimStringFieldValue);
        String serverConfigJson = JSON.toJSONString(opcuaConfig.getServer());
        String serverId = super.createServer(opcuaConfig.getServerName(), IOTProtocol.OPC_UA.getName(), serverConfigJson);

        JSONArray payloadJsonArray = buildPayloadString(uns);
        JSONObject injectNode = getInjectByServerId(fullNodes, serverId);
        // 如果server节点已经存在，则只需要往inject节点添加topic数据
        if (injectNode != null) {
            String payloadString = injectNode.getString("payload");
            JSONArray payloadArray = JSONArray.parseArray(payloadString);
            payloadArray.addAll(payloadJsonArray);
            injectNode.put("payload", payloadArray.toJSONString());

            JSONObject supModelNode = getSupModelByServerId(fullNodes, serverId);
            String mappings = supModelNode.getString("modelMapping");
            JSONObject jsonObject = JSON.parseObject(mappings);
            Map<String, Set<String>> maps = buildMapping(uns.getFields(), uns.getUnsTopic(), false);
            for (Map.Entry<String, Set<String>> entry : maps.entrySet()) {
                JSONArray jsonArray = jsonObject.getJSONArray(entry.getKey());
                if (jsonArray == null) {
                    jsonObject.put(entry.getKey(), entry.getValue());
                } else {
                    jsonArray.addAll(entry.getValue());
                }
            }
            supModelNode.put("modelMapping", jsonObject.toJSONString());

            return;
        }
        // 判断mqtt-broker节点是否存在，如果存在删除它
        JSONObject mqttBrokerNode = getMqttBrokerNode(fullNodes);
        if (mqttBrokerNode != null) {
            fullNodes.remove(mqttBrokerNode);
        }

        int maxHeight = super.getMaxHeight(fullNodes);
        // 替换节点id
        String selectModelNodeId = IDGenerator.generate();
        String mqttNodeId = IDGenerator.generate();
        String clientId = IDGenerator.generate();
        String injectId = IDGenerator.generate();

        String jsonFlowStr = tpl;
        jsonFlowStr = jsonFlowStr.replaceAll("\\$id_model_selector", selectModelNodeId)
                .replaceAll("\\$id_inject", injectId)
                .replaceAll("\\$id_mqtt_out", mqttNodeId)
                .replaceAll("\\$id_opcua_client", clientId)
                .replaceAll("\\$id_opcua_server", serverId);
        // 替换opcua-server endpoint
        jsonFlowStr = jsonFlowStr.replace("$opcua_server_addr", opcuaConfig.getServer().getEndpoint());
        String configString = payloadJsonArray.toJSONString().replace("\"", "\\\"");
        jsonFlowStr = jsonFlowStr.replace("$payload_array_string", configString);
        // 替换模型配置
        jsonFlowStr = jsonFlowStr.replace("$schema_json_string", uns.getUnsJsonString());
        // 替换点位和属性的映射关系
        Map<String, Set<String>> ms = buildMapping(uns.getFields(), uns.getUnsTopic(), false);
        jsonFlowStr = jsonFlowStr.replace("$mapping_string", JSON.toJSONString(ms).replace("\"", "\\\""));
        // 替换订阅采集频率
        jsonFlowStr = jsonFlowStr.replace("$pollRateUnit", opcuaConfig.getPollRate().getUnit());
        jsonFlowStr = jsonFlowStr.replace("$pollRate", opcuaConfig.getPollRate().getValue() + "");

        JSONArray jsonArr = JSON.parseArray(jsonFlowStr);

        // 设置整体流程高度
        for (int i = 0; i < jsonArr.size(); i++) {
            Integer highSpace = jsonArr.getJSONObject(i).getInteger("iy");
            int y = maxHeight + intervalHeight;
            if (highSpace != null) {
                y += highSpace;
            }
            jsonArr.getJSONObject(i).put("y", y);
        }
        fullNodes.addAll(jsonArr);
    }

    private JSONObject getInjectByServerId(JSONArray fullNodes, String serverId) {
        for (int i = 0; i < fullNodes.size(); i++) {
            String nodeType = fullNodes.getJSONObject(i).getString("type");
            if ("inject".equalsIgnoreCase(nodeType)) {
                String sid = fullNodes.getJSONObject(i).getString("id_server");
                if (serverId.equals(sid)) {
                    return fullNodes.getJSONObject(i);
                }
            }
        }
        return null;
    }

    private JSONObject getSupModelByServerId(JSONArray fullNodes, String serverId) {
        for (int i = 0; i < fullNodes.size(); i++) {
            String nodeType = fullNodes.getJSONObject(i).getString("type");
            if ("supmodel".equalsIgnoreCase(nodeType)) {
                String sid = fullNodes.getJSONObject(i).getString("id_server");
                if (serverId.equals(sid)) {
                    return fullNodes.getJSONObject(i);
                }
            }
        }
        return null;
    }

    private JSONArray buildPayloadString(BatchImportRequestVO.UnsVO uns) {
        JSONArray jsonArray = new JSONArray();
        List<FieldDefine> fields = uns.getFields();
        for (FieldDefine f : fields) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("model", uns.getUnsTopic().trim());
            jsonObject.put("nodeId", f.getIndex().trim());
            jsonArray.add(jsonObject);
        }
        return jsonArray;
    }

    public Map<String, Set<String>> buildMapping(List<FieldDefine> fields, String topic, boolean isArray) {
        // key=node  value=topic:fieldName
        Map<String, Set<String>> mapping = new HashMap<>();
        for (FieldDefine f : fields) {
            Set<String> ls = mapping.get(f.getIndex());
            if (ls == null) {
                ls = new HashSet<>();
            }
            ls.add(topic + ":" + f.getName());
            mapping.put(f.getIndex(), ls);
        }
        return mapping;
    }

    private JSONObject getMqttBrokerNode(JSONArray fullNodes) {
        for (int i = 0; i < fullNodes.size(); i++) {
            String nodeType = fullNodes.getJSONObject(i).getString("type");
            if ("mqtt-broker".equals(nodeType)) {
                return fullNodes.getJSONObject(i);
            }
        }
        return null;
    }


}

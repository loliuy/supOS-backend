package com.supos.adpter.nodered.service.parse;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.supos.adpter.nodered.dao.mapper.NodeServerMapper;
import com.supos.adpter.nodered.dao.po.IOTProtocolPO;
import com.supos.adpter.nodered.service.ObjectCachePool;
import com.supos.adpter.nodered.service.ProtocolServerService;
import com.supos.adpter.nodered.util.IDGenerator;
import com.supos.adpter.nodered.vo.BatchImportRequestVO;
import com.supos.common.dto.FieldDefine;
import com.supos.common.exception.NodeRedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;


/**
 * 处理自定义协议模版
 */
@Service("customProtocolParser")
public class CustomProtocolParser extends ParserApi {

    private String tplFile = "/custom-protocol.json.tpl";

    @Autowired
    private ObjectCachePool objectCachePool;
    @Autowired
    private NodeServerMapper nodeServerMapper;
    @Autowired
    private ProtocolServerService protocolServerService;

    @Override
    public String readTplFromCache(BatchImportRequestVO.UnsVO uns) {
        Object inputs = uns.getConfig().get("inputs");
        // 根据input=0或者1判断启用是否带inject节点的模版
        if (inputs != null && Integer.parseInt(inputs.toString()) > 0) {
            tplFile = "/custom-protocol-with-inject.json.tpl";
        }
        return readFromTpl(tplFile);

    }

    private JSONObject rebuildClientNode(Map<String, Object> clientMap, String serverConn, String serverId, String supModelId, String customProtocolNodeId) {
        clientMap.remove("server");
        // 关联server
        clientMap.put(serverConn, serverId);
        clientMap.put("x", 610);
        clientMap.put("y", 80);
        clientMap.put("id", customProtocolNodeId);
        clientMap.put("z", "");
        Object outputs = clientMap.get("outputs");
        int outs = outputs == null ? 1 : Integer.parseInt(outputs.toString());
        String[][] wires = new String[outs][1];
        wires[0][0] = supModelId;
        clientMap.put("wires", wires);
        clientMap.remove("inputs");
        clientMap.remove("outputs");
        return JSON.parseObject(JSON.toJSONString(clientMap));
    }

    private JSONObject rebuildServerNode(String serverConfigString, String serverId) {
        JSONObject serverNode = JSON.parseObject(serverConfigString);
        serverNode.put("id", serverId);
        return serverNode;
    }



    @Override
    public void parse(String tplJsonString, BatchImportRequestVO.UnsVO uns, JSONArray fullNodes) {

        int maxHeight = super.getMaxHeight(fullNodes);

        IOTProtocolPO iotProtocol = objectCachePool.getProtocolByName(uns.getProtocol());
        Object serverName = uns.getConfig().get("serverName");
        Object serverConfig = uns.getConfig().remove("server");
        if (serverConfig == null) {
            throw new NodeRedException("server配置为空");
        }
        if (serverName == null) {
            serverName = protocolServerService.guessServerNameFromConfig((Map<String, Object>)serverConfig);
        }
        String serverConfigJson = JSON.toJSONString(serverConfig);

//        Object outputDataType = uns.getConfig().get("outputDataType");
//        boolean isArray = outputDataType != null && "ARRAY".equalsIgnoreCase(outputDataType.toString());

        String serverId = super.createServer(serverName.toString(), uns.getProtocol(), serverConfigJson);
        // 获取inject节点，用于判断引用的是哪个流程模版
        JSONObject injectNode = getInjectByServerId(fullNodes, serverId);
        JSONObject mc = buildPayloadString(uns.getUnsTopic());
        if (injectNode != null) {
            String payloadString = injectNode.getString("payload");
            JSONArray payloadArray = JSONArray.parseArray(payloadString);
            payloadArray.add(mc);
            injectNode.put("payload", payloadArray.toJSONString());

            JSONObject supModelNode = getSupModelByServerId(fullNodes, serverId);
            String mappings = supModelNode.getString("modelMapping");
            if (isInteger(uns.getFields().get(0).getIndex())) {
                JSONObject obj1 = JSON.parseObject(mappings);
                obj1.putAll(buildMapping(uns.getFields(), uns.getUnsTopic(), true));
                supModelNode.put("modelMapping", obj1.toJSONString());
            } else {
                JSONObject obj2 = JSON.parseObject(mappings);
                Map<String, ?> maps = buildMapping(uns.getFields(), uns.getUnsTopic(), false);
                for (Map.Entry<String, ?> entry : maps.entrySet()) {
                    JSONArray jsonArray = obj2.getJSONArray(entry.getKey());
                    if (jsonArray == null) {
                        obj2.put(entry.getKey(), entry.getValue());
                    } else {
                        jsonArray.addAll((Collection) entry.getValue());
                    }
                }
                supModelNode.put("modelMapping", obj2.toJSONString());
            }
            return;
        }

        // 判断mqtt-broker节点是否存在，如果存在删除它
        JSONObject mqttBrokerNode = getMqttBrokerNode(fullNodes);
        if (mqttBrokerNode != null) {
            fullNodes.remove(mqttBrokerNode);
        }

        String supModelId = IDGenerator.generate();
        String injectId = IDGenerator.generate();
        String mqttNodeId = IDGenerator.generate();
        String functionId = IDGenerator.generate();
        String customProtocolNodeId = IDGenerator.generate();

        // 替换节点id
        String jsonFlowStr = tplJsonString.replaceAll("\\$id_model_selector", supModelId)
                .replaceAll("\\$id_mqtt", mqttNodeId)
                .replaceAll("\\$id_custom_protocol_server", serverId)
                .replaceAll("\\$id_inject", injectId)
                .replaceAll("\\$id_custom_protocol_node", customProtocolNodeId)
                .replaceAll("\\$id_func", functionId);
        // 替换topic
        jsonFlowStr = jsonFlowStr.replace("$model_topic", uns.getUnsTopic());
        // 替换模型数据 json字符串
        jsonFlowStr = jsonFlowStr.replace("$schema_json_string", uns.getUnsJsonString());

        // 替换点位和属性的映射关系
        if (isInteger(uns.getFields().get(0).getIndex())) {
            Map<String, ?> arrayMap = buildMapping(uns.getFields(), uns.getUnsTopic(), true);
            jsonFlowStr = jsonFlowStr.replace("$mapping_string", JSON.toJSONString(arrayMap).replace("\"", "\\\""));
        } else {
            Map<String, ?> jsonMap = buildMapping(uns.getFields(), uns.getUnsTopic(), false);
            jsonFlowStr = jsonFlowStr.replace("$mapping_string", JSON.toJSONString(jsonMap).replace("\"", "\\\""));
        }

        // 将modbus client配置动态放入inject中
        JSONArray payloads = new JSONArray();
        payloads.add(mc);
        String configString = payloads.toJSONString().replace("\"", "\\\"");
        jsonFlowStr = jsonFlowStr.replace("$payload_json_array", configString);

        JSONArray jsonArr = JSON.parseArray(jsonFlowStr);
        // 动态添加节点
        JSONObject clientNode = rebuildClientNode(uns.getConfig(), iotProtocol.getServerConn(), serverId, supModelId, customProtocolNodeId);
        JSONObject serverNode = rebuildServerNode(serverConfigJson, serverId);
        jsonArr.add(clientNode);
        jsonArr.add(serverNode);
        // 设置节点高度
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

    private JSONObject getMqttBrokerNode(JSONArray fullNodes) {
        for (int i = 0; i < fullNodes.size(); i++) {
            String nodeType = fullNodes.getJSONObject(i).getString("type");
            if ("mqtt-broker".equals(nodeType)) {
                return fullNodes.getJSONObject(i);
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

    private boolean isInteger(String str) {
        if (str == null) {
            return false;
        }
        return str.matches("-?\\d+"); // 匹配整数（包括负数）
    }

    public Map<String, ?> buildMapping(List<FieldDefine> fields, String topic, boolean isArray) {
        if (isArray) {
            // key=topic  value={"0":"", "1":""}
            Map<String, Map<String, String>> mappings = new HashMap<>();
            Map<String, String> indexMap = new HashMap<>();
            for (FieldDefine f : fields) {
                indexMap.put(f.getName(), f.getIndex());
            }
            mappings.put(topic, indexMap);
            return mappings;
        } else {
            // key=位号地址  value=[topic:fieldName]
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

    private JSONObject buildPayloadString(String topic) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("model", topic);
        return jsonObject;
    }
}

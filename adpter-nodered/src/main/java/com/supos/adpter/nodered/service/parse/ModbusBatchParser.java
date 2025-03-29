package com.supos.adpter.nodered.service.parse;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.supos.adpter.nodered.util.IDGenerator;
import com.supos.adpter.nodered.vo.BatchImportRequestVO;
import com.supos.common.dto.FieldDefine;
import com.supos.common.dto.protocol.ModbusConfigDTO;
import com.supos.common.enums.FunctionCode;
import com.supos.common.enums.IOTProtocol;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 批量topic处理
 * 解析modbus协议对应的node-red模版文件
 */
@Service("modbusBatchParser")
public class ModbusBatchParser extends ParserApi {

    private String tplFile = "/modbus-batch.json.tpl";

    @Override
    public String readTplFromCache(BatchImportRequestVO.UnsVO uns) {
        return readFromTpl(tplFile);
    }

    @Override
    public void parse(String tpl, BatchImportRequestVO.UnsVO uns, JSONArray fullNodes) {

        int maxHeight = super.getMaxHeight(fullNodes);
        // 将modbus的配置由key-value转成对象，方便读取
        ModbusConfigDTO modbusConfig = JSON.parseObject(JSON.toJSONString(uns.getConfig()), ModbusConfigDTO.class, Feature.TrimStringFieldValue);
        String serverConfigJson = JSON.toJSONString(modbusConfig.getServer());
        String serverId = super.createServer(modbusConfig.getServerName(), IOTProtocol.MODBUS.getName(), serverConfigJson);

        JSONObject injectNode = getInjectByServerId(fullNodes, serverId);
        JSONObject mc = buildPayloadString(modbusConfig, uns.getUnsTopic());
        if (injectNode != null) {
            String payloadString = injectNode.getString("payload");
            JSONArray payloadArray = JSONArray.parseArray(payloadString);
            payloadArray.add(mc);
            injectNode.put("payload", payloadArray.toJSONString());


            JSONObject supModelNode = getSupModelByServerId(fullNodes, serverId);
            String mappings = supModelNode.getString("modelMapping");
            JSONObject jsonObject = JSON.parseObject(mappings);
            jsonObject.putAll(buildMapping(uns.getFields(), uns.getUnsTopic(), true));
            supModelNode.put("modelMapping", jsonObject.toJSONString());

            String injectName = injectNode.getString("name");
            if (!StringUtils.hasText(injectName)) {
                injectNode.put("name", "unitId: " + modbusConfig.getUnitId());
            }
            return;
        }

        // 判断mqtt-broker节点是否存在，如果存在删除它
        JSONObject mqttBrokerNode = getMqttBrokerNode(fullNodes);
        if (mqttBrokerNode != null) {
            fullNodes.remove(mqttBrokerNode);
        }

        String modbusGetterId = IDGenerator.generate();
        String injectId = IDGenerator.generate();
        String mqttNodeId = IDGenerator.generate();
        String supModelId = IDGenerator.generate();
        String functionId = IDGenerator.generate();
        // 替换节点id
        String jsonFlowStr = tpl.replaceAll("\\$id_modbus_getter", modbusGetterId)
                .replaceAll("\\$id_mqtt", mqttNodeId)
                .replaceAll("\\$id_model_selector", supModelId)
                .replaceAll("\\$id_inject", injectId)
                .replaceAll("\\$id_func", functionId)
                .replaceAll("\\$id_modbus_client", serverId);
        // 替换unit id
        jsonFlowStr = jsonFlowStr.replace("$modbus_unit_id", modbusConfig.getUnitId());
        // 替换modbus采集频率
        jsonFlowStr = jsonFlowStr.replace("$pollRate", modbusConfig.getPollRate().getSeconds() + "");
        // 替换modbus服务端地址和端口
        jsonFlowStr = jsonFlowStr.replace("$modbus_host", modbusConfig.getServer().getHost().trim());
        jsonFlowStr = jsonFlowStr.replace("$modbus_port", modbusConfig.getServer().getPort().trim());
        // 给modbus服务端起一个名字
        jsonFlowStr = jsonFlowStr.replace("$modbus_client_name", modbusConfig.getServerName());
        // 替换模型数据 json字符串
        jsonFlowStr = jsonFlowStr.replace("$schema_json_string", uns.getUnsJsonString());
        // 将modbus client配置动态放入inject中
        JSONArray payloads = new JSONArray();
        payloads.add(mc);
        String configString = payloads.toJSONString().replace("\"", "\\\"");
        jsonFlowStr = jsonFlowStr.replace("$modbus_config_json_array", configString);
        Map<String, Map<String, String>> ms = buildMapping(uns.getFields(), uns.getUnsTopic(), true);
        jsonFlowStr = jsonFlowStr.replace("$mapping_string", JSON.toJSONString(ms).replace("\"", "\\\""));

        JSONArray jsonArr = JSON.parseArray(jsonFlowStr);
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

    public Map<String, Map<String, String>> buildMapping(List<FieldDefine> fields, String topic, boolean isArray) {
        // key=topic  value=[array index]
        Map<String, Map<String, String>> mappings = new HashMap<>();
        Map<String, String> indexMap = new HashMap<>();
        for (FieldDefine f : fields) {
            indexMap.put(f.getName(), f.getIndex());
        }
        mappings.put(topic, indexMap);
        return mappings;
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

    private JSONObject buildPayloadString(ModbusConfigDTO modbusConfig, String topic) {
        JSONObject jsonObject = new JSONObject();
        int code = FunctionCode.getCodeByName(modbusConfig.getFc());
        jsonObject.put("fc", code);
        jsonObject.put("unitid", Integer.parseInt(modbusConfig.getUnitId()));
        jsonObject.put("address", Integer.parseInt(modbusConfig.getAddress()));
        jsonObject.put("quantity", Integer.parseInt(modbusConfig.getQuantity()));
        jsonObject.put("model", topic);
        return jsonObject;
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

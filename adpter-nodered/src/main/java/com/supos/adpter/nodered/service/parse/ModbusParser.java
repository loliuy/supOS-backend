package com.supos.adpter.nodered.service.parse;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.parser.Feature;
import com.supos.adpter.nodered.util.IDGenerator;
import com.supos.adpter.nodered.vo.BatchImportRequestVO;
import com.supos.common.dto.FieldDefine;
import com.supos.common.dto.protocol.ModbusConfigDTO;
import com.supos.common.enums.IOTProtocol;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 解析时序模型对应的node-red模版文件
 */
@Service("modbusParser")
public class ModbusParser extends ParserApi {

    private String tplFile = "/modbus.json.tpl";

    @Override
    public String readTplFromCache(BatchImportRequestVO.UnsVO uns) {
        return readFromTpl(tplFile);
    }

    @Override
    public void parse(String tpl, BatchImportRequestVO.UnsVO uns, JSONArray fullNodes) {

        int maxHeight = super.getMaxHeight(fullNodes);

        String selectModelNodeId = IDGenerator.generate();
        String mqttNodeId = IDGenerator.generate();
        String modbusReadId = IDGenerator.generate();
        String modbusClientId = IDGenerator.generate();
        // 替换节点id
        String jsonFlowStr = tpl.replaceAll("\\$id_model_selector", selectModelNodeId)
                .replaceAll("\\$id_mqtt", mqttNodeId)
                .replaceAll("\\$id_modbus_read", modbusReadId)
                .replaceAll("\\$id_modbus_client", modbusClientId);
        // 替换模型topic
        jsonFlowStr = jsonFlowStr.replace("$model_topic", uns.getUnsTopic());
        // 将modbus的配置由key-value转成对象，方便读取
        ModbusConfigDTO modbusConfig = JSON.parseObject(JSON.toJSONString(uns.getConfig()), ModbusConfigDTO.class, Feature.TrimStringFieldValue);

        // 替换unit id
        jsonFlowStr = jsonFlowStr.replaceAll("\\$modbus_unit_id", modbusConfig.getUnitId());
        // 替换modbus功能码
        jsonFlowStr = jsonFlowStr.replace("$modbus_func_code", modbusConfig.getFc());
        // 替换modbus数组起始位置以及数量
        jsonFlowStr = jsonFlowStr.replace("$modbus_adr", modbusConfig.getAddress());
        jsonFlowStr = jsonFlowStr.replace("$modbus_quantity", modbusConfig.getQuantity());
        // 替换modbus采集频率
        jsonFlowStr = jsonFlowStr.replace("$Modbus_rate_unit", modbusConfig.getPollRate().getUnit());
        jsonFlowStr = jsonFlowStr.replace("$modbus_rate", modbusConfig.getPollRate().getValue() + "");
        // 替换modbus服务端地址和端口
        jsonFlowStr = jsonFlowStr.replace("$modbus_host", modbusConfig.getServer().getHost().trim());
        jsonFlowStr = jsonFlowStr.replace("$modbus_port", modbusConfig.getServer().getPort().trim());
        // 给modbus服务端起一个名字
        jsonFlowStr = jsonFlowStr.replace("$modbus_client_name", modbusConfig.getServerName());
        // 替换模型数据 json字符串
        jsonFlowStr = jsonFlowStr.replace("$schema_json_string", uns.getUnsJsonString());
        Map<String, Map<String, String>> ms = buildMapping(uns.getFields(), uns.getUnsTopic(), true);
        jsonFlowStr = jsonFlowStr.replace("$mapping_string", JSON.toJSONString(ms).replace("\"", "\\\""));

        String serverConfigJson = JSON.toJSONString(modbusConfig.getServer());
        super.createServer(modbusConfig.getServerName(), IOTProtocol.MODBUS.getName(), serverConfigJson);

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
}

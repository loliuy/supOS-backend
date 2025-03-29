package com.supos.adpter.nodered.service.parse;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.supos.adpter.nodered.dao.mapper.NodeServerMapper;
import com.supos.adpter.nodered.util.IDGenerator;
import com.supos.adpter.nodered.vo.BatchImportRequestVO;
import com.supos.common.dto.FieldDefine;
import com.supos.common.dto.protocol.OpcDAConfigDTO;
import com.supos.common.enums.IOTProtocol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 解析时序模型对应的node-red模版文件
 */
@Service("opcdaBatchParser")
public class OpcDABatchParser extends ParserApi {

    private String tplFile = "/opcda-read.json.tpl";

    private int intervalHeight = 40;

    @Autowired
    private NodeServerMapper nodeServerMapper;

    @Override
    public String readTplFromCache(BatchImportRequestVO.UnsVO uns) {
        return readFromTpl(tplFile);
    }

    @Override
    public void parse(String tpl, BatchImportRequestVO.UnsVO uns, JSONArray fullNodes) {

        OpcDAConfigDTO opcdaConfig = JSON.parseObject(JSON.toJSONString(uns.getConfig()), OpcDAConfigDTO.class, Feature.TrimStringFieldValue);
        String serverConfigJson = JSON.toJSONString(opcdaConfig.getServer());
        String serverId = super.createServer(opcdaConfig.getServerName(), IOTProtocol.OPC_DA.getName(), serverConfigJson);

        JSONObject injectNode = getNodeByType(fullNodes, "inject", serverId);
        // 如果server节点已经存在，则只需要往inject节点添加topic数据
        if (injectNode != null) {
            JSONObject supModelNode = getNodeByType(fullNodes, "supmodel", serverId);
            if (supModelNode != null) {
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
                supModelNode.put("selectedModel", "Auto");
            }
            JSONObject opcdaReadNode = getOpcdaReadClientByServer(fullNodes, serverId);
            if (opcdaReadNode != null) {
                JSONArray groupitems = opcdaReadNode.getJSONArray("groupitems");
                List<FieldDefine> fields = uns.getFields();
                Set<String> indexes = new HashSet<>(groupitems.toJavaList(String.class));
                for (FieldDefine f : fields) {
                    indexes.add(f.getIndex());
                }
                JSONArray newJSONArray = new JSONArray();
                newJSONArray.addAll(indexes);
                opcdaReadNode.put("groupitems", newJSONArray);
            }
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
                .replaceAll("\\$id_opcda_read", clientId)
                .replaceAll("\\$id_opcda_server", serverId);
        // 替换模型配置
        jsonFlowStr = jsonFlowStr.replace("$schema_json_string", uns.getUnsJsonString());
        jsonFlowStr = jsonFlowStr.replace("$select_model", uns.getUnsTopic());
        // 替换点位和属性的映射关系
        Map<String, Set<String>> ms = buildMapping(uns.getFields(), uns.getUnsTopic(), false);
        jsonFlowStr = jsonFlowStr.replace("$mapping_string", JSON.toJSONString(ms).replace("\"", "\\\""));
        // 替换订阅采集频率
        jsonFlowStr = jsonFlowStr.replace("$pollRate", opcdaConfig.getPollRate().getSeconds() + "");
        jsonFlowStr = jsonFlowStr.replace("$opcda_server_host", opcdaConfig.getServer().getHost());
        // 设置DCOM
        if (StringUtils.hasText(opcdaConfig.getServer().getDomain())) {
            jsonFlowStr = jsonFlowStr.replace("$opcda_server_domain", opcdaConfig.getServer().getDomain());
        } else {
            jsonFlowStr = jsonFlowStr.replace("$opcda_server_domain", "\\\"\\\"");
        }
        jsonFlowStr = jsonFlowStr.replace("$opcda_server_account", opcdaConfig.getServer().getAccount());
        jsonFlowStr = jsonFlowStr.replace("$opcda_server_password", opcdaConfig.getServer().getPassword());
        jsonFlowStr = jsonFlowStr.replace("$opcda_server_clsid", opcdaConfig.getServer().getClsid());
        jsonFlowStr = jsonFlowStr.replace("$opcda_server_timeout", opcdaConfig.getServer().getTimeout() + "");

        JSONArray jsonArr = JSON.parseArray(jsonFlowStr);

        // 设置整体流程高度
        for (int i = 0; i < jsonArr.size(); i++) {
            Integer highSpace = jsonArr.getJSONObject(i).getInteger("iy");
            int y = maxHeight + intervalHeight;
            if (highSpace != null) {
                y += highSpace;
            }
            jsonArr.getJSONObject(i).put("y", y);
            // 设置groupItems
            String type = jsonArr.getJSONObject(i).getString("type");
            if ("opcda-read".equals(type)) {
                JSONArray groupitems = jsonArr.getJSONObject(i).getJSONArray("groupitems");
                List<FieldDefine> fields = uns.getFields();
                Set<String> indexes = new HashSet<>();
                for (FieldDefine f : fields) {
                    indexes.add(f.getIndex());
                }
                groupitems.addAll(indexes);
            }
        }
        fullNodes.addAll(jsonArr);
    }

    private JSONObject getNodeByType(JSONArray fullNodes, String type, String serverId) {
        for (int i = 0; i < fullNodes.size(); i++) {
            String nodeType = fullNodes.getJSONObject(i).getString("type");
            if (type.equalsIgnoreCase(nodeType)) {
                String sid = fullNodes.getJSONObject(i).getString("id_server");
                if (serverId.equals(sid)) {
                    return fullNodes.getJSONObject(i);
                }
            }
        }
        return null;
    }

    private JSONObject getOpcdaReadClientByServer(JSONArray fullNodes, String serverId) {
        for (int i = 0; i < fullNodes.size(); i++) {
            String sid = fullNodes.getJSONObject(i).getString("server");
            if (serverId.equals(sid)) {
                return fullNodes.getJSONObject(i);
            }
        }
        return null;
    }

    public Map<String, Set<String>> buildMapping(List<FieldDefine> fields, String topic, boolean isArray) {
        // key=node  value=[topic:fieldName]
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

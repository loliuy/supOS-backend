package com.supos.adpter.nodered.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.supos.adpter.nodered.dao.mapper.NodeFlowMapper;
import com.supos.adpter.nodered.dao.mapper.NodeFlowModelMapper;
import com.supos.adpter.nodered.dao.po.IOTProtocolPO;
import com.supos.adpter.nodered.dao.po.NodeFlowPO;
import com.supos.adpter.nodered.enums.FlowStatus;
import com.supos.adpter.nodered.service.parse.*;
import com.supos.adpter.nodered.vo.BatchImportRequestVO;
import com.supos.common.dto.FieldDefine;
import com.supos.common.enums.IOTProtocol;
import com.supos.common.exception.NodeRedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ImportNodeRedFlowService {

    @Autowired
    private NodeRedAdapterService nodeRedAdapterService;
    @Autowired
    private ResourceLoader resourceLoader;
    @Autowired
    private NodeFlowMapper nodeFlowMapper;
    @Autowired
    private NodeFlowModelMapper nodeFlowModelMapper;
    private ParserApi parserApi;
    @Autowired
    private RelationalParser relationalParser;
    @Autowired
    private MqttParser mqttParser;
    @Autowired
    private ICMPParser icmpParser;
    @Autowired
    private ModbusParser modbusParser;
    @Autowired
    private ModbusBatchParser modbusBatchParser;
    @Autowired
    private OpcUAParser opcUAParser;
    @Autowired
    private OpcUABatchParser opcUABatchParser;
    @Autowired
    private RestApiParser restApiParser;
    @Autowired
    private OpcDABatchParser opcdaParse;
    @Autowired
    private CustomProtocolParser customProtocolParser;
    @Autowired
    private ObjectCachePool objectCachePool;

    /**
     * @param requestVO
     */
    public void importFlowFromUns(BatchImportRequestVO requestVO) {
        NodeFlowPO nf = nodeFlowMapper.getByName(requestVO.getName());
        while (nf != null) {
            String newName = requestVO.getName() + "(1)";
            log.error("流程({})已存在, 重名为{}", requestVO.getName(), newName);
            requestVO.setName(newName);
            nf = nodeFlowMapper.getByName(newName);
        }
        int topicSize = requestVO.getUns().size();
        JSONArray fullNodes = new JSONArray();
        log.info("node-red接收到协议：{}", requestVO.getUns().get(0).getProtocol());
        for (int i = 0; i < topicSize; i++) {
            BatchImportRequestVO.UnsVO unsVO = requestVO.getUns().get(i);
            // 解析时序还是关系模型的相关实现类
            ParserApi parserService = getParserImpl(unsVO.getProtocol(), topicSize);
            // 读取node-red的相关模版内容
            String tplJson = parserService.readTplFromCache(unsVO);
            // 替换模版中变量
            parserService.parse(tplJson, unsVO, fullNodes);

        }
        // 调用node-red接口， 部署新流程
        long id = nodeRedAdapterService.createFlow(requestVO.getName(), "", "node-red");
        List<String> topics = requestVO.getUns().stream().map(BatchImportRequestVO.UnsVO::getUnsTopic).collect(Collectors.toList());
        nodeRedAdapterService.proxyDeploy(id, fullNodes, topics);
    }

    public ParserApi getParserImpl(String protocol, int topicSize) {
        IOTProtocol protocolEnum = IOTProtocol.getByName(protocol);
        switch (protocolEnum) {
            case MODBUS: {
                if (topicSize == 1) {
                    return modbusParser;
                }
                return modbusBatchParser;
            }
            case OPC_UA: {
                // 判断是否启用批量模版
                if (topicSize == 1) {
                    return opcUAParser;
                }
                return opcUABatchParser;
            }
            case OPC_DA:
                return opcdaParse;
            case REST:
                return restApiParser;
            case RELATION:
                return relationalParser;
            case MQTT:
                return mqttParser;
            case ICMP:
                return icmpParser;
            default:
                IOTProtocolPO iotProtocol = objectCachePool.getProtocolByName(protocol);
                if (iotProtocol == null) {
                    throw new NodeRedException(400, "nodered.protocol.not.exist");
                }
                return customProtocolParser;
        }
    }

    /**
     * 根据topic批量删除关联流程
     *
     * @param topics
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, timeout = 300)
    public void deleteFlows(Collection<String> topics) {
        List<Long> parentIds = nodeFlowModelMapper.selectByTopics(topics);
        if (parentIds.isEmpty()) {
            log.info("skip delete flows, because flow-model refs is empty");
            return;
        }
        nodeFlowModelMapper.deleteByTopics(topics);
        for (Long id : parentIds) {
            nodeRedAdapterService.deleteFlow(id, false);
        }
    }

    public void updateFieldMapping(Collection<String> topics, List<FieldDefine> fields, String protocol) {
        List<Long> parentIds = nodeFlowModelMapper.selectByTopics(topics);
        if (parentIds.isEmpty()) {
            log.info("skip update flows, because flow-model refs is empty");
            return;
        }
        List<NodeFlowPO> nodeFlows = nodeFlowMapper.selectByIds(parentIds);
        for (NodeFlowPO nodeFlow : nodeFlows) {
            // 修改mapping关系
            JSONArray newFlowNodes = rebuildFlowNode(nodeFlow.getFlowData(), protocol, fields, topics);
            if (StringUtils.hasText(nodeFlow.getFlowId())) {
                nodeRedAdapterService.deployToNodeRed(nodeFlow.getFlowId(), nodeFlow.getFlowName(), nodeFlow.getDescription(), newFlowNodes);
            }
            nodeFlowMapper.deployUpdate(nodeFlow.getId(), nodeFlow.getFlowId(), FlowStatus.RUNNING.name(), newFlowNodes.toString());
        }
    }

    private JSONArray rebuildFlowNode(String flowJson, String protocol, List<FieldDefine> fields, Collection<String> topics) {
        ParserApi parser = getParserImpl(protocol, 2);
        JSONArray fullNodes = JSON.parseArray(flowJson);
        for (int i = 0; i < fullNodes.size(); i++) {
            String nodeType = fullNodes.getJSONObject(i).getString("type");
            if ("supmodel".equalsIgnoreCase(nodeType)) {
                Map<String, Object> allMap = new HashMap<>();
                for (String topic : topics) {
                    // TODO
                    boolean isArray = !"opcua".equalsIgnoreCase(protocol);
                    Map<String, ?> newMapping = parser.buildMapping(fields, topic, isArray);
                    allMap.putAll(newMapping);
                }
                String mappingString = JSON.toJSONString(allMap).replace("\"", "\\\"");
                fullNodes.getJSONObject(i).put("modelMapping", mappingString);
            }
        }
        return fullNodes;
    }

}

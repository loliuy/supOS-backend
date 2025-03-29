package com.supos.adpter.nodered.service;

import com.alibaba.fastjson.JSON;
import com.supos.adpter.nodered.dao.mapper.IOTProtocolMapper;
import com.supos.adpter.nodered.dao.mapper.NodeServerMapper;
import com.supos.adpter.nodered.dao.po.IOTProtocolPO;
import com.supos.adpter.nodered.dao.po.NodeServerPO;
import com.supos.adpter.nodered.util.IDGenerator;
import com.supos.adpter.nodered.util.IpUtils;
import com.supos.adpter.nodered.vo.*;
import com.supos.common.dto.protocol.KeyValuePair;
import com.supos.common.enums.IOTDataType;
import com.supos.common.enums.IOTProtocol;
import com.supos.common.exception.NodeRedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.lang.reflect.Array;
import java.util.*;

@Service
@Slf4j
public class ProtocolServerService {

    @Autowired
    private ObjectCachePool objectCachePool;
    @Autowired
    private IOTProtocolMapper iotProtocolMapper;
    @Autowired
    private NodeServerMapper nodeServerMapper;

    /**
     * 查询所有协议，包含自定义协议和内置协议
     * @return
     */
    public List<ProtocolsResponseVO> queryProtocols() {
        List<ProtocolsResponseVO> protocolNames = new LinkedList<>();
        List<KeyValuePair<String>> keyValuePairs = IOTProtocol.listSerialProtocol();
        for (KeyValuePair<String> kv : keyValuePairs) {
            protocolNames.add(new ProtocolsResponseVO(kv.getKey(), kv.getValue()));
        }
        List<String> dbProtocols = iotProtocolMapper.selectNames();
        for (String dbProtocol : dbProtocols) {
            protocolNames.add(new ProtocolsResponseVO(dbProtocol, dbProtocol));
        }
        return protocolNames;
    }

    /**
     * 根据协议名称 查询client和server的配置信息
     * @param protocolName
     * @return
     */
    public ProtocolResponseVO getProtocol(String protocolName) {
        IOTProtocolPO iotProtocolPO = iotProtocolMapper.queryByName(protocolName);
        if (iotProtocolPO == null) {
            throw new NodeRedException(400, "nodered.protocol.not.exist");
        }
        ProtocolResponseVO vo = new ProtocolResponseVO();
        vo.setServerConn(iotProtocolPO.getServerConn());
        Map clientConfig = JSON.parseObject(iotProtocolPO.getClientConfigJson(), Map.class);
        vo.setClientConfig(transfer(clientConfig, iotProtocolPO.getServerConn()));
        Map serverConfig = JSON.parseObject(iotProtocolPO.getServerConfigJson(), Map.class);
        vo.setServerConfig(transfer(serverConfig, ""));
        vo.setOutputDataType(buildKV());
        return vo;
    }

    /**
     * 删除自定义协议
     * @param name
     */
    public void deleteProtocol(String name) {
        iotProtocolMapper.deleteByName(name);
        objectCachePool.removeProtocol(name);
    }

    /**
     * 创建自定义协议， 并创建协议对应的server
     * @param addRequest
     * @return
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, timeout = 300)
    public AddProtocolResponseVO addProtocol(AddProtocolRequestVO addRequest) {
        IOTProtocol proto = IOTProtocol.getByName(addRequest.getName());
        if (!proto.getName().equalsIgnoreCase(IOTProtocol.UNKNOWN.getName())) {
            throw new NodeRedException(400, "nodered.protocol.has.exist");
        }
        IOTProtocolPO iotProtocol = iotProtocolMapper.queryByName(addRequest.getName());
        if (iotProtocol != null) {
            throw new NodeRedException(400, "nodered.protocol.has.exist");
        }
        iotProtocolMapper.create(buildIOTProtocolPO(addRequest));

        Map<String, Object> mServer = reverseTransfer(addRequest.getServerConfig());

        // 创建server实例
        AddServerRequestVO serverParam = buildServiceParam(addRequest.getName(), mServer);
        addServer(serverParam, false);

        AddProtocolResponseVO vo = new AddProtocolResponseVO();
        vo.setServerConn(addRequest.getServerConn());
        vo.setServerName(serverParam.getServerName());
        return vo;
    }

    public String guessServerNameFromConfig(Map<String, Object> serverConfig) {
        String host = "";
        String port = "";
        for (Map.Entry<String, Object> entry : serverConfig.entrySet()) {
            if (entry.getValue() != null) {
                if (IpUtils.containsIP(entry.getValue().toString()) || entry.getKey().equalsIgnoreCase("host")) {
                    host = entry.getValue().toString();
                }
                if (entry.getKey().equalsIgnoreCase("port") && entry.getValue().toString().matches("\\d+")) {
                    port = entry.getValue().toString();
                }
            }
        }
        String typeName = serverConfig.get("type").toString().replace(" ", "-");
        if (StringUtils.hasText(host) && StringUtils.hasText(port)) {
            return host + ":" + port;
        } else if (StringUtils.hasText(host)) {
            return typeName + "-" + host;
        } else if (StringUtils.hasText(port)) {
            return typeName + "-" + port;
        } else {
            return typeName + "-" + "server";
        }
    }

    private AddServerRequestVO buildServiceParam(String protocolName, Map<String, Object> serverConfig) {
        AddServerRequestVO vo = new AddServerRequestVO();
        vo.setServer(serverConfig);
        String serverName = guessServerNameFromConfig(serverConfig);
        vo.setServerName(serverName);
        vo.setProtocolName(protocolName);
        return vo;
    }

    private IOTProtocolPO buildIOTProtocolPO(AddProtocolRequestVO addRequest) {
        IOTProtocolPO po = new IOTProtocolPO();
        po.setCustom(1);
        po.setServerConn(addRequest.getServerConn());
        po.setName(addRequest.getName());
        Map<String, Object> clientConfigMap = reverseTransfer(addRequest.getClientConfig());
        Map<String, Object> serverConfigMap = reverseTransfer(addRequest.getServerConfig());
        po.setServerConfigJson(JSON.toJSONString(serverConfigMap));
        po.setClientConfigJson(JSON.toJSONString(clientConfigMap));
        return po;
    }

    /**
     * 前端导入自定义协议配置，包含client和server2个节点的json数据
     * @param configs
     * @return
     */
    public ProtocolImportResponseVO cleanProtocolConfig(List<Map<String,Object>> configs) {
        // 标记client节点和server节点
        KeyValuePair<Integer> serverIndex = getServerIndex(configs);
        if (serverIndex == null) {
            throw new NodeRedException(400, "nodered.server.config.not.exist");
        }
        // 去掉一些无效配置
        cleanFields(configs);

        Map<String,Object> serverConfig = configs.get(serverIndex.getValue());
        Map<String,Object> clientConfig = configs.get(1 - serverIndex.getValue());
        ProtocolImportResponseVO vo = new ProtocolImportResponseVO();
        vo.setClientConfig(transfer(clientConfig, serverIndex.getKey()));
        vo.setServerConfig(transfer(serverConfig, ""));
        vo.setServerConn(serverIndex.getKey());
        return vo;
    }

    // list转map
    private Map<String, Object> reverseTransfer(List<FieldObject> listFields) {
        Map<String, Object> reverseMap = new HashMap<>();
        for (FieldObject field : listFields) {
            reverseMap.put(field.getName(), field.getValue());
        }
        return reverseMap;
    }

    // map转list
    private List<FieldObject> transfer(Map<String, Object> config, String serverConn) {
        List<FieldObject> transferred = new ArrayList<>();
        for (Map.Entry<String, Object> entry : config.entrySet()) {
            FieldObject fo = new FieldObject();
            fo.setValue(entry.getValue());
            fo.setName(entry.getKey());
            if (StringUtils.hasText(serverConn) && entry.getKey().equals(serverConn)) {
                fo.setType("select");
                fo.setRequired(true);
            } else {
                if (entry.getValue() instanceof Boolean) {
                    fo.setType("switch");
                } else if (entry.getValue() instanceof String) {
                    fo.setType("input");
                } else if (entry.getValue() instanceof Integer
                        || entry.getValue() instanceof Long
                        || entry.getValue() instanceof Float
                        || entry.getValue() instanceof Double) {
                    fo.setType("inputNumber");
                } else if (entry.getValue() instanceof List) {
                    fo.setType("formList");
                }
            }
            transferred.add(fo);
        }
        return transferred;
    }

    private void cleanFields(List<Map<String,Object>> configs) {
        for (Map<String,Object> config : configs) {
            config.remove("id");
            config.remove("z");
            config.remove("x");
            config.remove("y");
            Object wires = config.get("wires");
            if (wires != null && wires.getClass().isArray()) {
                int outputs = Array.getLength(wires);
                config.put("outputs", outputs);
            }
            config.remove("wires");
            // 用于判断是否需要有inject节点
            if (!config.containsKey("inputs")) {
                config.put("inputs", 0);
            }
        }
    }

    private Set<KeyValuePair<String>> buildKV() {
        Set<KeyValuePair<String>> kvs = new HashSet<>();
        for (IOTDataType it : IOTDataType.values()) {
            kvs.add(new KeyValuePair<>(it.name(), it.getDesc()));
        }
        return kvs;
    }

    // 确定server位于LIST中的位置，并且指定client中连接server的字段名称
    private KeyValuePair<Integer> getServerIndex(List<Map<String,Object>> configs) {
        for (int i = 0; i < 2; i++) {
            Map<String,Object> config = configs.get(i);
            String id = config.get("id").toString();
            for (Map.Entry<String, Object> entry : configs.get(1-i).entrySet()) {
                Object v = entry.getValue();
                // server id匹配client的某个字段建立关联
                if (v instanceof String && id.equals(v.toString())) {
                    // 清空关联关系
                    entry.setValue("");
                    return new KeyValuePair<>(entry.getKey(), i);
                }
            }
        }
        return null;
    }


    /**
     * 添加协议server端配置
     * @param serverRequest
     */
    public String addServer(AddServerRequestVO serverRequest, boolean throwEx) {
        NodeServerPO nodeServer = objectCachePool.getServerByName(serverRequest.getServerName(), serverRequest.getProtocolName());
        if (nodeServer != null) {
            if (throwEx) {
                throw new NodeRedException(400, "nodered.server.exist");
            }
            return nodeServer.getId();
        }
        NodeServerPO nodeServerPO = buildPO(serverRequest);
        nodeServerMapper.insert(nodeServerPO);
        log.info("协议： {}, server： {}, 创建成功", serverRequest.getProtocolName(), serverRequest.getServerName());
        return nodeServerPO.getId();
    }

    private NodeServerPO buildPO(AddServerRequestVO serverRequest) {
        NodeServerPO po = new NodeServerPO();
        String id = IDGenerator.generate();
        po.setId(id);
        po.setProtocolName(serverRequest.getProtocolName());
        po.setServerName(serverRequest.getServerName());
        // map to json string
        serverRequest.getServer().put("id", id);
        String configJson = JSON.toJSONString(serverRequest.getServer());
        po.setConfigJson(configJson);
        return po;
    }

    private List<NodeServerVO> buildVO(List<NodeServerPO> pos) {
        List<NodeServerVO> vos = new ArrayList<>();
        for (NodeServerPO po : pos) {
            NodeServerVO vo = new NodeServerVO();
            vo.setProtocolName(po.getProtocolName());
            vo.setServerName(po.getServerName());
            Map config = JSON.parseObject(po.getConfigJson(), Map.class);
            vo.setServer(config);
            vo.setId(po.getId());
            vos.add(vo);
        }
        return vos;
    }

    /**
     * 根据协议查询对应的配置列表
     * @param protocolName
     * @return
     */
    public List<NodeServerVO> searchList(String protocolName) {
        List<NodeServerPO> nodeServerPOS = nodeServerMapper.selectList(protocolName);
        return buildVO(nodeServerPOS);
    }

    public void deleteById(String id) {
        NodeServerPO nodeServer = nodeServerMapper.selectById(id);
        if (nodeServer != null) {
            nodeServerMapper.deleteById(id);
        }
        objectCachePool.removeServer(nodeServer.getServerName());
    }

}

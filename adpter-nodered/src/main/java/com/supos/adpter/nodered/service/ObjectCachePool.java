package com.supos.adpter.nodered.service;

import com.supos.adpter.nodered.dao.mapper.IOTProtocolMapper;
import com.supos.adpter.nodered.dao.mapper.NodeServerMapper;
import com.supos.adpter.nodered.dao.po.IOTProtocolPO;
import com.supos.adpter.nodered.dao.po.NodeServerPO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ObjectCachePool {

    // 30分钟释放一次 #CacheCleanScheduler
    // 避免每次都落库查询
    public static final Map<String, IOTProtocolPO> IOT_PROTOCOL_MAP = new ConcurrentHashMap<>();
    // key=server_name+protocol
    public static final Map<String, NodeServerPO> SERVER_MAP = new ConcurrentHashMap<>();

    @Autowired
    private IOTProtocolMapper iotProtocolMapper;
    @Autowired
    private NodeServerMapper nodeServerMapper;

    public IOTProtocolPO getProtocolByName(String name) {
        IOTProtocolPO iotProtocol = IOT_PROTOCOL_MAP.get(name);
        if (iotProtocol == null) {
            synchronized (ObjectCachePool.class) {
                if (iotProtocol == null) {
                    iotProtocol = iotProtocolMapper.queryByName(name);
                    if (iotProtocol != null) {
                        IOT_PROTOCOL_MAP.put(name, iotProtocol);
                    }
                }
            }
        }
        return iotProtocol;
    }

    public void removeProtocol(String name) {
        IOT_PROTOCOL_MAP.remove(name);
    }

    public NodeServerPO getServerByName(String name, String protocol) {
        NodeServerPO nodeServer = SERVER_MAP.get(name + protocol);
        if (nodeServer == null) {
            synchronized (ObjectCachePool.class) {
                if (nodeServer == null) {
                    nodeServer = nodeServerMapper.selectByName(name, protocol);
                    if (nodeServer != null) {
                        SERVER_MAP.put(name + protocol, nodeServer);
                    }
                }
            }
        }
        return nodeServer;
    }

    public void removeServer(String name) {
        SERVER_MAP.remove(name);
    }

}

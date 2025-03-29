package com.supos.adpter.nodered.service.enums;

import com.supos.common.annotation.ProtocolIdentifierProvider;
import com.supos.common.dto.protocol.OpcUAConfigDTO;
import com.supos.common.dto.protocol.ProtocolTagEnums;
import com.supos.common.enums.IOTProtocol;
import com.supos.common.exception.NodeRedException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfig;
import org.eclipse.milo.opcua.sdk.client.api.identity.AnonymousProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.IdentityProvider;
import org.eclipse.milo.opcua.sdk.client.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.client.DiscoveryClient;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.eclipse.milo.opcua.stack.core.util.EndpointUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;


/**
 * 枚举opcua位号
 */
@Slf4j
@Service("opcUANodeIdentifiers")
@ProtocolIdentifierProvider(IOTProtocol.OPC_UA)
public class OpcUANodeIdentifiers implements IdentifiersInterface<OpcUAConfigDTO> {

    /**
     * 从根节点开始遍历所有Variable节点的名称和类型
     * @param config
     * @return 位号列表
     */
    @Override
    public List<ProtocolTagEnums> listTags(OpcUAConfigDTO config, String topic) {
        List<ProtocolTagEnums> container = new LinkedList<>();
        // 重新从server遍历一遍位号，并放入缓存
        OpcUaClient client = null;
        try {
            client = createClient(config.getServer().getEndpoint(), config.getServer().getHost());
            client.connect().get();
            NodeId rootNode = new NodeId(0, 85);
            browseNodesIntoCache(client, rootNode, container); // 从根节点开始
        } catch (Exception e) {
            log.error("create opcua client failed", e);
            throw new NodeRedException("create opcua client failed", e);
        } finally {
            try {
                if (client != null) {
                    client.disconnect().get();
                }
            } catch (Exception e) {
                log.error("opcua client disconnect fail", e);
            }
        }
        return container;
    }

    /*public static void main(String[] args) {
        List<ProtocolTagEnums> container = new LinkedList<>();
        // 重新从server遍历一遍位号，并放入缓存
        OpcUaClient client = null;
        try {
            client = createClient("opc.tcp://192.168.18.83:53530/OPCUA/SimulationServer", "192.168.18.83");
            client.connect().get();
            NodeId rootNode = new NodeId(0, 85);
            browseNodesIntoCache(client, rootNode, container); // 从根节点开始
        } catch (Exception e) {
            log.error("create opcua client failed", e);
            throw new NodeRedException("create opcua client failed", e);
        } finally {
            try {
                if (client != null) {
                    client.disconnect().get();
                }
            } catch (Exception e) {
                log.error("opcua client disconnect fail", e);
            }
        }
    }*/

    private OpcUaClient createClient(String endpointUri, String host) throws ExecutionException, InterruptedException, UaException {
        // 发现服务器的端点
        List<EndpointDescription> endpoints = DiscoveryClient.getEndpoints(endpointUri).get();

        // 选择第一个端点
        EndpointDescription endpoint = EndpointUtil.updateUrl(endpoints.get(0), host);

        // 配置客户端
        IdentityProvider identityProvider = new AnonymousProvider();
        OpcUaClientConfig config = OpcUaClientConfig.builder()
                .setEndpoint(endpoint)
                .setIdentityProvider(identityProvider)
                .build();

        // 创建客户端实例
        return OpcUaClient.create(config);
    }

    private void browseNodesIntoCache(OpcUaClient client, NodeId nodeId, List<ProtocolTagEnums> container) throws Exception {
        // 浏览节点
        client.getAddressSpace().browseNodes(nodeId).forEach(reference -> {
            NodeId refNodeId = null;
            if (reference.getNodeId().getIdentifier() instanceof UInteger) {
                refNodeId = new NodeId(reference.getNodeId().getNamespaceIndex(), (UInteger)reference.getNodeId().getIdentifier());
            } else {
                refNodeId = new NodeId(reference.getNodeId().getNamespaceIndex(), reference.getNodeId().getIdentifier().toString());
            }
            if ("Variable".equalsIgnoreCase(reference.getNodeClass().name())) {
                try {
                    UaVariableNode variableNode = (UaVariableNode)reference;
                    Object identifier = variableNode.getDataType().getIdentifier();
                    Object value = variableNode.getValue().getValue().getValue();
                    String javaType = convertToJavaType(identifier, value);

                    if (StringUtils.hasText(javaType)) {
                        container.add(new ProtocolTagEnums(reference.getNodeId().toParseableString(), javaType));
                    }
                } catch (Exception e) {
                    log.error("get variable node failed", e);
                }
            } else {
                // 递归浏览子节点
                try {
                    browseNodesIntoCache(client, refNodeId, container);
                } catch (Exception e) {
                    log.error("opcua节点递归失败， 节点ID={}", reference.getNodeId().toParseableString());
                }
            }
        });
    }

    private String convertToJavaType(Object dataTypeIdentifier, Object value) {
        switch (dataTypeIdentifier.toString()) {
            case "1": return "boolean";
            case "3": return "byte";
            case "10": return "float";
            case "11": return "double";
            case "12": case "15": return "string";
            case "13": return "datetime";
            case "4": case "5": case "6": case "7": case "27": case "28": return "int";
            case "8": case "9": return "long";
            default: return getValueType(value);
        }
    }

    private String getValueType(Object value) {
        log.info("valueType = {}", value.getClass().getSimpleName());
        if (value instanceof Integer) return "int";
        if (value instanceof Double) return "double";
        if (value instanceof Boolean) return "boolean";
        if (value instanceof Long) return "long";
        if (value instanceof String) return "string";
        if (value instanceof Float) return "float";
        return "";
    }
}

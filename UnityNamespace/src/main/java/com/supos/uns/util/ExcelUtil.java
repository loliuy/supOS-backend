package com.supos.uns.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.supos.common.Constants;
import com.supos.common.dto.protocol.*;
import com.supos.common.enums.ExcelTypeEnum;
import com.supos.common.enums.IOTProtocol;
import com.supos.common.utils.JsonUtil;
import com.supos.common.vo.FieldDefineVo;
import com.supos.uns.dao.po.UnsPo;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author sunlifang
 * @version 1.0
 * @description: TODO
 * @date 2025/1/10 13:54
 */
public class ExcelUtil {
    private static List<String> TEMPLATE_INDEX = new LinkedList<>();
    private static List<String> FOLDER_INDEX = new LinkedList<>();
    private static List<String> TIMESERIES_MODBUS_INDEX = new LinkedList<>();
    private static List<String> TIMESERIES_OPCUA_INDEX = new LinkedList<>();
    private static List<String> TIMESERIES_OPCDA_INDEX = new LinkedList<>();
    private static List<String> RELATION_RESTAPI_INDEX = new LinkedList<>();
    private static List<String> MQTT_INDEX = new LinkedList<>();

    private static List<String> TIMESERIES_INDEX = new LinkedList<>();
    private static List<String> RELATION_INDEX = new LinkedList<>();

    static {
        TEMPLATE_INDEX.add("name");
        TEMPLATE_INDEX.add("fields");
        TEMPLATE_INDEX.add("description");

        FOLDER_INDEX.add("name");
        FOLDER_INDEX.add("alias");
        FOLDER_INDEX.add("template");
        FOLDER_INDEX.add("fields");
        FOLDER_INDEX.add("description");

        TIMESERIES_MODBUS_INDEX.add("topic");
        TIMESERIES_MODBUS_INDEX.add("alias");
        TIMESERIES_MODBUS_INDEX.add("template");
        TIMESERIES_MODBUS_INDEX.add("fields");
        TIMESERIES_MODBUS_INDEX.add("server.host");
        TIMESERIES_MODBUS_INDEX.add("server.port");
        TIMESERIES_MODBUS_INDEX.add("unitId");
        TIMESERIES_MODBUS_INDEX.add("fc");
        TIMESERIES_MODBUS_INDEX.add("address");
        TIMESERIES_MODBUS_INDEX.add("quantity");
        TIMESERIES_MODBUS_INDEX.add("pollRate.value");
        TIMESERIES_MODBUS_INDEX.add("pollRate.unit");
        TIMESERIES_MODBUS_INDEX.add("description");
        TIMESERIES_MODBUS_INDEX.add("autoFlow");
        TIMESERIES_MODBUS_INDEX.add("autoDashboard");
        TIMESERIES_MODBUS_INDEX.add("persistence");
        TIMESERIES_MODBUS_INDEX.add("label");

        TIMESERIES_OPCUA_INDEX.add("topic");
        TIMESERIES_OPCUA_INDEX.add("alias");
        TIMESERIES_OPCUA_INDEX.add("template");
        TIMESERIES_OPCUA_INDEX.add("fields");
        TIMESERIES_OPCUA_INDEX.add("server.host");
        TIMESERIES_OPCUA_INDEX.add("server.port");
        TIMESERIES_OPCUA_INDEX.add("server.location");
        TIMESERIES_OPCUA_INDEX.add("pollRate.value");
        TIMESERIES_OPCUA_INDEX.add("pollRate.unit");
        TIMESERIES_OPCUA_INDEX.add("description");
        TIMESERIES_OPCUA_INDEX.add("autoFlow");
        TIMESERIES_OPCUA_INDEX.add("autoDashboard");
        TIMESERIES_OPCUA_INDEX.add("persistence");
        TIMESERIES_OPCUA_INDEX.add("label");

        TIMESERIES_OPCDA_INDEX.add("topic");
        TIMESERIES_OPCDA_INDEX.add("alias");
        TIMESERIES_OPCDA_INDEX.add("template");
        TIMESERIES_OPCDA_INDEX.add("fields");
        TIMESERIES_OPCDA_INDEX.add("server.host");
        TIMESERIES_OPCDA_INDEX.add("server.domain");
        TIMESERIES_OPCDA_INDEX.add("server.account");
        TIMESERIES_OPCDA_INDEX.add("server.password");
        TIMESERIES_OPCDA_INDEX.add("server.clsid");
        TIMESERIES_OPCDA_INDEX.add("server.timeout");
        TIMESERIES_OPCDA_INDEX.add("pollRate.value");
        TIMESERIES_OPCDA_INDEX.add("pollRate.unit");
        TIMESERIES_OPCDA_INDEX.add("description");
        TIMESERIES_OPCDA_INDEX.add("autoFlow");
        TIMESERIES_OPCDA_INDEX.add("autoDashboard");
        TIMESERIES_OPCDA_INDEX.add("persistence");
        TIMESERIES_OPCDA_INDEX.add("label");

        RELATION_RESTAPI_INDEX.add("topic");
        RELATION_RESTAPI_INDEX.add("alias");
        RELATION_RESTAPI_INDEX.add("template");
        RELATION_RESTAPI_INDEX.add("fields");
        RELATION_RESTAPI_INDEX.add("dataPath");
        RELATION_RESTAPI_INDEX.add("serverName");
        RELATION_RESTAPI_INDEX.add("method");
        RELATION_RESTAPI_INDEX.add("syncRate.unit");
        RELATION_RESTAPI_INDEX.add("syncRate.value");
        RELATION_RESTAPI_INDEX.add("pageDef.start.key");
        RELATION_RESTAPI_INDEX.add("pageDef.offset.key");
        RELATION_RESTAPI_INDEX.add("fullUrl");
        RELATION_RESTAPI_INDEX.add("headers");
        RELATION_RESTAPI_INDEX.add("body");
        RELATION_RESTAPI_INDEX.add("description");
        RELATION_RESTAPI_INDEX.add("autoFlow");
        RELATION_RESTAPI_INDEX.add("autoDashboard");
        RELATION_RESTAPI_INDEX.add("persistence");
        RELATION_RESTAPI_INDEX.add("label");

        MQTT_INDEX.add("topic");
        MQTT_INDEX.add("alias");
        MQTT_INDEX.add("template");
        MQTT_INDEX.add("fields");
        MQTT_INDEX.add("inputTopic");
        MQTT_INDEX.add("inputName");
        MQTT_INDEX.add("serverName");
        MQTT_INDEX.add("server.host");
        MQTT_INDEX.add("server.port");
        MQTT_INDEX.add("description");
        MQTT_INDEX.add("autoFlow");
        MQTT_INDEX.add("autoDashboard");
        MQTT_INDEX.add("persistence");
        MQTT_INDEX.add("label");

        TIMESERIES_INDEX.add("topic");
        TIMESERIES_INDEX.add("alias");
        TIMESERIES_INDEX.add("template");
        TIMESERIES_INDEX.add("fields");
        TIMESERIES_INDEX.add("description");
        TIMESERIES_INDEX.add("autoFlow");
        TIMESERIES_INDEX.add("autoDashboard");
        TIMESERIES_INDEX.add("persistence");
        TIMESERIES_INDEX.add("label");

        RELATION_INDEX.add("topic");
        RELATION_INDEX.add("alias");
        RELATION_INDEX.add("template");
        RELATION_INDEX.add("fields");
        RELATION_INDEX.add("description");
        RELATION_INDEX.add("autoFlow");
        RELATION_INDEX.add("autoDashboard");
        RELATION_INDEX.add("persistence");
        RELATION_INDEX.add("label");
    }

    /**
     * 校验表头是否正确
     * @param excelType
     * @param heads
     * @return
     */
    public static boolean checkHead(ExcelTypeEnum excelType, List<Object> heads) {
        List<String> needHeads = new ArrayList<>();
        List<String> tempHeads = heads != null ? heads.stream().map(Object::toString).collect(Collectors.toList()) : new ArrayList<>();
        switch (excelType) {
            case Template:
                needHeads = TEMPLATE_INDEX;break;
            case Folder:
                needHeads = FOLDER_INDEX;break;
            case RELATION:
                needHeads = RELATION_INDEX;break;
            case TIMESERIES:
                needHeads = TIMESERIES_INDEX;break;
            case RELATION_RESTAPI:
                needHeads = RELATION_RESTAPI_INDEX;break;
            case RELATION_MQTT:
                needHeads = MQTT_INDEX;break;
            case TIMESERIES_MODBUS:
                needHeads = TIMESERIES_MODBUS_INDEX;break;
            case TIMESERIES_OPCUA:
                needHeads = TIMESERIES_OPCUA_INDEX;break;
            case TIMESERIES_OPCDA:
                needHeads = TIMESERIES_OPCDA_INDEX;break;
            case TIMESERIES_MQTT:
                needHeads = MQTT_INDEX;break;
        }

        for (String needHead : needHeads) {
            if (!tempHeads.contains(needHead)) {
                return false;
            }
        }
        return true;
    }
    
    public static RowWrapper createRow(UnsPo unsPo, Map<String, UnsPo> templateMap, Map<String, Set<String>> unsLabelNamesMap) {
        List<Object> dataList = null;
        IOTProtocol protocol = null;

        if (unsPo.getPathType() == 0) {
            // 解析文件夹
            dataList = createRowForModel(unsPo, templateMap);
        } else if (unsPo.getPathType() == 1) {
            // 解析模板
            dataList = createRowForTemplate(unsPo);
        } else {
            //解析文件
            protocol = IOTProtocol.getByName(unsPo.getProtocolType());
            if (unsPo.getDataType() == Constants.TIME_SEQUENCE_TYPE) {
                //时序数据解析
                switch (protocol) {
                    case MODBUS: dataList = createRowForModbus(unsPo, templateMap, unsLabelNamesMap);break;
                    case OPC_UA: dataList = createRowForOpcua(unsPo, templateMap, unsLabelNamesMap);break;
                    case OPC_DA: dataList = createRowForOpcda(unsPo, templateMap, unsLabelNamesMap);break;
                    case MQTT: dataList = createRowForMqtt(unsPo, templateMap, unsLabelNamesMap);break;
                    default: dataList = createRowForBlankInstance(unsPo, templateMap, unsLabelNamesMap);break;
                }
            } else if (unsPo.getDataType() == Constants.RELATION_TYPE) {
                //关系数据解析
                switch (protocol) {
                    case REST: dataList = createRowForRestAPI(unsPo, templateMap, unsLabelNamesMap);break;
                    case RELATION: dataList = createRowForBlankRelationInstance(unsPo, templateMap, unsLabelNamesMap);break;
                    case MQTT: dataList = createRowForMqtt(unsPo, templateMap, unsLabelNamesMap);break;
                    default: dataList = createRowForBlankRelationInstance(unsPo, templateMap, unsLabelNamesMap);break;
                }
            }
        }

        return new RowWrapper(protocol, dataList);
    }

    /**
     * 文件夹
     * @param unsPo
     * @return
     */
    private static List<Object> createRowForModel(UnsPo unsPo, Map<String, UnsPo> templateMap) {
        List<Object> dataList = new ArrayList<>(3);
        dataList.add(unsPo.getPath());
        dataList.add(unsPo.getAlias() != null ? unsPo.getAlias() : "");
        if (unsPo.getModelId() != null) {
            UnsPo template = templateMap.get(unsPo.getModelId());
            dataList.add(template != null ? template.getPath() : "");
        } else {
            dataList.add("");
        }
        dataList.add(field(unsPo.getFields()));

        dataList.add(unsPo.getDescription());
        return dataList;
    }

    /**
     * 空白文件
     * @param unsPo
     * @return
     */
    private static List<Object> createRowForBlankInstance(UnsPo unsPo, Map<String, UnsPo> templateMap, Map<String, Set<String>> unsLabelNamesMap) {
        List<Object> dataList = new ArrayList<>(3);
        dataList.add(unsPo.getPath());
        dataList.add(unsPo.getAlias() != null ? unsPo.getAlias() : "");
        if (unsPo.getModelId() != null) {
            UnsPo template = templateMap.get(unsPo.getModelId());
            dataList.add(template != null ? template.getPath() : "");
        } else {
            dataList.add("");
        }
        dataList.add(field(unsPo.getFields()));
        dataList.add(unsPo.getDescription());
        if (unsPo.getWithFlags() != null) {
            dataList.add(Constants.withFlow(unsPo.getWithFlags()) ? "TRUE" : "FALSE");
            dataList.add(Constants.withDashBoard(unsPo.getWithFlags()) ? "TRUE" : "FALSE");
            dataList.add(Constants.withSave2db(unsPo.getWithFlags()) ? "TRUE" : "FALSE");
        } else {
            dataList.add("");
            dataList.add("");
            dataList.add("");
        }

        Set<String> labels = unsLabelNamesMap.get(unsPo.getId());
        if (CollectionUtils.isNotEmpty(labels)) {
            dataList.add(StringUtils.join(labels, ','));
        } else {
            dataList.add("");
        }
        return dataList;
    }

    /**
     * Modbus协议文件
     * @param unsPo
     * @return
     */
    private static List<Object> createRowForModbus(UnsPo unsPo, Map<String, UnsPo> templateMap, Map<String, Set<String>> unsLabelNamesMap) {
        List<Object> dataList = new ArrayList<>(TIMESERIES_MODBUS_INDEX.size());
        ModbusConfigDTO config = null;
        if (StringUtils.isNotBlank(unsPo.getProtocol())) {
            config = JsonUtil.fromJson(unsPo.getProtocol(), ModbusConfigDTO.class);
        }
        for(String key : TIMESERIES_MODBUS_INDEX) {
            if ("topic".equals(key)) {
                dataList.add(StringUtils.isNotBlank(unsPo.getPath()) ? unsPo.getPath() : "");
            }
            if ("alias".equals(key)) {
                dataList.add(unsPo.getAlias() != null ? unsPo.getAlias() : "");
            }
            if ("template".equals(key)) {
                if (unsPo.getModelId() != null) {
                    UnsPo template = templateMap.get(unsPo.getModelId());
                    dataList.add(template != null ? template.getPath() : "");
                } else {
                    dataList.add("");
                }
            }
            if ("fields".equals(key)) {
                dataList.add(field(unsPo.getFields()));
            }
            if ("server.host".equals(key)) {
                if (config != null && config.getServer() != null) {
                    dataList.add(config.getServer().getHost());
                } else {
                    dataList.add("");
                }
            }
            if ("server.port".equals(key)) {
                if (config != null && config.getServer() != null) {
                    dataList.add(config.getServer().getPort());
                } else {
                    dataList.add("");
                }
            }
            if ("unitId".equals(key)) {
                if (config != null && StringUtils.isNotBlank(config.getUnitId())) {
                    dataList.add(config.getUnitId());
                } else {
                    dataList.add("");
                }
            }
            if ("fc".equals(key)) {
                if (config != null && StringUtils.isNotBlank(config.getFc())) {
                    dataList.add(config.getFc());
                } else {
                    dataList.add("");
                }
            }
            if ("address".equals(key)) {
                if (config != null && StringUtils.isNotBlank(config.getAddress())) {
                    dataList.add(config.getAddress());
                } else {
                    dataList.add("");
                }
            }
            if ("quantity".equals(key)) {
                if (config != null && StringUtils.isNotBlank(config.getQuantity())) {
                    dataList.add(config.getQuantity());
                } else {
                    dataList.add("");
                }
            }
            if ("pollRate.value".equals(key)) {
                if (config != null && config.getPollRate() != null) {
                    dataList.add(config.getPollRate().getValue());
                } else {
                    dataList.add("");
                }
            }
            if ("pollRate.unit".equals(key)) {
                if (config != null && config.getPollRate() != null) {
                    dataList.add(config.getPollRate().getUnit());
                } else {
                    dataList.add("");
                }
            }
            if ("description".equals(key)) {
                dataList.add(StringUtils.isNotBlank(unsPo.getDescription()) ? unsPo.getDescription() : "");
            }
            if ("autoFlow".equals(key)) {
                if (unsPo.getWithFlags() != null) {
                    dataList.add(Constants.withFlow(unsPo.getWithFlags()) ? "TRUE" : "FALSE");
                } else {
                    dataList.add("");
                }
            }
            if ("autoDashboard".equals(key)) {
                if (unsPo.getWithFlags() != null) {
                    dataList.add(Constants.withDashBoard(unsPo.getWithFlags()) ? "TRUE" : "FALSE");
                } else {
                    dataList.add("");
                }
            }
            if ("persistence".equals(key)) {
                if (unsPo.getWithFlags() != null) {
                    dataList.add(Constants.withSave2db(unsPo.getWithFlags()) ? "TRUE" : "FALSE");
                } else {
                    dataList.add("");
                }
            }
            if ("label".equals(key)) {
                Set<String> labels = unsLabelNamesMap.get(unsPo.getId());
                if (CollectionUtils.isNotEmpty(labels)) {
                    dataList.add(StringUtils.join(labels, ','));
                } else {
                    dataList.add("");
                }
            }
        }
        return dataList;
    }

    /**
     * Opcua协议文件
     * @param unsPo
     * @return
     */
    private static List<Object> createRowForOpcua(UnsPo unsPo, Map<String, UnsPo> templateMap, Map<String, Set<String>> unsLabelNamesMap) {
        List<Object> dataList = new ArrayList<>(TIMESERIES_OPCUA_INDEX.size());
        OpcUAConfigDTO config = null;
        if (StringUtils.isNotBlank(unsPo.getProtocol())) {
            config = JsonUtil.fromJson(unsPo.getProtocol(), OpcUAConfigDTO.class);
        }
        for(String key : TIMESERIES_OPCUA_INDEX) {
            if ("topic".equals(key)) {
                dataList.add(StringUtils.isNotBlank(unsPo.getPath()) ? unsPo.getPath() : "");
            }
            if ("alias".equals(key)) {
                dataList.add(unsPo.getAlias() != null ? unsPo.getAlias() : "");
            }
            if ("template".equals(key)) {
                if (unsPo.getModelId() != null) {
                    UnsPo template = templateMap.get(unsPo.getModelId());
                    dataList.add(template != null ? template.getPath() : "");
                } else {
                    dataList.add("");
                }
            }
            if ("fields".equals(key)) {
                dataList.add(field(unsPo.getFields()));
            }
            if ("server.host".equals(key)) {
                if (config != null && config.getServer() != null) {
                    dataList.add(config.getServer().getHost());
                } else {
                    dataList.add("");
                }
            }
            if ("server.port".equals(key)) {
                if (config != null && config.getServer() != null) {
                    dataList.add(config.getServer().getPort());
                } else {
                    dataList.add("");
                }
            }
            if ("server.location".equals(key)) {
                if (config != null && config.getServer() != null) {
                    dataList.add(config.getServer().getLocation());
                } else {
                    dataList.add("");
                }
            }
            if ("pollRate.value".equals(key)) {
                if (config != null && config.getPollRate() != null) {
                    dataList.add(config.getPollRate().getValue());
                } else {
                    dataList.add("");
                }
            }
            if ("pollRate.unit".equals(key)) {
                if (config != null && config.getPollRate() != null) {
                    dataList.add(config.getPollRate().getUnit());
                } else {
                    dataList.add("");
                }
            }
            if ("description".equals(key)) {
                dataList.add(StringUtils.isNotBlank(unsPo.getDescription()) ? unsPo.getDescription() : "");
            }
            if ("autoFlow".equals(key)) {
                if (unsPo.getWithFlags() != null) {
                    dataList.add(Constants.withFlow(unsPo.getWithFlags()) ? "TRUE" : "FALSE");
                } else {
                    dataList.add("");
                }
            }
            if ("autoDashboard".equals(key)) {
                if (unsPo.getWithFlags() != null) {
                    dataList.add(Constants.withDashBoard(unsPo.getWithFlags()) ? "TRUE" : "FALSE");
                } else {
                    dataList.add("");
                }
            }
            if ("persistence".equals(key)) {
                if (unsPo.getWithFlags() != null) {
                    dataList.add(Constants.withSave2db(unsPo.getWithFlags()) ? "TRUE" : "FALSE");
                } else {
                    dataList.add("");
                }
            }
            if ("label".equals(key)) {
                Set<String> labels = unsLabelNamesMap.get(unsPo.getId());
                if (CollectionUtils.isNotEmpty(labels)) {
                    dataList.add(StringUtils.join(labels, ','));
                } else {
                    dataList.add("");
                }
            }
        }
        return dataList;
    }

    /**
     * Opcda协议文件
     * @param unsPo
     * @return
     */
    private static List<Object> createRowForOpcda(UnsPo unsPo, Map<String, UnsPo> templateMap, Map<String, Set<String>> unsLabelNamesMap) {
        List<Object> dataList = new ArrayList<>(TIMESERIES_OPCDA_INDEX.size());
        OpcDAConfigDTO config = null;
        if (StringUtils.isNotBlank(unsPo.getProtocol())) {
            config = JsonUtil.fromJson(unsPo.getProtocol(), OpcDAConfigDTO.class);
        }
        for(String key : TIMESERIES_OPCDA_INDEX) {
            if ("topic".equals(key)) {
                dataList.add(StringUtils.isNotBlank(unsPo.getPath()) ? unsPo.getPath() : "");
            }
            if ("alias".equals(key)) {
                dataList.add(unsPo.getAlias() != null ? unsPo.getAlias() : "");
            }
            if ("template".equals(key)) {
                if (unsPo.getModelId() != null) {
                    UnsPo template = templateMap.get(unsPo.getModelId());
                    dataList.add(template != null ? template.getPath() : "");
                } else {
                    dataList.add("");
                }
            }
            if ("fields".equals(key)) {
                dataList.add(field(unsPo.getFields()));
            }
            if ("server.host".equals(key)) {
                if (config != null && config.getServer() != null && config.getServer().getHost() != null) {
                    dataList.add(config.getServer().getHost());
                } else {
                    dataList.add("");
                }
            }
            if ("server.domain".equals(key)) {
                if (config != null && config.getServer() != null && config.getServer().getDomain() != null) {
                    dataList.add(config.getServer().getDomain());
                } else {
                    dataList.add("");
                }
            }
            if ("server.account".equals(key)) {
                if (config != null && config.getServer() != null && config.getServer().getAccount() != null) {
                    dataList.add(config.getServer().getAccount());
                } else {
                    dataList.add("");
                }
            }
            if ("server.password".equals(key)) {
                if (config != null && config.getServer() != null && config.getServer().getPassword() != null) {
                    dataList.add(config.getServer().getPassword());
                } else {
                    dataList.add("");
                }
            }
            if ("server.clsid".equals(key)) {
                if (config != null && config.getServer() != null && config.getServer().getClsid() != null) {
                    dataList.add(config.getServer().getClsid());
                } else {
                    dataList.add("");
                }
            }
            if ("server.timeout".equals(key)) {
                if (config != null && config.getServer() != null) {
                    dataList.add(config.getServer().getTimeout());
                } else {
                    dataList.add("");
                }
            }
            if ("pollRate.value".equals(key)) {
                if (config != null && config.getPollRate() != null) {
                    dataList.add(config.getPollRate().getValue());
                } else {
                    dataList.add("");
                }
            }
            if ("pollRate.unit".equals(key)) {
                if (config != null && config.getPollRate() != null) {
                    dataList.add(config.getPollRate().getUnit());
                } else {
                    dataList.add("");
                }
            }
            if ("description".equals(key)) {
                dataList.add(StringUtils.isNotBlank(unsPo.getDescription()) ? unsPo.getDescription() : "");
            }
            if ("autoFlow".equals(key)) {
                if (unsPo.getWithFlags() != null) {
                    dataList.add(Constants.withFlow(unsPo.getWithFlags()) ? "TRUE" : "FALSE");
                } else {
                    dataList.add("");
                }
            }
            if ("autoDashboard".equals(key)) {
                if (unsPo.getWithFlags() != null) {
                    dataList.add(Constants.withDashBoard(unsPo.getWithFlags()) ? "TRUE" : "FALSE");
                } else {
                    dataList.add("");
                }
            }
            if ("persistence".equals(key)) {
                if (unsPo.getWithFlags() != null) {
                    dataList.add(Constants.withSave2db(unsPo.getWithFlags()) ? "TRUE" : "FALSE");
                } else {
                    dataList.add("");
                }
            }
            if ("label".equals(key)) {
                Set<String> labels = unsLabelNamesMap.get(unsPo.getId());
                if (CollectionUtils.isNotEmpty(labels)) {
                    dataList.add(StringUtils.join(labels, ','));
                } else {
                    dataList.add("");
                }
            }
        }
        return dataList;
    }

    /**
     * mqtt协议文件
     * @param unsPo
     * @return
     */
    private static List<Object> createRowForMqtt(UnsPo unsPo, Map<String, UnsPo> templateMap, Map<String, Set<String>> unsLabelNamesMap) {
        List<Object> dataList = new ArrayList<>(MQTT_INDEX.size());
        MqttConfigDTO config = null;
        if (StringUtils.isNotBlank(unsPo.getProtocol())) {
            config = JsonUtil.fromJson(unsPo.getProtocol(), MqttConfigDTO.class);
        }
        for(String key : MQTT_INDEX) {
            if ("topic".equals(key)) {
                dataList.add(StringUtils.isNotBlank(unsPo.getPath()) ? unsPo.getPath() : "");
            }
            if ("alias".equals(key)) {
                dataList.add(unsPo.getAlias() != null ? unsPo.getAlias() : "");
            }
            if ("template".equals(key)) {
                if (unsPo.getModelId() != null) {
                    UnsPo template = templateMap.get(unsPo.getModelId());
                    dataList.add(template != null ? template.getPath() : "");
                } else {
                    dataList.add("");
                }
            }
            if ("fields".equals(key)) {
                dataList.add(field(unsPo.getFields()));
            }
            if ("inputTopic".equals(key)) {
                if (config != null && config.getInputTopic() != null) {
                    dataList.add(config.getInputTopic());
                } else {
                    dataList.add("");
                }
            }
            if ("inputName".equals(key)) {
                if (config != null && config.getInputName() != null) {
                    dataList.add(config.getInputName());
                } else {
                    dataList.add("");
                }
            }
            if ("serverName".equals(key)) {
                if (config != null && StringUtils.isNotBlank(config.getServerName())) {
                    dataList.add(config.getServerName());
                } else {
                    dataList.add("");
                }
            }
            if ("server.host".equals(key)) {
                if (config != null && config.getServer() != null) {
                    dataList.add(config.getServer().getHost());
                } else {
                    dataList.add("");
                }
            }
            if ("server.port".equals(key)) {
                if (config != null && config.getServer() != null) {
                    dataList.add(config.getServer().getPort());
                } else {
                    dataList.add("");
                }
            }
            if ("description".equals(key)) {
                dataList.add(StringUtils.isNotBlank(unsPo.getDescription()) ? unsPo.getDescription() : "");
            }
            if ("autoFlow".equals(key)) {
                if (unsPo.getWithFlags() != null) {
                    dataList.add(Constants.withFlow(unsPo.getWithFlags()) ? "TRUE" : "FALSE");
                } else {
                    dataList.add("");
                }
            }
            if ("autoDashboard".equals(key)) {
                if (unsPo.getWithFlags() != null) {
                    dataList.add(Constants.withDashBoard(unsPo.getWithFlags()) ? "TRUE" : "FALSE");
                } else {
                    dataList.add("");
                }
            }
            if ("persistence".equals(key)) {
                if (unsPo.getWithFlags() != null) {
                    dataList.add(Constants.withSave2db(unsPo.getWithFlags()) ? "TRUE" : "FALSE");
                } else {
                    dataList.add("");
                }
            }
            if ("label".equals(key)) {
                Set<String> labels = unsLabelNamesMap.get(unsPo.getId());
                if (CollectionUtils.isNotEmpty(labels)) {
                    dataList.add(StringUtils.join(labels, ','));
                } else {
                    dataList.add("");
                }
            }
        }
        return dataList;
    }

    /**
     * RestAPI协议文件
     * @param unsPo
     * @return
     */
    private static List<Object> createRowForRestAPI(UnsPo unsPo, Map<String, UnsPo> templateMap, Map<String, Set<String>> unsLabelNamesMap) {
        if (StringUtils.equals("/rest/post", unsPo.getPath())) {
            System.out.println();
        }
        List<Object> dataList = new ArrayList<>(RELATION_RESTAPI_INDEX.size());
        RestConfigDTO config = null;
        if (StringUtils.isNotBlank(unsPo.getProtocol())) {
            config = JsonUtil.fromJson(unsPo.getProtocol(), RestConfigDTO.class);
        }
        for(String key : RELATION_RESTAPI_INDEX) {
            if ("topic".equals(key)) {
                dataList.add(StringUtils.isNotBlank(unsPo.getPath()) ? unsPo.getPath() : "");
            }
            if ("alias".equals(key)) {
                dataList.add(unsPo.getAlias() != null ? unsPo.getAlias() : "");
            }
            if ("template".equals(key)) {
                if (unsPo.getModelId() != null) {
                    UnsPo template = templateMap.get(unsPo.getModelId());
                    dataList.add(template != null ? template.getPath() : "");
                } else {
                    dataList.add("");
                }
            }
            if ("fields".equals(key)) {
                dataList.add(field(unsPo.getFields()));
            }
            if ("dataPath".equals(key)) {
                dataList.add(StringUtils.isNotBlank(unsPo.getDataPath()) ? unsPo.getDataPath() : "");
            }
            if ("serverName".equals(key)) {
                if (config != null && StringUtils.isNotBlank(config.getServerName())) {
                    dataList.add(config.getServerName());
                } else {
                    dataList.add("");
                }
            }
            if ("method".equals(key)) {
                if (config != null && StringUtils.isNotBlank(config.getMethod())) {
                    dataList.add(config.getMethod());
                } else {
                    dataList.add("");
                }
            }
            if ("syncRate.unit".equals(key)) {
                if (config != null && config.getSyncRate() != null) {
                    dataList.add(config.getSyncRate().getUnit());
                } else {
                    dataList.add("");
                }
            }
            if ("syncRate.value".equals(key)) {
                if (config != null && config.getSyncRate() != null) {
                    dataList.add(config.getSyncRate().getValue());
                } else {
                    dataList.add("");
                }
            }
            if ("pageDef.start.key".equals(key)) {
                if (config != null && config.getPageDef() != null) {
                    dataList.add(config.getPageDef().getStart().getValue());
                } else {
                    dataList.add("");
                }
            }
            if ("pageDef.offset.key".equals(key)) {
                if (config != null && config.getPageDef() != null) {
                    dataList.add(config.getPageDef().getOffset().getValue());
                } else {
                    dataList.add("");
                }
            }
            if ("fullUrl".equals(key)) {
                if (config != null && StringUtils.isNotBlank(config.gainFullUrl())) {
                    dataList.add(config.gainFullUrl());
                } else {
                    dataList.add("");
                }
            }
            if ("headers".equals(key)) {
                if (config != null && config.getHeaders() != null) {
                    dataList.add(config.getHeaders());
                } else {
                    dataList.add("");
                }
            }
            if ("body".equals(key)) {
                if (config != null && StringUtils.isNotBlank(config.getBody()) && !StringUtils.equals(config.getBody(), "{}")) {
                    dataList.add(config.getBody());
                } else {
                    dataList.add("");
                }
            }
            if ("description".equals(key)) {
                dataList.add(StringUtils.isNotBlank(unsPo.getDescription()) ? unsPo.getDescription() : "");
            }
            if ("autoFlow".equals(key)) {
                if (unsPo.getWithFlags() != null) {
                    dataList.add(Constants.withFlow(unsPo.getWithFlags()) ? "TRUE" : "FALSE");
                } else {
                    dataList.add("");
                }
            }
            if ("autoDashboard".equals(key)) {
                if (unsPo.getWithFlags() != null) {
                    dataList.add(Constants.withDashBoard(unsPo.getWithFlags()) ? "TRUE" : "FALSE");
                } else {
                    dataList.add("");
                }
            }
            if ("persistence".equals(key)) {
                if (unsPo.getWithFlags() != null) {
                    dataList.add(Constants.withSave2db(unsPo.getWithFlags()) ? "TRUE" : "FALSE");
                } else {
                    dataList.add("");
                }
            }
            if ("label".equals(key)) {
                Set<String> labels = unsLabelNamesMap.get(unsPo.getId());
                if (CollectionUtils.isNotEmpty(labels)) {
                    dataList.add(StringUtils.join(labels, ','));
                } else {
                    dataList.add("");
                }
            }
        }
        return dataList;
    }

    /**
     * 空白关系实例
     * @param unsPo
     * @return
     */
    private static List<Object> createRowForBlankRelationInstance(UnsPo unsPo, Map<String, UnsPo> templateMap, Map<String, Set<String>> unsLabelNamesMap) {
        List<Object> dataList = new ArrayList<>(RELATION_INDEX.size());
        for(String key : RELATION_INDEX) {
            if ("topic".equals(key)) {
                dataList.add(StringUtils.isNotBlank(unsPo.getPath()) ? unsPo.getPath() : "");
            }
            if ("alias".equals(key)) {
                dataList.add(unsPo.getAlias() != null ? unsPo.getAlias() : "");
            }
            if ("template".equals(key)) {
                if (unsPo.getModelId() != null) {
                    UnsPo template = templateMap.get(unsPo.getModelId());
                    dataList.add(template != null ? template.getPath() : "");
                } else {
                    dataList.add("");
                }
            }
            if ("fields".equals(key)) {
                dataList.add(field(unsPo.getFields()));
            }
            if ("description".equals(key)) {
                dataList.add(StringUtils.isNotBlank(unsPo.getDescription()) ? unsPo.getDescription() : "");
            }
            if ("autoFlow".equals(key)) {
                if (unsPo.getWithFlags() != null) {
                    dataList.add(Constants.withFlow(unsPo.getWithFlags()) ? "TRUE" : "FALSE");
                } else {
                    dataList.add("");
                }
            }
            if ("autoDashboard".equals(key)) {
                if (unsPo.getWithFlags() != null) {
                    dataList.add(Constants.withDashBoard(unsPo.getWithFlags()) ? "TRUE" : "FALSE");
                } else {
                    dataList.add("");
                }
            }
            if ("persistence".equals(key)) {
                if (unsPo.getWithFlags() != null) {
                    dataList.add(Constants.withSave2db(unsPo.getWithFlags()) ? "TRUE" : "FALSE");
                } else {
                    dataList.add("");
                }
            }
            if ("label".equals(key)) {
                Set<String> labels = unsLabelNamesMap.get(unsPo.getId());
                if (CollectionUtils.isNotEmpty(labels)) {
                    dataList.add(StringUtils.join(labels, ','));
                } else {
                    dataList.add("");
                }
            }
        }
        return dataList;
    }

    public static List<Object> fillForModbusInstance(List<Object> dataList) {
        for (int i = dataList.size(); i < TIMESERIES_MODBUS_INDEX.size(); i++) {
            dataList.add("");
        }
        return dataList;
    }

    public static List<Object> fillForOpcuaInstance(List<Object> dataList) {
        for (int i = dataList.size(); i < TIMESERIES_OPCUA_INDEX.size(); i++) {
            dataList.add("");
        }
        return dataList;
    }

    public static List<Object> fillForRestAPIInstance(List<Object> dataList) {
        for (int i = dataList.size(); i < RELATION_RESTAPI_INDEX.size(); i++) {
            dataList.add("");
        }
        return dataList;
    }

    public static List<Object> fillForWrite(List<Object> dataList, ExcelTypeEnum excelType) {
        List<String> head = null;
        switch (excelType) {
            case TIMESERIES_OPCUA:head = TIMESERIES_OPCUA_INDEX;break;
            case TIMESERIES_OPCDA:head = TIMESERIES_OPCDA_INDEX;break;
            case TIMESERIES_MODBUS:head = TIMESERIES_MODBUS_INDEX;break;
            case RELATION_RESTAPI:head = RELATION_RESTAPI_INDEX;break;
            case TIMESERIES:head = TIMESERIES_INDEX;break;
            case RELATION:head = RELATION_INDEX;break;
            case Folder:head = FOLDER_INDEX;break;
            case Template:head = TEMPLATE_INDEX;break;
            case TIMESERIES_MQTT:head = MQTT_INDEX;break;
            case RELATION_MQTT:head = MQTT_INDEX;break;
        }
        for (int i = dataList.size(); i < head.size(); i++) {
            dataList.add("");
        }
        return dataList;
    }

    /**
     * 文件夹
     * @param unsPo
     * @return
     */
    private static List<Object> createRowForTemplate(UnsPo unsPo) {
        List<Object> dataList = new ArrayList<>(TEMPLATE_INDEX.size());
        for(String key : TEMPLATE_INDEX) {
            if ("name".equals(key)) {
                dataList.add(unsPo.getPath());
            }
            if ("fields".equals(key)) {
                dataList.add(field(unsPo.getFields()));
            }
            if ("description".equals(key)) {
                dataList.add(StringUtils.isNotBlank(unsPo.getDescription()) ? unsPo.getDescription() : "");
            }
        }
        return dataList;
    }

    private static String field(String fieldStr) {
        if (StringUtils.isNotBlank(fieldStr) && !StringUtils.equals(fieldStr, "null") && !StringUtils.equals(fieldStr, "[]")) {
            List<FieldDefineVo> list = JsonUtil.fromJson(fieldStr, new TypeReference<List<FieldDefineVo>>() {
            }.getType());
            FieldDefineVo[] field = list.stream().filter(f -> !f.getName().startsWith(Constants.SYSTEM_FIELD_PREV)).toArray(n -> new FieldDefineVo[n]);
            if (field != null && field.length > 0) {
                return JsonUtil.toJson(field);
            }
        }
        return "";
    }

    @Data
    @AllArgsConstructor
    public static class RowWrapper {
        private IOTProtocol protocol;
        private List<Object> dataList;

    }
}

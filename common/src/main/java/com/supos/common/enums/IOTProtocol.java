package com.supos.common.enums;

import com.supos.common.dto.protocol.*;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * iot协议，包括modbus、opcua、rest、mqtt
 */
@Getter
public enum IOTProtocol {

    REST("rest", "Api", RestConfigDTO.class),

    RELATION("relation", "Relation", null),

    MODBUS("modbus", "Modbus", ModbusConfigDTO.class),

    MQTT("mqtt", "MQTT", MqttConfigDTO.class),

    ICMP("icmp", "ICMP", ICMPConfigDTO.class),

    OPC_UA("opcua", "OPC UA", OpcUAConfigDTO.class),

    OPC_DA("opcda", "OPC DA", OpcDAConfigDTO.class),

    UNKNOWN("unknown", "Unknown", null);

    private String name;

    private String displayName;
    public final Class<?> protocolClass;

    IOTProtocol(String name, String displayName, Class protocolClass) {
        this.name = name;
        this.displayName = displayName;
        this.protocolClass = protocolClass;
    }

    public static IOTProtocol getByName(String name) {
        IOTProtocol iotProtocol = nameMap.get(name);
        return iotProtocol == null ? UNKNOWN : iotProtocol;
    }

    private static final Map<String, IOTProtocol> nameMap = new HashMap<>(4);

    static {
        for (IOTProtocol v : IOTProtocol.values()) {
            nameMap.put(v.name.toLowerCase(), v);
        }
    }

    public static List<KeyValuePair<String>> listSerialProtocol() {
        List<KeyValuePair<String>> ps = new ArrayList<>();
        ps.add(new KeyValuePair<>(MODBUS.name, MODBUS.displayName));
        ps.add(new KeyValuePair<>(OPC_UA.name, OPC_UA.displayName));
        ps.add(new KeyValuePair<>(OPC_DA.name, OPC_DA.displayName));
        ps.add(new KeyValuePair<>(MQTT.name, MQTT.displayName));
        ps.add(new KeyValuePair<>(ICMP.name, ICMP.displayName));
        return ps;
    }

    public static boolean contains(String name) {
        return nameMap.containsKey(name);
    }
}

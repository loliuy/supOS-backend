package com.supos.adpter.nodered.service.register;

import com.supos.adpter.nodered.service.enums.IdentifiersInterface;
import com.supos.common.annotation.ProtocolIdentifierProvider;
import com.supos.common.dto.protocol.BaseConfigDTO;
import com.supos.common.dto.protocol.BaseServerConfigDTO;
import com.supos.common.enums.IOTProtocol;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;

import java.util.Map;

@Slf4j
public class IdentifiersContext {

    private Map<Object, Class<?>> instanceMap;

    private ApplicationContext applicationContext;

    public IdentifiersContext(Map<Object, Class<?>> instanceMap, ApplicationContext applicationContext) {
        this.instanceMap = instanceMap;
        this.applicationContext = applicationContext;
    }

    public IdentifiersInterface<BaseConfigDTO> getInstance(IOTProtocol protocol) {
        if (protocol == null) {
            return null;
        }
        Class<?> aClass = instanceMap.get(protocol);
        if (aClass == null) {
            log.error("当前协议的枚举类不存在，协议类型： {}", protocol.name());
            return null;
        }
        return (IdentifiersInterface<BaseConfigDTO>)applicationContext.getBean(aClass);
    }

}

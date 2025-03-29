package com.supos.adpter.eventflow.service.enums;

import com.supos.common.dto.protocol.BaseConfigDTO;
import com.supos.common.dto.protocol.ProtocolTagEnums;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public interface IdentifiersInterface <T extends BaseConfigDTO> {

    /**
     * key=server address, value=Tag list
     */
    Map<String, List<ProtocolTagEnums>> TAG_ENUMS_CACHE = new ConcurrentHashMap<>();

    Collection<ProtocolTagEnums> listTags(T config, String topic);

}

package com.supos.adpter.nodered.service.enums;

import com.supos.common.annotation.ProtocolIdentifierProvider;
import com.supos.common.dto.protocol.OpcDAConfigDTO;
import com.supos.common.dto.protocol.ProtocolTagEnums;
import com.supos.common.enums.IOTProtocol;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 枚举opcda位号
 */
@Slf4j
@Service("opcDANodeIdentifiers")
@ProtocolIdentifierProvider(IOTProtocol.OPC_DA)
public class OpcDANodeIdentifiers implements IdentifiersInterface<OpcDAConfigDTO> {


    @Override
    public List<ProtocolTagEnums> listTags(OpcDAConfigDTO serverConfig, String topic) {

        return Collections.emptyList();
    }

}

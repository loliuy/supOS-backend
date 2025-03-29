package com.supos.common.dto.protocol;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.springframework.util.StringUtils;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ModbusConfigDTO extends BaseConfigDTO {

    private String serverName;

    @NotEmpty
    private String unitId;

    /**
     * FC1->Coil
     * FC2->Input
     * FC3->HoldingRegister
     * FC4->InputRegister
     */
    @NotEmpty
    private String fc;
    @NotEmpty
    private String address;

    private String quantity;

    @NotNull
    @Valid
    private RateDTO pollRate;

    @NotNull(message = "nodered.protocol.modbus.server.empty")
    private ModbusServerConfigDTO server;

    public String getServerName() {
        if (StringUtils.hasText(serverName)) {
            return serverName;
        } else if (server == null) {
            return null;
        }
        return server.getHost() + ":" + server.getPort() + "@" + unitId;
    }
}

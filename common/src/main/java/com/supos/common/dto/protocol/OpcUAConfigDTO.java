package com.supos.common.dto.protocol;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.springframework.util.StringUtils;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpcUAConfigDTO extends BaseConfigDTO {

    private String serverName;

    @NotNull
    @Valid
    private RateDTO pollRate;

    @Valid
    private OpcuaServerConfigDTO server;

    public String getServerName() {
        if (StringUtils.hasText(serverName)) {
            return serverName;
        }
        return server.getEndpoint();
    }



}

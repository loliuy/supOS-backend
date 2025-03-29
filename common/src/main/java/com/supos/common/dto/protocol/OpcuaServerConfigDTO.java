package com.supos.common.dto.protocol;

import lombok.Data;
import org.springframework.util.StringUtils;

@Data
public class OpcuaServerConfigDTO extends BaseServerConfigDTO {

    private String location;

    public String getEndpoint() {
        String lt = "";
        if (StringUtils.hasText(location)) {
            if (location.startsWith("/")) {
                lt = location;
            } else {
                lt += "/" + location;
            }
        }
        String endpoint = String.format("opc.tcp://%s:%s%s", super.getHost(), super.getPort(), lt);
        return endpoint.trim();
    }

}

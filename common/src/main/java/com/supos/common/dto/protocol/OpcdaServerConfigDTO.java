package com.supos.common.dto.protocol;

import lombok.Data;

@Data
public class OpcdaServerConfigDTO extends BaseServerConfigDTO {

    private String domain;

    private String account;

    private String password;

    private String clsid;

    private long timeout; // ms

    public long getTimeout() {
        return timeout > 0 ? timeout : 5000;
    }


}

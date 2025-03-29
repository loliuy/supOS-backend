package com.supos.adpter.nodered.vo;

import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
@Valid
public class AddProtocolRequestVO implements Serializable {

    private static final long serialVersionUID = 1l;
    //
    @NotEmpty(message = "name can't be empty")
    private String name;

    // 协议配置
    @NotEmpty(message = "client config can't be empty")
    private List<FieldObject> clientConfig;

    // server配置
    @NotEmpty(message = "server config can't be empty")
    private List<FieldObject> serverConfig;

    @NotEmpty(message = "server endpoint can't be empty")
    private String serverConn;

}

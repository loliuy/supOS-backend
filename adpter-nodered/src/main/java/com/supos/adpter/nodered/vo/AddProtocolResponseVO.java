package com.supos.adpter.nodered.vo;

import com.supos.common.dto.protocol.KeyValuePair;
import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.List;
import java.util.Set;

@Data
@Valid
public class AddProtocolResponseVO implements Serializable {

    private static final long serialVersionUID = 1l;

    // server字段名称
    private String serverConn;

    // 选中的server名称
    private String serverName;



}

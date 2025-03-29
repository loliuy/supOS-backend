package com.supos.adpter.eventflow.vo;

import jakarta.validation.Valid;
import lombok.Data;

import java.io.Serializable;

@Data
@Valid
public class AddProtocolResponseVO implements Serializable {

    private static final long serialVersionUID = 1l;

    // server字段名称
    private String serverConn;

    // 选中的server名称
    private String serverName;



}

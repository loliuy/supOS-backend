package com.supos.adpter.eventflow.vo;

import com.alibaba.fastjson.JSONArray;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serializable;

@Data
@Valid
public class DeployFlowRequestVO implements Serializable {

    private static final long serialVersionUID = 1l;

    // 主键ID
    @NotEmpty(message = "nodered.flowId.empty")
    private String id;

    private JSONArray flows;

}

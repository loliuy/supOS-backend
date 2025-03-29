package com.supos.adpter.nodered.vo;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.List;

@Data
@Valid
public class DeployFlowRequestVO implements Serializable {

    private static final long serialVersionUID = 1l;

    // 主键ID
    @NotEmpty(message = "nodered.flowId.empty")
    private String id;

    private JSONArray flows;

}

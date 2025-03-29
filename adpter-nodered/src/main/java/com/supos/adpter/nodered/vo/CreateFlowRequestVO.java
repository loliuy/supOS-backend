package com.supos.adpter.nodered.vo;

import com.alibaba.fastjson.JSONArray;
import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotEmpty;
import java.io.Serializable;

@Data
@Valid
public class CreateFlowRequestVO implements Serializable {

    private static final long serialVersionUID = 1l;

    @NotEmpty(message = "flow name can't be empty")
    private String flowName;

    private String description;

    private String template;

}

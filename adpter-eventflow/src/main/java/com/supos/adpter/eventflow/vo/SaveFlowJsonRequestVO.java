package com.supos.adpter.eventflow.vo;

import com.alibaba.fastjson.JSONArray;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serializable;

@Data
@Valid
public class SaveFlowJsonRequestVO implements Serializable {

    private static final long serialVersionUID = 1l;

    @NotEmpty(message = "id can't be empty")
    private String id;

    private JSONArray flows;

}

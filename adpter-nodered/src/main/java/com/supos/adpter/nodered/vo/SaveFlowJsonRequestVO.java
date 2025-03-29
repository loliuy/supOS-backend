package com.supos.adpter.nodered.vo;

import com.alibaba.fastjson.JSONArray;
import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.io.Serializable;

@Data
@Valid
public class SaveFlowJsonRequestVO implements Serializable {

    private static final long serialVersionUID = 1l;

    @NotEmpty(message = "id can't be empty")
    private String id;

    private JSONArray flows;

}

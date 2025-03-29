package com.supos.adpter.eventflow.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serializable;

@Data
@Valid
public class UpdateFlowRequestVO implements Serializable {

    private static final long serialVersionUID = 1l;

    @NotEmpty(message = "id can't be empty")
    private String id;

    @NotEmpty(message = "name can't be empty")
    private String flowName;

    private String description;
}

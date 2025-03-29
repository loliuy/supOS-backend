package com.supos.adpter.eventflow.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

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

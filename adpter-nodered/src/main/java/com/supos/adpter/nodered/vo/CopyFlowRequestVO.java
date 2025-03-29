package com.supos.adpter.nodered.vo;

import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.io.Serializable;

@Data
@Valid
public class CopyFlowRequestVO implements Serializable {

    private static final long serialVersionUID = 1l;

    @NotEmpty(message = "sourceId can't be empty")
    private String sourceId;

    @NotEmpty(message = "flow name can't be empty")
    private String flowName;

    private String description;

    private String template;

}

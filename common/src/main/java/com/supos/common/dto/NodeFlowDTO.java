package com.supos.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NodeFlowDTO implements Serializable {

    private static final long serialVersionUID = 1l;

    private String id;

    private String flowName;

    private String flowId;

    private String description;

    private String flowStatus;

    private String template;

}

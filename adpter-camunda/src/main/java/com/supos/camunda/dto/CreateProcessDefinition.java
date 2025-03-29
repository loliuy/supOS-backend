package com.supos.camunda.dto;

import lombok.Data;

@Data
public class CreateProcessDefinition {

    private Long id;

    private String name;

    private String description;
}

package com.supos.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import jakarta.validation.constraints.NotEmpty;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InstanceField {
    @NotEmpty
    String topic;
    @NotEmpty
    String field;

    public InstanceField() {
    }

    public InstanceField(String topic, String field) {
        this.topic = topic;
        this.field = field;
    }
}
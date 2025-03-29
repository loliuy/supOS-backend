package com.supos.adpter.eventflow.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class BatchQueryRequest {

    @NotNull
    private List<String> topics;
}

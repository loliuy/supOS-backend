package com.supos.app.vo;

import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.io.Serializable;

@Data
@Valid
public class CreateHomepageVO implements Serializable {

    private static final long serialVersionUID = 1l;

    @NotEmpty(message = "app name can't be empty")
    private String appName;

    private long htmlId;
}

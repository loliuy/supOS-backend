package com.supos.app.vo;

import com.supos.app.util.Constants;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Valid
public class CreateAppVO implements Serializable {

    private static final long serialVersionUID = 1l;

    @NotEmpty(message = "nodered.parameter.name.not.empty")
//    @Pattern(regexp = Constants.NAME_REG, message = "nodered.parameter.name.reg.invalid")
    private String name;
}

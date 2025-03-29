package com.supos.app.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AppHtmlVO implements Serializable {

    private static final long serialVersionUID = 1l;

    private String appName;

    private String url;

    private String content;


}

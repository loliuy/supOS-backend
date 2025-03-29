package com.supos.adpter.kong.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class MarkRouteRequestVO implements Serializable {

    private static final long serialVersionUID = 1l;

    private String name;

    private String url;
}

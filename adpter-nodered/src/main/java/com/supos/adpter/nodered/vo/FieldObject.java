package com.supos.adpter.nodered.vo;

import lombok.Data;

@Data
public class FieldObject {

    private String name;

    private String type;

    private Object value;

    private Object defaultValue;

    private boolean required;

}

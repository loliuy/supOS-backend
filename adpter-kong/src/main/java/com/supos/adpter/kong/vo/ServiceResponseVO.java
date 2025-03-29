package com.supos.adpter.kong.vo;

import lombok.Data;

import java.util.List;

@Data
public class ServiceResponseVO {

    private String id;

    private String name;

    private List<String> tags;
}
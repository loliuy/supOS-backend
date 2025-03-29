package com.supos.app.dao.po;


import lombok.Data;

import java.util.Date;

@Data
public class AppPO {

    private String name;

    private String showName;

    private String description;

    private Date createTime;
}

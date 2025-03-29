package com.supos.app.dao.po;


import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
public class AppHtmlPO {

    private long id;

    private String appName;

    private String path;

    private String fileName;

    private int homepage;

//    private Date createTime;

}

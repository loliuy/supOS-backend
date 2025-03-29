package com.supos.common.dto.grafana;

import cn.hutool.core.codec.Base64;
import lombok.Data;

/**
 * data source
 * @author xinwangji@supos.com
 * @date 2024/10/12 9:31
 * @description
 */
@Data
public class GrafanaDataSourceDto {

    private String uid;

    /**
     * 名称
     */
    private String name;

    /**
     * 地址
     */
    private String url;

    /**
     * 用户名
     */
    private String user;

    /**
     * 密码
     */
    private String password;

    /**
     * 认证凭据
     * base(user:password)
     */
    private String basicAuth;

    public void createBasicAuth(){
        String auth = Base64.encode(this.getUser()+ ":" + this.getPassword());
        this.setBasicAuth(auth);
    }
}

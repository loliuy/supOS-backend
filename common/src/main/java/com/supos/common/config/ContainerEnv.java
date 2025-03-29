package com.supos.common.config;

import lombok.Data;

@Data
public class ContainerEnv {

    private boolean serviceIsShow;

    private String serviceLogo;

    private String serviceDescription;

    private String serviceRedirectUrl;

    private String serviceAccount;

    private String servicePassword;
}
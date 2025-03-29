package com.supos.gateway.model.dto;

import lombok.Data;

/**
 * @author xinwangji@supos.com
 * @date 2024/11/21 18:46
 * @description
 */
@Data
public class AccessTokenDto {

    private String access_token;

    private int expires_in;

    private String id_token;



}

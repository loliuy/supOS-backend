package com.supos.common.dto.auth;

import lombok.Data;

@Data
public class AccessTokenDto {

    private String accessToken;

    private Integer expiresIn;

    private Long refreshExpiresIn;

    private String refreshToken;
}

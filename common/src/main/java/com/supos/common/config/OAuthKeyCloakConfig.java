package com.supos.common.config;

import com.supos.common.utils.KeycloakUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author xinwangji@supos.com
 * @date 2024/11/21 10:11
 * @description
 */
@Data
@Component
@AllArgsConstructor
@NoArgsConstructor
@ConfigurationProperties(prefix = "oauth.keycloak")
public class OAuthKeyCloakConfig {

    private String realm;

    private String clientName;

    private String clientId;

    private String clientSecret;

    private String authorizationGrantType;

    private String redirectUri;

    private String issuerUri;

    private String suposHome;

    private long refreshTokenTime;

    public String getRedirectUri() {
        return KeycloakUtil.removePortIfDefault(redirectUri);
    }
}

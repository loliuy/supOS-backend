package com.supos.common.utils;

import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.supos.common.config.OAuthKeyCloakConfig;
import com.supos.common.dto.auth.AccessTokenDto;
import com.supos.common.exception.BuzException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class KeycloakUtil {

    @Resource
    private OAuthKeyCloakConfig keyCloakConfig;

    private String getApiUrl(){
        return keyCloakConfig.getIssuerUri() + "/realms/" + keyCloakConfig.getRealm() + "/protocol/openid-connect";
    }

    private String getAdminApiUrl(){
        return keyCloakConfig.getIssuerUri() + "/admin/realms/" + keyCloakConfig.getRealm();
    }


    public HttpResponse userinfo(String accessToken){
        //获取用户信息
        String url = getApiUrl() + "/userinfo";
        log.info(">>>>>>>>>>>>Keycloak userinfo URL：{}",url);
        HttpResponse response = HttpUtil.createRequest(Method.GET,url).bearerAuth(accessToken).execute();
        log.info(">>>>>>>>>>>>Keycloak userinfo response code：{},body:{}",response.getStatus(),response.body());
        return response;
    }


    public HttpResponse refreshToken(String refreshToken){
        String url = getApiUrl() +"/token";
        Map<String,Object> params = new HashMap<>();
        params.put("grant_type","refresh_token");
        params.put("refresh_token",refreshToken);
        params.put("client_id",keyCloakConfig.getClientId());
        params.put("client_secret",keyCloakConfig.getClientSecret());
        log.info(">>>>>>>>>>>>Keycloak refreshToken URL：{},params:{}",url, JSON.toJSON(params));
        HttpResponse response = HttpRequest.post(url)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .form(params)
                .timeout(5000)
                .execute();
        log.info(">>>>>>>>>>>>Keycloak refreshToken response code：{},body:{}",response.getStatus(),response.body());
        return response;
    }

    public JSONObject getKeyCloakToken(String code){
        String url = getApiUrl() +"/token";
        Map<String,Object> params = new HashMap<>();
        params.put("grant_type",keyCloakConfig.getAuthorizationGrantType());
        params.put("code",code);
        params.put("redirect_uri",keyCloakConfig.getRedirectUri());
        params.put("client_id",keyCloakConfig.getClientId());
        params.put("client_secret",keyCloakConfig.getClientSecret());
        log.info(">>>>>>>>>>>>Keycloak get token URL：{},params:{}",url,JSON.toJSON(params));
        HttpResponse response = HttpRequest.post(url)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .form(params)
                .timeout(5000)
                .execute();
        log.info(">>>>>>>>>>>>Keycloak get token response code：{},body:{}",response.getStatus(),response.body());
        if (200 != response.getStatus()){
            return null;
        }
        return JSON.parseObject(response.body());
    }


    public String getAdminToken(){
        String url = keyCloakConfig.getIssuerUri() + "/realms/master/protocol/openid-connect/token";
        Map<String,Object> params = new HashMap<>();
        params.put("username","admin");
        params.put("password","admin");
        params.put("grant_type","password");
        params.put("client_id","admin-cli");
        log.info(">>>>>>>>>>>>Keycloak getAdminToken URL：{},params:{}",url, JSON.toJSON(params));
        HttpResponse response = HttpRequest.post(url)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .form(params)
                .timeout(5000)
                .execute();
        log.info(">>>>>>>>>>>>Keycloak getAdminToken response code：{},body:{}",response.getStatus(),response.body());
        if(200 != response.getStatus()){
            throw new RuntimeException("Keycloak getAdminToken 失败");
        }
        return JSON.parseObject(response.body()).getString("access_token");
    }


    public boolean deleteUser(String id){
        String url = getAdminApiUrl() +"/users/" + id;
        log.info(">>>>>>>>>>>>Keycloak deleteUser URL：{}",url);
        HttpResponse response = HttpRequest.delete(url).bearerAuth(getAdminToken()).execute();
        log.info(">>>>>>>>>>>>Keycloak deleteUser response code：{},body:{}",response.getStatus(),response.body());
        return 204 == response.getStatus();
    }

    public boolean resetPwd(String userId,String password){
        String url = getAdminApiUrl() +"/users/"+userId+"/reset-password";
        JSONObject params = new JSONObject();
        params.put("type","password");
        params.put("temporary",false);
        params.put("value",password);
        log.info(">>>>>>>>>>>>Keycloak resetPwd URL：{}",url);
        HttpResponse response = HttpRequest.put(url).bearerAuth(getAdminToken()).body(params.toJSONString()).execute();
        log.info(">>>>>>>>>>>>Keycloak resetPwd response code：{},body:{}",response.getStatus(),response.body());
        return 204 == response.getStatus();
    }

    public boolean updateUser(String userId,JSONObject params){
        String url = getAdminApiUrl() + "/users/" + userId;
        log.info(">>>>>>>>>>>>Keycloak updateUser URL：{},params:{}",url,params.toString());
        HttpResponse response = HttpRequest.put(url).bearerAuth(getAdminToken()).body(params.toJSONString()).execute();
        log.info(">>>>>>>>>>>>Keycloak updateUser response code：{},body:{}",response.getStatus(),response.body());
        return 204 == response.getStatus();
    }

    public String getClientId(){
        String url =  getAdminApiUrl() + "/clients?clientId=" + keyCloakConfig.getClientId();
        log.info(">>>>>>>>>>>>Keycloak getClientId URL：{}",url);
        HttpResponse response = HttpRequest.get(url).bearerAuth(getAdminToken()).execute();
        log.info(">>>>>>>>>>>>Keycloak getClientId response code：{},body:{}",response.getStatus(),response.body());
        if(200 != response.getStatus()){
            throw new RuntimeException("Keycloak getClientId 失败");
        }
        JSONArray array = JSON.parseArray(response.body());
        if (null == array){
            throw new RuntimeException("Keycloak getClientId 失败");
        }
        return array.getJSONObject(0).getString("id");
    }

    public boolean setRole(String userId,Integer type,JSONArray params){
        String url = getAdminApiUrl() + "/users/" + userId + "/role-mappings/clients/" + getClientId();
        log.info(">>>>>>>>>>>>Keycloak setRole URL：{},type:{},params:{}",url,type,params.toString());
        HttpRequest httpRequest = null;
        if (1 == type){
            httpRequest = HttpUtil.createRequest(Method.POST,url);
        } else {
            httpRequest = HttpUtil.createRequest(Method.DELETE,url);
        }
        HttpResponse response = httpRequest.bearerAuth(getAdminToken()).body(params.toJSONString()).execute();
        log.info(">>>>>>>>>>>>Keycloak setRole response code：{},body:{}",response.getStatus(),response.body());
        return 204 == response.getStatus();
    }

    public String createUser(String body){
        String url = getAdminApiUrl() + "/users";
        log.info(">>>>>>>>>>>>Keycloak createUser URL：{},body:{}",url,body);
        HttpResponse response = HttpRequest.post(url).bearerAuth(getAdminToken()).body(body).execute();
        log.info(">>>>>>>>>>>>Keycloak createUser response code：{},body:{}",response.getStatus(),response.body());
        //http://keycloak:8080/admin/realms/supos/users/a09d1625-3244-4cfb-ae86-f226caa44121
        String location = response.header("Location");
        if (response.getStatus() == 201 && StrUtil.isNotBlank(location)){
            return StrUtil.subAfter(location,"/",true);
        } else if (response.getStatus() == 409){
            throw new BuzException("user.create.already.exists");
        } else {
            throw new BuzException("user.create.failed");
        }
    }

    public static String removePortIfDefault(String url) {
        // 使用正则表达式匹配并处理端口号为80或443的情况
        String regex = "(http://[^:/]+)(:(80|443))?(/.*)?";

        // 如果匹配到端口为80或443的情况
        if (ReUtil.isMatch(regex, url)) {
            // 使用正则替换，去掉端口号为80或443的部分
            return ReUtil.replaceAll(url, regex, "$1$4");
        }

        // 如果没有匹配，返回原始 URL
        return url;
    }

    public AccessTokenDto login(String username,String password){
        String url = getApiUrl() +"/token";
        Map<String,Object> params = new HashMap<>();
        params.put("grant_type","password");
        params.put("username",username);
        params.put("password",password);
        params.put("client_id",keyCloakConfig.getClientId());
        params.put("client_secret",keyCloakConfig.getClientSecret());
        log.info(">>>>>>>>>>>>Keycloak login URL：{},params:{}",url, JSON.toJSON(params));
        HttpResponse response = HttpRequest.post(url)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .form(params)
                .timeout(5000)
                .execute();
        log.info(">>>>>>>>>>>>Keycloak login response code：{},body:{}",response.getStatus(),response.body());
        if (response.getStatus() == 200){
            return JSON.parseObject(response.body(), AccessTokenDto.class);
        }
        return null;
    }
}

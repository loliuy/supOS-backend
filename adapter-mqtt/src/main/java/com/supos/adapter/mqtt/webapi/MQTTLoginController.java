package com.supos.adapter.mqtt.webapi;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.supos.common.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;

@Slf4j
@RestController
public class MQTTLoginController {

    // 创建缓存
    private long updateTime;
    String repsBody;

    @GetMapping(value = "/supos/inter-api/login", produces = "application/json")
    public String login(HttpServletRequest request, HttpServletResponse response) {
        String res = emqxLogin();
        Map body = JsonUtil.fromJson(res, Map.class);
        response.setHeader("Authorization", "Bearer " + body.get("token"));
        String fwUrl = "http://emqx:18083/" + request.getRequestURI();
        HttpRequest.get(fwUrl).execute();
        return res;
    }

    private synchronized String emqxLogin() {
        long now = System.currentTimeMillis();
        if (repsBody == null || now - updateTime > 5 * 60 * 1000) {
            HttpResponse response = HttpRequest.post("http://emqx:18083/api/v5/login")
                    .body("{\"username\":\"admin\",\"password\":\"public\"}", "application/json")
                    .execute();
            String body = response.body();
            log.info("emqxLogin response: {}", body);
            repsBody = body;
            updateTime = now;
        }
        return repsBody;
    }

}

package com.supos.webhook.task;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.supos.webhook.dao.po.WebhookPO;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
@Slf4j
public class WebhookTask<T> implements Runnable {

    private WebhookPO subscriber;

    private T data;

    @Override
    public void run() {
        doSend();
    }

    /**
     * 同步发送
     * @return boolean success or false
     */
    public boolean syncRun() {
        return doSend();
    }

    private boolean doSend() {
        // 发送http请求
        log.info("==>webhook async send data to {},  url is {}, event is {}", subscriber.getName(), subscriber.getUrl(), subscriber.getSubscribeEvent());
        try {
            HttpRequest clientRequest = HttpUtil.createPost(subscriber.getUrl());
            clientRequest.addHeaders(buildHeaders());
            clientRequest.timeout(5000);
            Map<String, Object> bodyMap = new HashMap<>();
            bodyMap.put("data", data);
            clientRequest.body(JSON.toJSONString(bodyMap));
            HttpResponse response = clientRequest.execute();
            log.info("<==webhook response: {}", response.body());
            return response.getStatus() == 200;
        } catch (Exception e) {
            log.error("webhook send error", e);
        }
        return false;
    }

    private Map<String, String> buildHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        if (StringUtils.hasText(subscriber.getHeaders())) {
            JSONArray headerArray = JSON.parseArray(subscriber.getHeaders());
            for (int i = 0; i < headerArray.size(); i++) {
                JSONObject jo = headerArray.getJSONObject(i);
                headers.put(jo.getString("key"), jo.getString("value"));
            }
        }
        return headers;
    }
}

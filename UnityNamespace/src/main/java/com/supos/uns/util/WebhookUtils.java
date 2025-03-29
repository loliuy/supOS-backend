package com.supos.uns.util;

import com.supos.uns.dao.po.UnsPo;
import com.supos.webhook.dto.WebhookDataDTO;

import java.util.ArrayList;
import java.util.List;

public class WebhookUtils {

    public static List<WebhookDataDTO> transfer(List<UnsPo> unsList) {
        List<WebhookDataDTO> wdds = new ArrayList<>();
        for (UnsPo uns : unsList) {
            // 只推送实例数据
            if (uns.getPathType().intValue() == 2) {
                WebhookDataDTO wdd = new WebhookDataDTO();
                wdd.setFields(uns.getFields());
                wdd.setConfig(uns.getProtocol());
                wdd.setTopic(uns.getPath());
                wdd.setType(uns.getDataType());
                wdd.setAlias(uns.getAlias());
                wdds.add(wdd);
            }
        }
        return wdds;
    }

    public static List<WebhookDataDTO> transfer(List<UnsPo> unsList, String fields) {
        List<WebhookDataDTO> wdds = new ArrayList<>();
        for (UnsPo uns : unsList) {
            // 只推送实例数据
            if (uns.getPathType().intValue() == 2) {
                WebhookDataDTO wdd = new WebhookDataDTO();
                wdd.setFields(fields);
                wdd.setConfig(uns.getProtocol());
                wdd.setTopic(uns.getPath());
                wdd.setType(uns.getDataType());
                wdd.setAlias(uns.getAlias());
                wdds.add(wdd);
            }
        }
        return wdds;
    }
}

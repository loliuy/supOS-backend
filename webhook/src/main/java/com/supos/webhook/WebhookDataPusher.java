package com.supos.webhook;

import cn.hutool.core.thread.ThreadUtil;
import com.supos.common.enums.WebhookSubscribeEvent;
import com.supos.webhook.dao.mapper.WebhookMapper;
import com.supos.webhook.dao.po.WebhookPO;
import com.supos.webhook.dto.WebhookDataDTO;
import com.supos.webhook.task.WebhookTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class WebhookDataPusher {

    @Autowired
    private WebhookMapper webhookMapper;

    /**
     * 将数据变化发送给订阅方
     * @param event 订阅事件
     * @param data 订阅数据
     * @param serial 是否串行发送
     * @return
     */
    public void push(WebhookSubscribeEvent event, List<WebhookDataDTO> data, boolean serial) {
        List<WebhookPO> webhooks = webhookMapper.selectByEvent(event.name());
        if (webhooks.isEmpty()) {
            log.info("no subscriber for event {}, end webhook", event.name());
            return;
        }
        for (WebhookPO webhook : webhooks) {
            if (serial) {
                new WebhookTask<>(webhook, data).syncRun();
            } else {
                ThreadUtil.execAsync(new WebhookTask<>(webhook, data));
            }
        }
    }
}

package com.supos.uns.service;

import com.supos.common.annotation.Description;
import org.junit.jupiter.api.Order;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class DemoListener {

    @EventListener(classes = DemoEvent.class)
    @Order(9)
    @Description("测试1")
    void onDemoEvent(DemoEvent event) {
        System.out.println("执行1");
    }
}

package com.supos.uns.service;

import com.supos.common.annotation.Description;
import org.junit.jupiter.api.Order;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class DemoListener2 {

    @EventListener(classes = DemoEvent.class)
    @Order(15)
    @Description("测试2")
    void onDemoEvent(DemoEvent event) {
        System.out.println("执行2");
    }
}

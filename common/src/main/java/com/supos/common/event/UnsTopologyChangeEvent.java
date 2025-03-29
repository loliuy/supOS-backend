package com.supos.common.event;

import org.springframework.context.ApplicationEvent;

/**
 * @author sunlifang
 * @version 1.0
 * @description: TODO
 * @date 2024/12/11 19:26
 */
public class UnsTopologyChangeEvent extends ApplicationEvent {

    public UnsTopologyChangeEvent(Object source) {
        super(source);
    }
}

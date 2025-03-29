package com.supos.common.event;

import com.supos.common.dto.CreateTopicDto;
import org.springframework.context.ApplicationEvent;

import java.util.List;

public class UpdateCalcInstanceEvent extends ApplicationEvent {
    public final List<CreateTopicDto> topics;

    public UpdateCalcInstanceEvent(Object source, List<CreateTopicDto> topics) {
        super(source);
        this.topics = topics;
    }
}

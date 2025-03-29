package com.supos.common.event;

import com.supos.common.SrcJdbcType;
import com.supos.common.dto.CreateTopicDto;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;
import java.util.Map;

public class InitTopicsEvent extends ApplicationEvent {
    public final Map<SrcJdbcType, List<CreateTopicDto>> topics;

    public InitTopicsEvent(Object source,  Map<SrcJdbcType,  List<CreateTopicDto>> topics) {
        super(source);
        this.topics = topics;
    }
}

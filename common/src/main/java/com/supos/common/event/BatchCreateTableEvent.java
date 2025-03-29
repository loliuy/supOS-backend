package com.supos.common.event;

import com.supos.common.SrcJdbcType;
import com.supos.common.dto.CreateTopicDto;
import com.supos.common.event.multicaster.EventStatusAware;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;
import org.springframework.util.StringUtils;

import java.util.Map;

public class BatchCreateTableEvent extends ApplicationEvent implements EventStatusAware {
    public final boolean fromImport;

    public String flowName;
    public final Map<SrcJdbcType, CreateTopicDto[]> topics;

    public BatchCreateTableEvent(Object source, boolean fromImport, Map<SrcJdbcType, CreateTopicDto[]> topics) {
        super(source);
        this.fromImport = fromImport;
        this.topics = topics;
    }

    public BatchCreateTableEvent setFlowName(String flowName) {
        if (StringUtils.hasText(flowName)) {
            this.flowName = flowName;
        }
        return this;
    }

    @Setter
    private EventStatusAware delegateAware;

    @Override
    public void beforeEvent(int totalListeners, int i, String listenerName) {
        if (delegateAware != null) {
            delegateAware.beforeEvent(totalListeners, i, listenerName);
        }
    }

    @Override
    public void afterEvent(int totalListeners, int i, String listenerName, Throwable err) {
        if (delegateAware != null) {
            delegateAware.afterEvent(totalListeners, i, listenerName, err);
        }
    }
}

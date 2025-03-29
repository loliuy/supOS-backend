package com.supos.common.event;

import com.supos.common.SrcJdbcType;
import com.supos.common.dto.UpdateFieldDto;
import com.supos.common.event.multicaster.EventStatusAware;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

public class BatchUpdateTableEvent extends ApplicationEvent implements EventStatusAware {

    public final UpdateFieldDto[] topics;

    public SrcJdbcType jdbcType;

    public BatchUpdateTableEvent(Object source, SrcJdbcType jdbcType, UpdateFieldDto[] topics) {
        super(source);
        this.topics = topics;
        this.jdbcType = jdbcType;
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

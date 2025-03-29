package com.supos.common.event;

import com.supos.common.dto.grafana.DashboardDto;
import org.springframework.context.ApplicationEvent;

import java.util.List;

/**
 * 创建grafana dashboard 数据库记录
 */
public class BatchCreateDashboardEvent extends ApplicationEvent {

    public final List<DashboardDto> dashboardDtoList;

    public BatchCreateDashboardEvent(Object source, List<DashboardDto> dashboardDtoList) {
        super(source);
        this.dashboardDtoList = dashboardDtoList;
    }
}

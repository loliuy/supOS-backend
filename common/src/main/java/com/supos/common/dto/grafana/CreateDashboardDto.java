package com.supos.common.dto.grafana;

import lombok.Data;

/**
 * @author xinwangji@supos.com
 * @date 2024/10/29 10:08
 * @description
 */
@Data
public class CreateDashboardDto {

    private String uid;

    private String title;

    private String message;
}

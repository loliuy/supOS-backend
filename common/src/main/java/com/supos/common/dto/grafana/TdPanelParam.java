package com.supos.common.dto.grafana;

import lombok.Data;

/**
 * @author xinwangji@supos.com
 * @date 2024/10/12 10:53
 * @description
 */
@Data
public class TdPanelParam {


    private Integer id;

    private String title;

    private String dataSourceUid;

    private String columns;

    private String schema;

    private String tableName;

    private int gridPosX;

}

package com.supos.uns.vo;

import lombok.Data;

@Data
public class DbTableInfoVo {
    String dataSourceId;
    String databaseType;
    String databaseName;
    String schemaName;
    String tableName;
}

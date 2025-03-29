package com.supos.common.dto;

import lombok.Data;

@Data
public class SimpleUnsInstance {
    String topic;
    String alias;
    String tableName;
    Integer dataType;
    boolean removeTableWhenDeleteInstance;

    public SimpleUnsInstance(String topic, String alias, String tableName, Integer dataType, boolean removeTableWhenDeleteInstance) {
        this.topic = topic;
        this.alias = alias;
        this.tableName = tableName;
        this.dataType = dataType;
        this.removeTableWhenDeleteInstance = removeTableWhenDeleteInstance;
    }

    public String getTableName() {
        if (tableName != null) {
            return tableName;
        }
        if (alias != null) {
            return alias;
        }
        return topic;
    }
}

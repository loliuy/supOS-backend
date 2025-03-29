package com.supos.common.dto;

import com.supos.common.utils.JsonUtil;
import lombok.Data;

@Data
public class StreamInfo {
    public static final int STATUS_RUNNING = 1;
    public static final int STATUS_PAUSED = 2;
    String name;// 流计算名称

    String sql;
    String targetTable;// 目标表
    int status; // 0--未知, 1--在运行, 2--已暂停

    public StreamInfo() {
    }

    public StreamInfo(String name, String targetTable, int status) {
        this.name = name;
        this.targetTable = targetTable;
        this.status = status;
    }

    public String toSting() {
        return JsonUtil.toJson(this);
    }


}

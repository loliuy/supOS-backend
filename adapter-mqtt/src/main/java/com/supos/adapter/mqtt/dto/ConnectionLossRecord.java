package com.supos.adapter.mqtt.dto;

import cn.hutool.core.bean.BeanUtil;
import com.supos.adapter.mqtt.util.DateUtil;
import lombok.Data;

@Data
public class ConnectionLossRecord implements Cloneable {
    Long firstLossTime;
    Long lastLossTime;
    int reconnectRetryCount;
    Long lastReconnectTime;
    Long lastReconnectOkTime;
    String lastCause;
    long errorCount;

    public void update(Throwable lossErr) {
        long now = System.currentTimeMillis();
        if (firstLossTime == null) {
            firstLossTime = now;
        }
        lastLossTime = now;
        String msg = lossErr.getMessage();
        if (msg == null && lossErr.getCause() != null) {
            msg = lossErr.getCause().getMessage();
        }
        lastCause = msg;
        errorCount++;
    }

    @Override
    public ConnectionLossRecord clone() {
        try {
            return (ConnectionLossRecord) super.clone();
        } catch (CloneNotSupportedException e) {
            ConnectionLossRecord msg = new ConnectionLossRecord();
            BeanUtil.copyProperties(this, msg);
            return msg;
        }
    }

    public String getFirstLossTimeStr() {
        return DateUtil.dateStr(firstLossTime);
    }

    public String getLastLossTimeStr() {
        return DateUtil.dateStr(lastLossTime);
    }

    public String getLastReconnectTimeStr() {
        return DateUtil.dateStr(lastReconnectTime);
    }

    public String getLastReconnectOkTimeStr() {
        return DateUtil.dateStr(lastReconnectOkTime);
    }

    public void lastReconnect() {
        lastReconnectOkTime = System.currentTimeMillis();
        reconnectRetryCount++;
    }

    public void lastReconnectSuccess() {
        lastReconnectOkTime = System.currentTimeMillis();
    }


}

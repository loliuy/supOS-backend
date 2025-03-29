package com.supos.uns.bo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supos.common.utils.I18nUtils;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RunningStatus {
    int code;
    String msg;
    String errTipFile;
    Integer n;
    Integer i;
    String task;

    Long spendMills;
    Boolean finished;
    Double progress; // 进度 [0,100]

    public RunningStatus(int code, String msg) {
        this.code = code;
        this.msg = I18nUtils.getMessage(msg);
        this.finished = true;
    }

    public RunningStatus(int code, String msg, String errTipFile) {
        this.code = code;
        this.msg = I18nUtils.getMessage(msg);
        this.errTipFile = errTipFile;
        this.finished = true;
    }

    public RunningStatus(int n, int i, String task, String msg) {
        this.n = n;
        this.i = i;
        this.task = task;
        this.msg = msg;
    }

    public RunningStatus setSpendMills(Long spend) {
        this.spendMills = spend;
        Integer n = this.n;
        if (n != null && n > 0 && i != null) {
            progress = ((int) (1000 * i.doubleValue() / n.doubleValue())) / 10.0;
        }
        return this;
    }

    public RunningStatus setProgress(Double progress) {
        this.progress = progress;
        return this;
    }

    public RunningStatus setCode(int code) {
        this.code = code;
        return this;
    }

    public RunningStatus setTask(String task) {
        this.task = task;
        return this;
    }
}

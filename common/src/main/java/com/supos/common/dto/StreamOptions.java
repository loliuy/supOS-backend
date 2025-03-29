package com.supos.common.dto;

import com.supos.common.annotation.StreamOptionsValidator;
import lombok.Data;

@Data
@StreamOptionsValidator
public class StreamOptions {
    StreamWindowOptions window;

    String whereCondition;

    String havingCondition;

    String trigger; // AT_ONCE,WINDOW_CLOSE,FORCE_WINDOW_CLOSE, MAX_DELAY time

    String waterMark;// 如 100s,100m,100h,3d

    String deleteMark;

    Boolean ignoreExpired;
    Boolean fillHistory;
    Boolean ignoreUpdate;

    String startTime;// 格式形如：2020-01-30

    String endTime;


}

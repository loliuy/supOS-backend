package com.supos.common.enums;

import com.supos.common.Constants;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author xinwangji@supos.com
 * @date 2024/11/13 11:27
 * @description
 */
@Getter
@AllArgsConstructor
public enum ExcelTypeEnum {


    RELATION("relation", Constants.RELATION_TYPE, 2),
    RELATION_RESTAPI("relation-restAPI", Constants.RELATION_TYPE, 4),
    RELATION_MQTT("relation-mqtt", Constants.RELATION_TYPE, 5),
    TIMESERIES("TimeSeries", Constants.TIME_SEQUENCE_TYPE, 3),
    TIMESERIES_MODBUS("TimeSeries-modbus", Constants.TIME_SEQUENCE_TYPE, 6),
    TIMESERIES_OPCUA("TimeSeries-opcua", Constants.TIME_SEQUENCE_TYPE, 7),
    TIMESERIES_MQTT("TimeSeries-mqtt", Constants.TIME_SEQUENCE_TYPE, 8),
    TIMESERIES_OPCDA("TimeSeries-opcda", Constants.TIME_SEQUENCE_TYPE, 9),

    CalcSTREAM("calc_stream", Constants.CALCULATION_HIST_TYPE, -1),
    CalcRealTime("calc_realtime", Constants.CALCULATION_REAL_TYPE, -1),

    Template("Template", null, 0),
    Folder("Folder", null, 1),
    Label("Label", null, -1),
    ERROR("error", 0, -1);

    private final String code;

    private final Integer dataType;// 1--时序库 2--关系库

    /**
     * 批次序号
     */
    private final Integer index;

    public static ExcelTypeEnum valueOfCode(String code) {
        for (ExcelTypeEnum obj : ExcelTypeEnum.values()) {
            if (obj.code.equals(code)) {
                return obj;
            }
        }
        return ERROR;
    }

    public static List<ExcelTypeEnum> sort() {
        return Arrays.stream(ExcelTypeEnum.values()).sorted(Comparator.comparing(ExcelTypeEnum::getIndex)).collect(Collectors.toList());
    }

    public static void main(String[] args) {
        System.out.println(ExcelTypeEnum.sort());
    }
}

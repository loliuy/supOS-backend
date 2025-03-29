package com.supos.common;

public enum NodeType {
    Path(0), Model(1), Instance(2), InstanceForCalc(3),InstanceForTimeseries(4),AlarmRule(5);

    public final int code;

    private NodeType(int code) {
        this.code = code;
    }

    public static NodeType valueOf(int code) {
        switch (code) {
            case 0:
                return Path;
            case 1:
                return Model;
            case 2:
                return Instance;
            case 3:
                return InstanceForCalc;
            case 4:
                return InstanceForTimeseries;
            case 5:
                return AlarmRule;
        }
        return Path;
    }
}

package com.supos.common;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum SrcJdbcType {
    TdEngine(1, "tdengine-datasource", "td", Constants.TIME_SEQUENCE_TYPE),
    Postgresql(2, "postgresql", "pg", Constants.RELATION_TYPE),
    TimeScaleDB(3, "postgresql", "tmsc", Constants.TIME_SEQUENCE_TYPE),
    ;

    public final int id;

    public final String dataSrcType;
    public final String alias;
    public final int typeCode;//1--时序，2--关系

    SrcJdbcType(int id, String dataSrcType, String alias, int typeCode) {
        this.id = id;
        this.dataSrcType = dataSrcType;
        this.alias = alias;
        this.typeCode = typeCode;
    }

    public static SrcJdbcType getById(Integer id) {
        return idMap.get(id);
    }

    public static final Map<Integer, SrcJdbcType> idMap;

    static {
        HashMap<Integer, SrcJdbcType> map = new HashMap<>(4);
        for (SrcJdbcType v : SrcJdbcType.values()) {
            map.put(v.id, v);
        }
        idMap = Collections.unmodifiableMap(map);
    }

    public String toString() {
        return alias;
    }

}

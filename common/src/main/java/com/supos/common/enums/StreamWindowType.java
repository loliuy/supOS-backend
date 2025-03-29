package com.supos.common.enums;

import com.supos.common.dto.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public enum StreamWindowType {
    SESSION(() -> new StreamWindowOptionsSession()),
    INTERVAL(() -> new StreamWindowOptionsInterval()),
    EVENT_WINDOW(() -> new StreamWindowOptionsEventWindow()),
    STATE_WINDOW(() -> new StreamWindowOptionsStateWindow()),
    COUNT_WINDOW(() -> new StreamWindowOptionsCountWindow());

    private final Supplier<Object> optionsSupplier;

    StreamWindowType(Supplier<Object> optionsSupplier) {
        this.optionsSupplier = optionsSupplier;
    }

    public Object buildOptions() {
        return optionsSupplier.get();
    }

    public static StreamWindowType of(String name) {
        return nameMap.get(name);
    }

    static final Map<String, StreamWindowType> nameMap = new HashMap<>(8);

    static {
        for (StreamWindowType v : StreamWindowType.values()) {
            nameMap.put(v.name(), v);
        }
    }

}

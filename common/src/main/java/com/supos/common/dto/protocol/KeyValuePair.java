package com.supos.common.dto.protocol;

import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Data
public class KeyValuePair<T> {
    @NotEmpty
    private String key;
    private T value;

    public KeyValuePair(String key, T value) {
        this.key = key;
        this.value = value;
    }
}

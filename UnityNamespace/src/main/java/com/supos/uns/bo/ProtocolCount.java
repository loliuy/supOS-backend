package com.supos.uns.bo;

import lombok.Data;

import java.util.Objects;
@Data
public class ProtocolCount {
    String protocol;
    int count;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ProtocolCount that = (ProtocolCount) o;
        return count == that.count && Objects.equals(protocol, that.protocol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(protocol, count);
    }
}

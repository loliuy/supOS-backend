package com.supos.uns.bo;

import lombok.Data;

import java.util.Objects;
@Data
public class PathTypeCount {
    int pathType;
    int count;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        PathTypeCount that = (PathTypeCount) o;
        return pathType == that.pathType && count == that.count;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pathType, count);
    }
}

package com.supos.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supos.common.enums.FieldType;
import com.supos.common.utils.JsonUtil;
import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Data
@Valid
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FieldDefine implements Cloneable {
    @NotEmpty
    private String name;//字段名
    @NotNull
    private FieldType type;// 字段类型：int, long, float, string, boolean
    private Boolean unique;// 是否唯一约束
    private String index; // 对应的协议字段key
    private String displayName;//显式名
    private String remark;//备注
    private Integer maxLen;// 最大长度

    public boolean isUnique() {
        return unique != null && unique;
    }

    public FieldDefine() {
    }

    public FieldDefine(String name, FieldType type) {
        this.setName(name);
        this.type = type;
    }

    public FieldDefine(String name, FieldType type, boolean unique) {
        this.setName(name);
        this.unique = unique;
        this.type = type;
    }

    public FieldDefine(String name, FieldType type, String index) {
        this.setName(name);
        this.setIndex(index);
        this.type = type;
    }

    public FieldDefine(String name, FieldType type, Boolean unique, String index, String displayName, String remark) {
        this.setName(name);
        this.setIndex(index);
        this.type = type;
        this.unique = unique;
        this.displayName = displayName;
        this.remark = remark;
    }

    public void setName(String name) {
        this.name = name != null ? name.trim() : null;
    }

    public void setIndex(String index) {
        this.index = index != null ? index.trim() : null;
    }

    public String toString() {
        return JsonUtil.toJsonUseFields(this);
    }

    public FieldDefine clone() {
        try {
            return (FieldDefine) super.clone();
        } catch (CloneNotSupportedException e) {
            FieldDefine f = new FieldDefine();
            f.setName(name);
            f.setType(type);
            f.setUnique(unique);
            return f;
        }
    }
}

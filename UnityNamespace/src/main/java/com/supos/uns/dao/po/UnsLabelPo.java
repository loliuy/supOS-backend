package com.supos.uns.dao.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(UnsLabelPo.TABLE_NAME)
public class UnsLabelPo {

    public static final String TABLE_NAME = "uns_label";

    @TableId(type = IdType.AUTO)
    private Long id;

    private String labelName;

    private Date createAt;

    public UnsLabelPo(String labelName) {
        this.labelName = labelName;
    }
}

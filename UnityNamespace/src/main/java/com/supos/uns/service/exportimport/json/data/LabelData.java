package com.supos.uns.service.exportimport.json.data;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.supos.uns.service.exportimport.core.data.LabelDataBase;
import lombok.Data;

/**
 * @author sunlifang
 * @version 1.0
 * @description: LabelData
 * @date 2025/5/8 17:09
 */
@Data
public class LabelData extends LabelDataBase {

    @ExcelIgnore
    private String error;
}

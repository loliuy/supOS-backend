package com.supos.uns.service.exportimport.json.data;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.supos.uns.service.exportimport.core.data.TemplateDataBase;
import lombok.Data;

/**
 * @author sunlifang
 * @version 1.0
 * @description: TemplateData
 * @date 2025/5/8 16:24
 */
@Data
public class TemplateData extends TemplateDataBase {

    @ExcelIgnore
    private String error;
}

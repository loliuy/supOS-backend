package com.supos.uns.service.exportimport.core.data;

import com.alibaba.excel.annotation.ExcelProperty;
import com.supos.uns.service.exportimport.core.data.ExportImportData;
import lombok.Data;

/**
 * @author sunlifang
 * @version 1.0
 * @description: TemplateData
 * @date 2025/5/8 16:24
 */
@Data
public class TemplateDataBase implements ExportImportData {

    @ExcelProperty(index = 0)
    private String name;
    @ExcelProperty(index = 1)
    private String alias;
    @ExcelProperty(index = 2)
    private String fields;
    @ExcelProperty(index = 3)
    private String description;
}

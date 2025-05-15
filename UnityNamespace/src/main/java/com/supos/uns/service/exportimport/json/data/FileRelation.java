package com.supos.uns.service.exportimport.json.data;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.supos.uns.service.exportimport.core.data.FileRelationBase;
import lombok.Data;

/**
 * @author sunlifang
 * @version 1.0
 * @description: FileRelation
 * @date 2025/5/8 17:26
 */
@Data
public class FileRelation extends FileRelationBase {

    @ExcelIgnore
    private String error;
}

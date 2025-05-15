package com.supos.uns.service.exportimport.json.data;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.supos.uns.service.exportimport.core.data.FolderDataBase;
import lombok.Data;

/**
 * @author sunlifang
 * @version 1.0
 * @description: FolderData
 * @date 2025/5/8 17:14
 */
@Data
public class FolderData extends FolderDataBase {

    @ExcelIgnore
    private String error;
}

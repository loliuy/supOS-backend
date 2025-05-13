package com.supos.uns.service.exportimport.json.data;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.supos.uns.service.exportimport.core.data.FileTimeseriesBase;
import lombok.Data;

/**
 * @author sunlifang
 * @version 1.0
 * @description: FileTimeseries
 * @date 2025/5/8 17:19
 */
@Data
public class FileTimeseries extends FileTimeseriesBase {

    @ExcelIgnore
    private String error;
}

package com.supos.uns.service.exportimport.core.parser;

import com.supos.uns.service.exportimport.core.ExcelImportContext;
import com.supos.uns.service.exportimport.core.data.ExportImportData;

import java.util.Map;

/**
 * @author sunlifang
 * @version 1.0
 * @description: TODO
 * @date 2025/5/10 10:15
 */
public interface ParserAble {

    default void parseExcel(int batch, int index, Map<String, Object> dataMap, ExcelImportContext context){}

    default void parseJson(int batch, int index, ExportImportData data, ExcelImportContext context){}
}

package com.supos.uns.service.exportimport.core.parser;

import com.supos.uns.service.exportimport.core.ExcelImportContext;
import com.supos.uns.service.exportimport.core.data.ExportImportData;
import com.supos.uns.service.exportimport.json.data.LabelData;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/**
 * @author sunlifang
 * @version 1.0
 * @description: LabelParser
 * @date 2025/4/22 19:17
 */
public class LabelParser extends AbstractParser {

    @Override
    public void parseExcel(int batch, int index, Map<String, Object> dataMap, ExcelImportContext context) {
        if (isEmptyRow(dataMap)) {
            return;
        }
        String label = getString(dataMap, "name", "");
        if (StringUtils.isBlank(label)) {
            return;
        }
        context.addLabel(label);
    }

    @Override
    public void parseJson(int batch, int index, ExportImportData data, ExcelImportContext context) {
        if (data == null) {
            return;
        }
        LabelData labelData = (LabelData) data;
        String label = labelData.getName();
        if (StringUtils.isBlank(label)) {
            return;
        }
        context.addLabel(label);
    }
}

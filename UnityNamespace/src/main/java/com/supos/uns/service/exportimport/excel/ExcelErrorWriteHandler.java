package com.supos.uns.service.exportimport.excel;

import com.alibaba.excel.metadata.Head;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.write.handler.CellWriteHandler;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.metadata.holder.WriteTableHolder;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;

import java.util.List;

/**
 * @author sunlifang
 * @version 1.0
 * @description: ExcelErrorWriteHandler
 * @date 2025/5/8 20:52
 */
public class ExcelErrorWriteHandler implements CellWriteHandler {

    private CellStyle cellStyle;

    @Override
    public void afterCellDispose(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder, List<WriteCellData<?>> cellDataList, Cell cell, Head head, Integer relativeRowIndex, Boolean isHead) {
        String value = cell.getStringCellValue();
        if (StringUtils.startsWith(value, "Import Error:")) {
            Workbook workbook = writeSheetHolder.getSheet().getWorkbook();
            if (this.cellStyle == null) {
                CellStyle cellStyle = workbook.createCellStyle();
                cellStyle.setFillForegroundColor(IndexedColors.RED1.getIndex());
                cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                this.cellStyle = cellStyle;
            }

            cell.setCellStyle(this.cellStyle);
        }
    }
}

package com.supos.uns.service.exportimport.excel;

import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.metadata.CellExtra;
import com.alibaba.excel.metadata.data.ReadCellData;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.excel.read.metadata.holder.ReadHolder;
import com.alibaba.excel.read.metadata.holder.ReadSheetHolder;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.supos.common.enums.ExcelTypeEnum;
import com.supos.uns.util.ExportImportUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author sunlifang
 * @version 1.0
 * @description: ExcelRowErrorHandler
 * @date 2025/5/6 14:29
 */
@Slf4j
public class ExcelRowErrorHandler implements ReadListener<Map<Integer, String>> {

    private Map<Integer, Map<Integer, String>> error;

    private ExcelWriter excelWriter;

    private WriteSheet writeSheet;

    private List<Map<Integer, String>> dataList = new ArrayList<>(200);

    public ExcelRowErrorHandler(ExcelWriter excelWriter, WriteSheet writeSheet, Map<Integer, Map<Integer, String>> error) {
        this.error = error;
        this.excelWriter = excelWriter;
        this.writeSheet = writeSheet;
    }

    @Override
    public void onException(Exception exception, AnalysisContext context) throws Exception {
        ReadListener.super.onException(exception, context);
    }

    @Override
    public void invokeHead(Map<Integer, ReadCellData<?>> headMap, AnalysisContext context) {
        ReadListener.super.invokeHead(headMap, context);
    }

    @Override
    public void extra(CellExtra extra, AnalysisContext context) {
        ReadListener.super.extra(extra, context);
    }

    @Override
    public boolean hasNext(AnalysisContext context) {
        return ReadListener.super.hasNext(context);
    }

    @Override
    public void invoke(Map<Integer, String> data, AnalysisContext context) {
        ReadHolder readHolder =context.currentReadHolder();
        if (readHolder instanceof ReadSheetHolder) {
            ReadSheetHolder readSheetHolder = (ReadSheetHolder) readHolder;
            int sheetNo = readSheetHolder.getSheetNo();
            int rowIndex = readSheetHolder.getRowIndex();

            ExcelTypeEnum excelType = ExcelTypeEnum.valueOfIndex(sheetNo);
            switch (excelType) {
/*                case Explanation:
                    dataList.add(data);
                    if (dataList.size() % 500 == 0) {
                        write();
                    }
                    break;*/
                case Template:
                case Label:
                case Folder:
                case FILE_TIMESERIES:
                case FILE_RELATION:
/*                case FILE_CALCULATE:
                case FILE_AGGREGATION:
                case FILE_REFERENCE:*/
                    if (rowIndex < 4) {
                        return;
                    }
                    dataList.add(addError(sheetNo, rowIndex, data));
                    if (dataList.size() % 500 == 0) {
                        write();
                    }
                    break;
            }
        }
    }

    private Map<Integer, String> addError(int sheetNo, int rowIndex, Map<Integer, String> data) {
        Map<Integer, String> subErrorMap = error.get(sheetNo);
        if (subErrorMap != null) {
            String error = subErrorMap.get(rowIndex);
            if (StringUtils.isNotBlank(error)) {
                ExcelTypeEnum  excelType = ExcelTypeEnum.valueOfIndex(sheetNo);
                // 填充空单元格
                int errorIndex = ExportImportUtil.errorIndex(excelType);
                if (data.size() < errorIndex) {
                    for (int i = data.size(); i < errorIndex; i++) {
                        data.put(i, "");
                    }
                }
                data.put(ExportImportUtil.errorIndex(excelType), String.format("Import Error:%s", error));
            }
        }
        return data;
    }

    private void write() {
        excelWriter.write(dataList, writeSheet);
        dataList.clear();
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        excelWriter.write(dataList, writeSheet);
        dataList.clear();
    }
}

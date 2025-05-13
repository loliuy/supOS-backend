package com.supos.uns.service.exportimport.excel;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.supos.common.Constants;
import com.supos.common.dto.InstanceField;
import com.supos.common.enums.ExcelTypeEnum;
import com.supos.common.utils.ApplicationContextUtils;
import com.supos.uns.dao.po.UnsLabelPo;
import com.supos.uns.dao.po.UnsPo;
import com.supos.uns.service.UnsExcelService;
import com.supos.uns.service.UnsManagerService;
import com.supos.uns.service.exportimport.core.DataExporter;
import com.supos.uns.service.exportimport.core.ExcelExportContext;
import com.supos.uns.service.exportimport.core.ExportNode;
import com.supos.uns.service.exportimport.core.data.ExportImportData;
import com.supos.uns.util.ExportImportUtil;
import com.supos.uns.util.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.core.io.ClassPathResource;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author sunlifang
 * @version 1.0
 * @description: ExcelDataExporter
 * @date 2025/5/10 17:57
 */
@Slf4j
public class ExcelDataExporter extends DataExporter {

    private UnsManagerService unsManagerService;
    private UnsExcelService unsExcelService;

    public ExcelDataExporter() {
        this.unsManagerService = ApplicationContextUtils.getBean(UnsManagerService.class);
        this.unsExcelService = ApplicationContextUtils.getBean(UnsExcelService.class);
    }

    @Override
    public String exportData(ExcelExportContext context, List<ExportNode> exportFolderList, List<ExportNode> exportFileList, List<UnsLabelPo> labels) {
        try {
            String datePath = DateUtil.format(new Date(), "yyyyMMddHHmmss");
            String path = String.format("%s%s%s", Constants.EXCEL_ROOT, datePath, Constants.EXCEL_OUT_PATH);
            String targetPath = String.format("%s%s", FileUtils.getFileRootPath(), path);
            FileUtil.touch(targetPath);

            ExcelWriter excelWriter = EasyExcel.write(targetPath).withTemplate(new ClassPathResource(Constants.EXCEL_TEMPLATE_PATH).getInputStream()).build();
            //unsExcelService.writeExplanationRow(excelWriter);

            // 导出模板
            if (MapUtils.isNotEmpty(context.getTemplateMap())) {
                List<ExportImportData> templateDatas = context.getTemplateMap().values().stream().map(template -> ExportImportUtil.createRow(template, context).getExportImportData()).collect(Collectors.toList());
                writeRow(excelWriter, ExcelTypeEnum.Template, templateDatas);
            }

            // 导出标签
            if (CollectionUtils.isNotEmpty(labels)) {
                List<ExportImportData> labelDatas = labels.stream().map(label -> ExportImportUtil.createRow(label).getExportImportData()).collect(Collectors.toList());
                writeRow(excelWriter, ExcelTypeEnum.Label, labelDatas);
            }


            if (CollectionUtils.isNotEmpty(exportFolderList) || CollectionUtils.isNotEmpty(exportFileList)) {
                Map<ExcelTypeEnum, Set<String>> excelTypeItemMap = new HashMap<>();
                Map<ExcelTypeEnum, AtomicInteger> sheetRowMap = new HashMap<>();
                // 导出文件
                for (ExportNode file : exportFileList) {
                    UnsPo unsPo = file.getUnsPo();
                    ExportImportUtil.RowWrapper rowWrapper = ExportImportUtil.createRow(unsPo, context);
                    file.setRowWrapper(rowWrapper);
                }

/*                Map<Long, UnsPo> referFileIdMap = new HashMap<>();
                Map<String, UnsPo> referFileAliadMap = new HashMap<>();
                if (CollectionUtils.isNotEmpty(context.getRefers())) {
                    Set<Long> referFileIds = context.getRefers().stream().filter(f -> f.getId() != null).map(InstanceField::getId).collect(Collectors.toSet());
                    Set<String> referFileAliass = context.getRefers().stream().filter(f -> f.getAlias() != null).map(InstanceField::getAlias).collect(Collectors.toSet());

                    if (CollectionUtils.isNotEmpty(referFileIds)) {
                        List<UnsPo> referFilesByIds =unsManagerService.list(Wrappers.lambdaQuery(UnsPo.class).in(UnsPo::getId, referFileIds));

                        referFileIdMap.putAll(referFilesByIds.stream().collect(Collectors.toMap(UnsPo::getId, Function.identity(), (k1, k2) -> k2)));
                    }
                    if (CollectionUtils.isNotEmpty(referFileAliass)) {
                        List<UnsPo> referFilesByAliass =unsManagerService.list(Wrappers.lambdaQuery(UnsPo.class).in(UnsPo::getAlias, referFileAliass));

                        referFileAliadMap.putAll(referFilesByAliass.stream().collect(Collectors.toMap(UnsPo::getAlias, Function.identity(), (k1, k2) -> k2)));
                    }
                }*/

                Map<ExcelTypeEnum, List<ExportImportUtil.RowWrapper>> rowWrapperMap = exportFileList.stream().map(file -> {
                    ExportImportUtil.RowWrapper rowWrapper = file.getRowWrapper();
                    //rowWrapper.handleRefer(referFileIdMap, referFileAliadMap);
                    return rowWrapper;
                }).collect(Collectors.groupingBy(ExportImportUtil.RowWrapper::getExcelType));
                for (Map.Entry<ExcelTypeEnum, List<ExportImportUtil.RowWrapper>> e : rowWrapperMap.entrySet()) {
                    writeRow(excelWriter, e.getKey(), e.getValue().stream().map(ExportImportUtil.RowWrapper::getExportImportData).collect(Collectors.toList()));
                }

                // 导出文件夹
                if (CollectionUtils.isNotEmpty(exportFolderList)) {
                    List<ExportImportData> folderDatas = exportFolderList.stream().map(folder -> ExportImportUtil.createRow(folder.getUnsPo(), context).getExportImportData()).collect(Collectors.toList());
                    writeRow(excelWriter, ExcelTypeEnum.Folder, folderDatas);
                }

/*                List<ExcelTypeEnum> sortedExcelType = ExcelTypeEnum.sort();
                for (ExcelTypeEnum excelType : sortedExcelType) {
                    if (excelType.getIndex() < 0) {
                        continue;
                    }
                    if (CollectionUtils.isNotEmpty(excelTypeItemMap.get(excelType))) {
                        excelWriter.getWorkbook().setActiveSheet(excelType.getIndex());
                        break;
                    }
                }*/
            }

            excelWriter.finish();
            log.info("export success:{}", targetPath);
            return path;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void writeRow(ExcelWriter excelWriter, ExcelTypeEnum excelType, List<ExportImportData> dataList) {
        WriteSheet writeSheet = EasyExcel.writerSheet().relativeHeadRowIndex(0).sheetNo(excelType.getIndex()).build();

        excelWriter.write(dataList, writeSheet);
    }
}

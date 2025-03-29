package com.supos.uns.service;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.net.URLDecoder;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Lists;
import com.supos.common.Constants;
import com.supos.common.dto.CreateTopicDto;
import com.supos.common.dto.excel.ExcelFolderDto;
import com.supos.common.dto.excel.ExcelNameSpaceDto;
import com.supos.common.dto.FieldDefine;
import com.supos.common.dto.JsonResult;
import com.supos.common.dto.excel.ExcelTemplateDto;
import com.supos.common.enums.ExcelTypeEnum;
import com.supos.common.enums.IOTProtocol;
import com.supos.common.exception.BuzException;
import com.supos.common.utils.FieldUtils;
import com.supos.common.utils.I18nUtils;
import com.supos.common.utils.JsonMapConvertUtils;
import com.supos.common.utils.JsonUtil;
import com.supos.common.vo.FieldDefineVo;
import com.supos.uns.bo.RunningStatus;
import com.supos.uns.dao.po.UnsLabelPo;
import com.supos.uns.dao.po.UnsLabelRefPo;
import com.supos.uns.dao.po.UnsPo;
import com.supos.uns.util.FileUtils;
import com.supos.uns.vo.CreateTemplateVo;
import com.supos.uns.vo.ExportParam;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.BeanUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;


@Slf4j
@Service
public class UnsExcelService {

    @Resource
    private UnsManagerService unsManagerService;

    @Resource
    private UnsLabelService unsLabelService;

    @Resource
    private UnsLabelRefService unsLabelRefService;

    public void excelDownload(HttpServletResponse response, String path) {
        try {
            path = URLDecoder.decode(path, StandardCharsets.UTF_8);
            File file = new File(FileUtils.getFileRootPath(), path);
            if (!file.exists()) {
                throw new BuzException("uns.file.not.exist");
            }
            String datePath = DateUtil.format(new Date(), "yyyyMMddHHmmss");
            String fileName = String.format("%s%s%s", "namespace-", datePath, ".xlsx");
            FileUtils.downloadFile(response, fileName, new FileInputStream(file));
        } catch (Exception e) {
            log.error("文件下载失败", e);
            throw new BuzException(e.getMessage());
        }
    }

    static class LogWrapperConsumer implements Consumer<RunningStatus> {
        final Consumer<RunningStatus> target;
        Boolean finished;
        String lastTask;
        Double lastProgress;

        LogWrapperConsumer(Consumer<RunningStatus> target) {
            this.target = target;
        }

        @Override
        public void accept(RunningStatus runningStatus) {
            log.info("** status: {}", JSON.toJSONString(runningStatus));
            finished = runningStatus.getFinished();
            String task = runningStatus.getTask();
            if (task != null) {
                lastTask = task;
            }
            Double progress = runningStatus.getProgress();
            if (progress != null) {
                lastProgress = progress;
            }
            target.accept(runningStatus);
        }
    }

    public void asyncImport(File file ,Consumer<RunningStatus> consumer,boolean isAsync) {
        if (!file.exists()) {
            String message = I18nUtils.getMessage("uns.file.not.exist");
            consumer.accept(new RunningStatus(400, message));
            return;
        }

        int sheetIndex = 0;
        int skipRow = 4;
        ExcelReader reader = ExcelUtil.getReader(file, sheetIndex);
        int sheetCount = reader.getSheetCount();
        Map<String, String> excelCheckErrorMap = new HashMap<>(4);
        List<CreateTemplateVo> templateVoList = new LinkedList<>();
        Map<String, String[]> labelsMap = new HashMap<>();
        List<CreateTopicDto> topicList = new LinkedList<>();
        Map<String, CreateTopicDto> topicMap = new HashMap<>();
        Set<String> aliasInExcel = new HashSet<>();
        try {
            do {
                String sheetName = reader.getSheet().getSheetName();
                ExcelTypeEnum excelType = ExcelTypeEnum.valueOfCode(sheetName);
                if (ExcelTypeEnum.ERROR.equals(excelType)) {
                    String message = I18nUtils.getMessage("uns.import.template.error");
                    consumer.accept(new RunningStatus(400, message));
                    return;
                }
                reader.setIgnoreEmptyRow(false);
                List<Map<String, Object>> dataList = reader.read(0, skipRow, Integer.MAX_VALUE);
                if (CollectionUtils.isNotEmpty(dataList)) {
                    if (ExcelTypeEnum.Template == excelType) {
                        templateVoList.addAll(new TemplateParser().parseExcelDataList(sheetIndex, skipRow, dataList, excelCheckErrorMap));
                    } else if (ExcelTypeEnum.Folder == excelType) {
                        topicList.addAll(new FolderParser().parseExcelDataList(sheetIndex, skipRow, dataList, topicMap, labelsMap, excelCheckErrorMap, aliasInExcel));
                    } else {
                        topicList.addAll(new TopicParser().parseExcelDataList(sheetIndex, skipRow, excelType, dataList, topicMap, labelsMap, excelCheckErrorMap, aliasInExcel));
                    }
                }

                sheetIndex++;
                if (sheetIndex < sheetCount) {
                    reader.setSheet(sheetIndex);
                }
            } while (sheetIndex < sheetCount);
        } catch (Throwable ex) {
            log.error("UnsImportErr:{}", file.getPath(), ex);
            consumer.accept(new RunningStatus(400, I18nUtils.getMessage("uns.create.status.error")));
            return;
        }

        if (isAsync){
            ThreadUtil.execAsync(() -> this.doImport(file.getPath(), consumer, file, templateVoList, topicList, labelsMap, reader, excelCheckErrorMap));
        } else {
            this.doImport(file.getPath(), consumer, file, templateVoList, topicList, labelsMap, reader, excelCheckErrorMap);
        }

    }

    @Transactional(timeout = 300, rollbackFor = Throwable.class)
    public void doImport(String excelFilePath, Consumer<RunningStatus> consumer, File file, List<CreateTemplateVo> templateVoList, List<CreateTopicDto> topicList, Map<String, String[]> labelsMap, ExcelReader reader, Map<String, String> errorMap) {
        if (CollectionUtils.isEmpty(topicList) && CollectionUtils.isEmpty(templateVoList) && MapUtils.isEmpty(errorMap)) {
            String message = I18nUtils.getMessage("uns.import.excel.empty");
            consumer.accept(new RunningStatus(400, message));
            return;
        } else {
            LogWrapperConsumer wrapperConsumer = new LogWrapperConsumer(consumer);
            String finalTask = I18nUtils.getMessage("uns.create.task.name.final");

            if (CollectionUtils.isNotEmpty(templateVoList)) {
                if (log.isInfoEnabled()) {
                    log.info("*** Excel[{}] 发起导入模板请求 createTemplate：{}", file, JsonUtil.toJsonUseFields(templateVoList));
                }
                errorMap.putAll(unsManagerService.createTemplates(templateVoList));
            }

            if (CollectionUtils.isNotEmpty(topicList)) {
                if (log.isInfoEnabled()) {
                    log.info("*** Excel[{}] 发起导入模型请求 createModelAndInstance：{}", file, JsonUtil.toJsonUseFields(topicList));
                }

                try {
                    Map<String, String> allErrorMap = unsManagerService.createModelAndInstance(topicList, labelsMap,
                            FileUtil.getPrefix(file.getAbsolutePath()), wrapperConsumer);
                    errorMap.putAll(allErrorMap);
                } catch (Throwable ex) {
                    String lastTask = wrapperConsumer.lastTask;
                    Double lastProgress = wrapperConsumer.lastProgress;
                    log.error("UnsImportErr:{} lastTask={}", excelFilePath, lastTask, ex);
                    Throwable cause = ex.getCause();
                    String errMsg;
                    if (cause != null) {
                        errMsg = cause.getMessage();
                    } else {
                        errMsg = ex.getMessage();
                    }
                    if (errMsg == null) {
                        errMsg = I18nUtils.getMessage("uns.create.status.error");
                    }
                    consumer.accept(new RunningStatus(500, errMsg)
                            .setTask(lastTask != null ? lastTask : finalTask)
                            .setProgress(lastProgress != null ? lastProgress : 0.0)
                    );
                    return;
                }
            }

            if (errorMap.isEmpty()) {
                String message = I18nUtils.getMessage("uns.import.rs.ok");
                consumer.accept(new RunningStatus(200, message)
                        .setTask(finalTask)
                        .setProgress(100.0));
                return;
            }

            File outFile = destFile("err_" + file.getName().replace(' ', '-'));

            ExcelWriter excelWriter = reader.getWriter();
            excelWriter.setDestFile(outFile);

            Map<Integer, Map<Integer, String>> error = new HashMap<>();
            for (Map.Entry<String, String> entry : errorMap.entrySet()) {
                String[] keyArr = entry.getKey().split("-");
                Map<Integer, String> subError = error.computeIfAbsent(Integer.valueOf(keyArr[0]), k -> new HashMap<>());
                subError.put(Integer.valueOf(keyArr[1]), entry.getValue());
            }
            for (Map.Entry<Integer, Map<Integer, String>> entry : error.entrySet()) {
                Map<Integer, String> subError = entry.getValue();
                if (MapUtils.isNotEmpty(subError)) {
                    writeErrTipExcel(entry.getKey(), subError, excelWriter);
                }
            }

            excelWriter.close();

            String message = I18nUtils.getMessage("uns.import.rs.hasErr");
            consumer.accept(new RunningStatus(206, message, FileUtils.getRelativePath(outFile.getAbsolutePath()))
                    .setTask(finalTask)
                    .setProgress(100.0));
        }
    }

    public JsonResult<String> templateImport(MultipartFile file) {
        String extName = FileUtil.extName(file.getOriginalFilename());
        if (!"xlsx".equals(extName)) {
            throw new BuzException("uns.import.not.xlsx");
        }
        try {
            int sheetIndex = 0;
            ExcelReader reader = ExcelUtil.getReader(file.getInputStream(), sheetIndex);
            int sheetCount = reader.getSheetCount();
            boolean hasData = false;
            do {
                String sheetName = reader.getSheet().getSheetName();
                ExcelTypeEnum excelType = ExcelTypeEnum.valueOfCode(sheetName);
                if (ExcelTypeEnum.ERROR.equals(excelType)) {
                    throw new BuzException("uns.import.template.error");
                }
                List<Object> heads = reader.readRow(0);
                if (CollectionUtils.isEmpty(heads) || !com.supos.uns.util.ExcelUtil.checkHead(excelType, heads)) {
                    throw new BuzException("uns.import.head.error", sheetName);
                }

                List<Map<String, Object>> dataList = reader.read(0, 4, 7);
                if (!CollectionUtils.isEmpty(dataList)) {
                    hasData = true;
                }
                sheetIndex++;
                if (sheetIndex < sheetCount) {
                    reader.setSheet(sheetIndex);
                }
            } while (sheetIndex < sheetCount);

            if (!hasData) {
                String msg = I18nUtils.getMessage("uns.import.excel.empty");
                return new JsonResult<>(500, msg);
            }
            File destFile = destFile(file.getOriginalFilename());
            file.transferTo(destFile);
            return new JsonResult<String>().setData(FileUtils.getRelativePath(destFile.getAbsolutePath()));
        }catch (BuzException e) {
            throw e;
        }catch (Exception e) {
            log.error("导入异常", e);
            throw new BuzException("uns.import.error");
        }
    }

    private static final File destFile(String fileName) {
        String datePath = DateUtil.format(new Date(), "yyyyMMddHHmmss");
        String targetPath = String.format("%s%s%s/%s", FileUtils.getFileRootPath(), Constants.EXCEL_ROOT, datePath, fileName);
        File outFile = FileUtil.touch(targetPath);
        return outFile;
    }

    static void writeErrTipExcel(int index, Map<Integer, String> errorMap, ExcelWriter excelWriter) {
        Workbook workbook = excelWriter.getWorkbook();
        CellStyle cellStyle = createCellStyle(excelWriter);
        // 操作第一个Sheet
        Sheet sheet = workbook.getSheetAt(index);
        for (Map.Entry<Integer, String> errEntry : errorMap.entrySet()) {
            String msg = errEntry.getValue();
            if (StringUtils.isNotBlank(msg)) {
                int rowNum = errEntry.getKey();
                Row row = sheet.getRow(rowNum);
                if (row != null) {
                    for (Cell cell : row) {
                        // 应用样式到单元格
                        cell.setCellStyle(cellStyle);
                    }
                    int lastCellNum = row.getLastCellNum();
                    Cell lastCell = row.createCell(lastCellNum + 1);
                    lastCell.setCellStyle(cellStyle);
                    lastCell.setCellValue(msg);
                }
            }
        }
    }






    private static boolean isEmptyRow(Map<String, Object> dataMap) {
        if (dataMap == null) {
            return true;
        }
        for (Object value : dataMap.values()) {
            if (value != null) {
                if (value instanceof CharSequence && StringUtils.isNotBlank((String) value)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static Boolean getBoolean(Map<String, Object> dataMap, String key, Boolean defaultValue) {
        Boolean finalValue = defaultValue;
        Object value = dataMap.get(key);
        if (value != null) {
            if (value instanceof Boolean) {
                finalValue = (Boolean) value;
            } else if (value instanceof String) {
                String tempValue = (String) value;
                if (StringUtils.isNotBlank(tempValue)) {
                    if (tempValue.equalsIgnoreCase("true") || tempValue.equalsIgnoreCase("false")) {
                        finalValue = Boolean.valueOf(tempValue);
                    } else {
                        // 无效值
                        finalValue = null;
                    }
                }
            }
        }

        return finalValue;
    }

    public static String getString(Map<String, Object> dataMap, String key, String defaultValue) {
        String finalValue = defaultValue;
        Object value = dataMap.get(key);
        if (value != null) {
            if (value instanceof String) {
                String tempValue = (String) value;
                if (StringUtils.isNotBlank(tempValue)) {
                    finalValue = tempValue;
                }
            }
        }

        return finalValue;
    }

    private static void addValidErrMsg(StringBuilder er, Set<ConstraintViolation<Object>> violations) {
        for (ConstraintViolation<Object> v : violations) {
            String t = v.getRootBeanClass().getSimpleName();
            String msg = I18nUtils.getMessage(v.getMessage());
            er.append('[').append(v.getPropertyPath()).append(' ').append(msg).append(']');
        }
    }

    static CellStyle createCellStyle(ExcelWriter writer) {
        CellStyle cellStyle = writer.createCellStyle();
        cellStyle.setFillForegroundColor(IndexedColors.RED1.getIndex());
        cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return cellStyle;
    }

    public JsonResult<String> dataExport(ExportParam exportParam) {
        try {
            // 1.获取要导出的数据
            final List<ExportNode> exportFolderList = new LinkedList<>();
            final List<ExportNode> exportFileList = new LinkedList<>();
            final Map<String, ExportNode> exportFolderMap = new HashMap<>();
            final Map<String, ExportNode> exportFileMap = new HashMap<>();
            Set<String> templateIds = new HashSet<>();
            Set<String> fileIds = new HashSet<>();
            if (StringUtils.equals(ExportParam.EXPORT_TYPE_ALL, exportParam.getExportType())) {
                List<UnsPo> folders = unsManagerService.list(Wrappers.lambdaQuery(UnsPo.class)
                        .eq(UnsPo::getPathType, 0)
                        .and(c -> c.ne(UnsPo::getDataType, Constants.ALARM_RULE_TYPE).or().isNull(UnsPo::getDataType))
                        .orderByAsc(UnsPo::getPath));
                folders.forEach(folder -> {
                    String path = folder.getPath();
                    ExportNode exportNode = new ExportNode(folder);
                    exportFolderList.add(exportNode);
                    exportFolderMap.put(path, exportNode);
                    if (folder.getModelId() != null) {
                        templateIds.add(folder.getModelId());
                    }
                });

                List<UnsPo> files = unsManagerService.list(Wrappers.lambdaQuery(UnsPo.class).eq(UnsPo::getPathType, 2)
                        .in(UnsPo::getDataType, Lists.newArrayList(Constants.TIME_SEQUENCE_TYPE, Constants.RELATION_TYPE))
                        .ne(UnsPo::getDataType, Constants.ALARM_RULE_TYPE).orderByAsc(UnsPo::getPath));
                files.forEach(file -> {
                    String path = file.getPath();
                    ExportNode exportNode = new ExportNode(file);
                    exportFileList.add(exportNode);
                    exportFileMap.put(path, exportNode);
                    fileIds.add(file.getId());
                    if (file.getModelId() != null) {
                        templateIds.add(file.getModelId());
                    }
                });
            } else if (CollectionUtils.isNotEmpty(exportParam.getModels()) || CollectionUtils.isNotEmpty(exportParam.getInstances())) {
                // 需要导出的文件夹,将文件夹和下属文件都查出来
                if (CollectionUtils.isNotEmpty(exportParam.getModels())) {
                    Set<String> folders = exportParam.getModels().stream().filter(StringUtils::isNotBlank).collect(Collectors.toSet());
                    if (CollectionUtils.isNotEmpty(folders)) {
                        LambdaQueryWrapper<UnsPo> query = Wrappers.lambdaQuery(UnsPo.class)
                                .in(UnsPo::getPathType, Lists.newArrayList(0, 2))
                                .and(c -> c.ne(UnsPo::getDataType, Constants.ALARM_RULE_TYPE).or().isNull(UnsPo::getDataType));
                        query.and( i -> {
                            for (String modelTopic : folders) {
                                i.or().likeRight(UnsPo::getPath, modelTopic);
                            }
                        });
                        query.orderByAsc(UnsPo::getPathType, UnsPo::getPath);

                        List<UnsPo> folderAndFiles = unsManagerService.list(query);

                        folderAndFiles.forEach(folderOrFile -> {
                            String path = folderOrFile.getPath();
                            ExportNode exportNode = new ExportNode(folderOrFile);
                            if (folderOrFile.getPathType() == 0) {
                                exportFolderList.add(exportNode);
                                exportFolderMap.put(path, exportNode);
                            } else if (folderOrFile.getPathType() == 2) {
                                exportFileList.add(exportNode);
                                exportFileMap.put(path, exportNode);
                                fileIds.add(folderOrFile.getId());
                            }
                            if (folderOrFile.getModelId() != null) {
                                templateIds.add(folderOrFile.getModelId());
                            }
                        });
                    }
                }

                // 需要导出的文件
                if (CollectionUtils.isNotEmpty(exportParam.getInstances())) {
                    Set<String> filePaths = exportParam.getInstances().stream().filter(StringUtils::isNotBlank).collect(Collectors.toSet());
                    if (CollectionUtils.isNotEmpty(filePaths)) {
                        // 1.先获取对应的文件夹
                        Set<String> folderPaths = new HashSet<>();
                        for (String path : filePaths) {
                            if (StringUtils.contains(path, "/")) {
                                String temPath = StringUtils.substring(path, 0, StringUtils.lastIndexOf(path, '/') + 1);
                                folderPaths.add(temPath);
                                while (StringUtils.countMatches(temPath, '/') > 1) {
                                    temPath = StringUtils.substring(temPath, 0, StringUtils.lastOrdinalIndexOf(temPath, "/", 2) + 1);
                                    folderPaths.add(temPath);
                                }
                                folderPaths.add(temPath);
                            }
                        }

                        if (CollectionUtils.isNotEmpty(folderPaths)) {
                            LambdaQueryWrapper<UnsPo> folderQuery = Wrappers.lambdaQuery(UnsPo.class)
                                    .eq(UnsPo::getPathType, 0)
                                    .and(c -> c.ne(UnsPo::getDataType, Constants.ALARM_RULE_TYPE).or().isNull(UnsPo::getDataType))
                                    .in(UnsPo::getPath, folderPaths);
                            folderQuery.orderByAsc(UnsPo::getPath);
                            List<UnsPo> folders = unsManagerService.list(folderQuery);

                            folders.forEach(folder -> {
                                String path = folder.getPath();
                                ExportNode exportNode = new ExportNode(folder);
                                exportFolderList.add(exportNode);
                                exportFolderMap.put(path, exportNode);
                                if (folder.getModelId() != null) {
                                    templateIds.add(folder.getModelId());
                                }
                            });
                        }

                        // 2.获取文件
                        List<UnsPo> files = unsManagerService.list(Wrappers.lambdaQuery(UnsPo.class).eq(UnsPo::getPathType, 2)
                                .in(UnsPo::getDataType, Lists.newArrayList(Constants.TIME_SEQUENCE_TYPE, Constants.RELATION_TYPE))
                                .in(UnsPo::getPath, exportParam.getInstances())
                                .ne(UnsPo::getDataType, Constants.ALARM_RULE_TYPE));
                        files.forEach(file -> {
                            String path = file.getPath();
                            ExportNode exportNode = new ExportNode(file);
                            exportFileList.add(exportNode);
                            exportFileMap.put(path, exportNode);
                            fileIds.add(file.getId());
                            if (file.getModelId() != null) {
                                templateIds.add(file.getModelId());
                            }
                        });
                    }
                }
            }

            // 查询模板
            Map<String, UnsPo> templateMap = new HashMap<>();
            if (StringUtils.equals(ExportParam.EXPORT_TYPE_ALL, exportParam.getExportType())) {
                List<UnsPo> templates = unsManagerService.list(Wrappers.lambdaQuery(UnsPo.class).eq(UnsPo::getPathType, 1).ne(UnsPo::getDataType, 5));
                if (CollectionUtils.isNotEmpty(templates)) {
                    templateMap.putAll(templates.stream().collect(Collectors.toMap(UnsPo::getId, Function.identity(), (k1, k2) -> k2)));
                }
            } else if (CollectionUtils.isNotEmpty(templateIds)) {
                List<UnsPo> templates = unsManagerService.list(Wrappers.lambdaQuery(UnsPo.class).eq(UnsPo::getPathType, 1).in(UnsPo::getId, templateIds));
                if (CollectionUtils.isNotEmpty(templates)) {
                    templateMap.putAll(templates.stream().collect(Collectors.toMap(UnsPo::getId, Function.identity(), (k1, k2) -> k2)));
                }
            }

            // 查询标签
            Map<String, Set<String>> unsLabelNamesMap = new HashMap<>();
            if (CollectionUtils.isNotEmpty(fileIds)) {
                List<UnsLabelRefPo> labelRefPos = unsLabelRefService.list(Wrappers.lambdaQuery(UnsLabelRefPo.class).in(UnsLabelRefPo::getUnsId, fileIds));
                if (CollectionUtils.isNotEmpty(labelRefPos)) {
                    Map<String, List<UnsLabelRefPo>> unsLabelMap = labelRefPos.stream().collect(Collectors.groupingBy(UnsLabelRefPo::getUnsId));
                    Set<Long> labelIds = labelRefPos.stream().map(UnsLabelRefPo::getLabelId).collect(Collectors.toSet());
                    List<UnsLabelPo> labels = unsLabelService.list(Wrappers.lambdaQuery(UnsLabelPo.class).in(UnsLabelPo::getId, labelIds));
                    Map<Long, UnsLabelPo> labelMap = labels.stream().collect(Collectors.toMap(UnsLabelPo::getId, Function.identity(), (k1, k2) -> k2));
                    for (Map.Entry<String, List<UnsLabelRefPo>> e : unsLabelMap.entrySet()) {
                        for (UnsLabelRefPo labelRefPo : e.getValue()) {
                            UnsLabelPo unsLabelPo = labelMap.get(labelRefPo.getLabelId());
                            unsLabelNamesMap.computeIfAbsent(e.getKey(), k -> new HashSet<>()).add(unsLabelPo.getLabelName());
                        }
                    }
                }
            }

            // 2.对数据预处理便于后续导出
            for (ExportNode exportFolder : exportFolderList) {
                UnsPo folder = exportFolder.getUnsPo();
                String path = folder.getPath();
                if (StringUtils.countMatches(path, '/') > 2) {
                    String parentPath = StringUtils.substring(path, 0, StringUtils.lastOrdinalIndexOf(path, "/", 2) + 1);
                    ExportNode parentNode = exportFolderMap.get(parentPath);
                    if (parentNode != null) {
                        exportFolder.setParent(parentNode);
                    }
                }
            }
            for (ExportNode exportFile : exportFileList) {
                UnsPo file = exportFile.getUnsPo();
                String path = file.getPath();
                if (StringUtils.countMatches(path, '/') >= 2) {
                    String parentPath = StringUtils.substring(path, 0, StringUtils.lastOrdinalIndexOf(path, "/", 1)+1);
                    ExportNode parentNode = exportFolderMap.get(parentPath);
                    if (parentNode != null) {
                        exportFile.setParent(parentNode);
                    }
                }
            }

            // 2.开始将数据写入excel
            String datePath = DateUtil.format(new Date(), "yyyyMMddHHmmss");
            String path = String.format("%s%s%s", Constants.EXCEL_ROOT, datePath, Constants.EXCEL_OUT_PATH);
            String targetPath = String.format("%s%s", FileUtils.getFileRootPath(), path);
            FileUtil.copyFile(new ClassPathResource(Constants.EXCEL_TEMPLATE_PATH).getInputStream(), new File(targetPath));
            if (CollectionUtils.isNotEmpty(exportFolderList) || CollectionUtils.isNotEmpty(exportFileList)) {
                Map<ExcelTypeEnum, Set<String>> excelTypeItemMap = new HashMap<>();
                ExcelWriter excelWriter = ExcelUtil.getReader(targetPath).getWriter();
                excelWriter.getStyleSet().setAlign(HorizontalAlignment.LEFT, VerticalAlignment.CENTER);
                Map<ExcelTypeEnum, AtomicInteger> sheetRowMap = new HashMap<>();
                for (ExportNode file : exportFileList) {
                    UnsPo unsPo = file.getUnsPo();
                    com.supos.uns.util.ExcelUtil.RowWrapper rowWrapper = com.supos.uns.util.ExcelUtil.createRow(unsPo, templateMap, unsLabelNamesMap);
                    file.setRowWrapper(rowWrapper);
                    IOTProtocol protocol = rowWrapper.getProtocol();

                    if (protocol == null || protocol == IOTProtocol.UNKNOWN) {
                        // 空实例
                        if (unsPo.getDataType() == Constants.TIME_SEQUENCE_TYPE) {
                            writeFolderAndFile(excelWriter, ExcelTypeEnum.TIMESERIES, file, excelTypeItemMap, sheetRowMap, templateMap, unsLabelNamesMap);
                        } else if (unsPo.getDataType() == Constants.RELATION_TYPE) {
                            writeFolderAndFile(excelWriter, ExcelTypeEnum.RELATION, file, excelTypeItemMap, sheetRowMap, templateMap, unsLabelNamesMap);
                        }
                    } else {
                        // 有协议的实例
                        switch (protocol) {
                            case MODBUS:
                                writeFolderAndFile(excelWriter, ExcelTypeEnum.TIMESERIES_MODBUS, file, excelTypeItemMap, sheetRowMap, templateMap, unsLabelNamesMap);
                                break;
                            case OPC_UA:
                                writeFolderAndFile(excelWriter, ExcelTypeEnum.TIMESERIES_OPCUA, file, excelTypeItemMap, sheetRowMap, templateMap, unsLabelNamesMap);
                                break;
                            case OPC_DA:
                                writeFolderAndFile(excelWriter, ExcelTypeEnum.TIMESERIES_OPCDA, file, excelTypeItemMap, sheetRowMap, templateMap, unsLabelNamesMap);
                                break;
                            case RELATION:
                                writeFolderAndFile(excelWriter, ExcelTypeEnum.RELATION, file, excelTypeItemMap, sheetRowMap, templateMap, unsLabelNamesMap);
                                break;
                            case REST:
                                writeFolderAndFile(excelWriter, ExcelTypeEnum.RELATION_RESTAPI, file, excelTypeItemMap, sheetRowMap, templateMap, unsLabelNamesMap);
                                break;
                            case MQTT:
                                if (unsPo.getDataType() == Constants.TIME_SEQUENCE_TYPE) {
                                    writeFolderAndFile(excelWriter, ExcelTypeEnum.TIMESERIES_MQTT, file, excelTypeItemMap, sheetRowMap, templateMap, unsLabelNamesMap);
                                } else if (unsPo.getDataType() == Constants.RELATION_TYPE) {
                                    writeFolderAndFile(excelWriter, ExcelTypeEnum.RELATION_MQTT, file, excelTypeItemMap, sheetRowMap, templateMap, unsLabelNamesMap);
                                }
                                break;
                        }
                    }
                }

                for (ExportNode folder : exportFolderList) {
                    com.supos.uns.util.ExcelUtil.RowWrapper rowWrapper = com.supos.uns.util.ExcelUtil.createRow(folder.getUnsPo(), templateMap, unsLabelNamesMap);
                    folder.setRowWrapper(rowWrapper);
                    writeFolderAndFile(excelWriter, ExcelTypeEnum.Folder, folder, excelTypeItemMap, sheetRowMap, templateMap, unsLabelNamesMap);
                }

                for (UnsPo template : templateMap.values()) {
                    writeFolderAndFile(excelWriter, ExcelTypeEnum.Template, new ExportNode(template), excelTypeItemMap, sheetRowMap, templateMap, unsLabelNamesMap);
                }

                List<ExcelTypeEnum> sortedExcelType = ExcelTypeEnum.sort();
                for (ExcelTypeEnum excelType : sortedExcelType) {
                    if (excelType.getIndex() < 0) {
                        continue;
                    }
                    if (CollectionUtils.isNotEmpty(excelTypeItemMap.get(excelType))) {
                        excelWriter.getWorkbook().setActiveSheet(excelType.getIndex());
                        break;
                    }
                }
                excelWriter.close();
            }
            return new JsonResult<String>().setData(path);
        } catch (Exception e) {
            log.error("导出异常", e);
            String msg = I18nUtils.getMessage("uns.export.error");
            return new JsonResult<>(500, msg);
        }
    }

    private void writeFolderAndFile(ExcelWriter excelWriter, ExcelTypeEnum excelType, ExportNode exportNode,
                                    Map<ExcelTypeEnum, Set<String>> excelTypeItemMap, Map<ExcelTypeEnum, AtomicInteger> sheetRowMap,
                                    Map<String, UnsPo> templateMap, Map<String, Set<String>> unsLabelNamesMap) {
        ExportNode parentNode = exportNode.getParent();
        if (parentNode != null && excelType == ExcelTypeEnum.Folder) {
            writeFolderAndFile(excelWriter, excelType, parentNode, excelTypeItemMap, sheetRowMap, templateMap, unsLabelNamesMap);

        }
        Set<String> pathSet = excelTypeItemMap.computeIfAbsent(excelType, k -> new HashSet<>());
        UnsPo unsPo = exportNode.getUnsPo();
        if (pathSet.add(unsPo.getPath())) {
            com.supos.uns.util.ExcelUtil.RowWrapper rowWrapper = exportNode.getRowWrapper();
            if (rowWrapper == null) {
                rowWrapper = com.supos.uns.util.ExcelUtil.createRow(unsPo, templateMap, unsLabelNamesMap);
            }
            List<Object> dataList = com.supos.uns.util.ExcelUtil.fillForWrite(rowWrapper.getDataList(), excelType);
            writeRow(excelWriter, excelType, sheetRowMap, dataList);
        }
    }

    private void writeRow(ExcelWriter excelWriter, ExcelTypeEnum excelType, Map<ExcelTypeEnum, AtomicInteger> sheetRowMap, List<Object> dataList) {
        excelWriter.setSheet(excelType.getCode());
        AtomicInteger rowIndex = sheetRowMap.computeIfAbsent(excelType, k -> new AtomicInteger(4));
        excelWriter.setCurrentRow(rowIndex.getAndAdd(1));
        excelWriter.writeRow(dataList);
    }

    static class TemplateParser extends AbstractParser {

        static final List<String> createTemplateFieldNames;


        static {
            createTemplateFieldNames = Arrays.stream(BeanUtils.getPropertyDescriptors(ExcelTemplateDto.class))
                    .map(f -> f.getName()).filter(f -> !"class".equals(f)).collect(Collectors.toList());
        }

        public List<CreateTemplateVo> parseExcelDataList(int batch, int skipRow, List<Map<String, Object>> dataList, Map<String, String> excelCheckErrorMap) {
            List<CreateTemplateVo> templateList = new ArrayList<>(dataList.size());
            Map<String, CreateTemplateVo> templateMap = new HashMap<>();
            for (int i = 0, sz = dataList.size(); i < sz; i++) {
                Map<String, Object> dataMap = dataList.get(i);
                if (isEmptyRow(dataMap)) {
                    continue;
                }

                ExcelTemplateDto excelTemplateDto = BeanUtil.copyProperties(dataMap, ExcelTemplateDto.class, "fields");
                excelTemplateDto.setBatch(batch);
                excelTemplateDto.setIndex(i + skipRow);
                String batchIndex = excelTemplateDto.gainBatchIndex();

                {
                    StringBuilder er = null;
                    Set<ConstraintViolation<Object>> violations = validator.validate(excelTemplateDto);
                    if (!violations.isEmpty()) {
                        if (er == null) {
                            er = new StringBuilder(128);
                        }
                        addValidErrMsg(er, violations);
                    }
                    if (er != null) {
                        excelCheckErrorMap.put(batchIndex, er.toString());
                        continue;
                    }
                }

                CreateTemplateVo templateVo = new CreateTemplateVo();
                templateVo.setPath(excelTemplateDto.getName());
                templateVo.setDescription(excelTemplateDto.getDescription());
                templateVo.setBatch(excelTemplateDto.getBatch());
                templateVo.setIndex(excelTemplateDto.getIndex());

                CreateTemplateVo templateInExcel = templateMap.get(templateVo.getPath());
                if (templateInExcel != null) {
                    // excel 中存在重复的topic
                    log.warn(I18nUtils.getMessage("uns.excel.duplicate.item", String.format("%s|%s", ExcelTypeEnum.Template.getCode(), templateInExcel.getPath())));
                    continue;
                }

                Pair<Boolean, List<FieldDefineVo>> checkFieldResult = checkField(batchIndex, dataMap, excelCheckErrorMap);
                if (checkFieldResult.getLeft()) {
                    if (checkFieldResult.getRight() != null) {
                        FieldDefineVo[] fieldDefines = checkFieldResult.getRight().stream().toArray(n -> new FieldDefineVo[n]);
                        templateVo.setFields(fieldDefines);
                    } else {
                        excelCheckErrorMap.put(batchIndex, I18nUtils.getMessage("uns.field.empty"));
                        continue;
                    }
                } else {
                    continue;
                }

                for (String templateField : createTemplateFieldNames) {
                    dataMap.remove(templateField);
                }

                templateList.add(templateVo);
                templateMap.put(templateVo.getPath(), templateVo);
            }
            return templateList;
        }
    }

    static class FolderParser extends AbstractParser {

        static final List<String> createFolderFieldNames;


        static {
            createFolderFieldNames = Arrays.stream(BeanUtils.getPropertyDescriptors(ExcelFolderDto.class))
                    .map(f -> f.getName()).filter(f -> !"class".equals(f)).collect(Collectors.toList());
        }

        public List<CreateTopicDto> parseExcelDataList(int batch, int skipRow, List<Map<String, Object>> dataList, Map<String, CreateTopicDto> topicMap, Map<String, String[]> labelsMap, Map<String, String> excelCheckErrorMap, Set<String> aliasInExcel) {
            ArrayList<CreateTopicDto> topicList = new ArrayList<>(dataList.size());
            for (int i = 0, sz = dataList.size(); i < sz; i++) {
                Map<String, Object> dataMap = dataList.get(i);
                if (isEmptyRow(dataMap)) {
                    continue;
                }

                ExcelFolderDto excelFolderDto = BeanUtil.copyProperties(dataMap, ExcelFolderDto.class, "fields");
                excelFolderDto.setBatch(batch);
                excelFolderDto.setIndex(i + skipRow);
                String batchIndex = excelFolderDto.gainBatchIndex();

                {
                    StringBuilder er = null;
                    Set<ConstraintViolation<Object>> violations = validator.validate(excelFolderDto);
                    if (!violations.isEmpty()) {
                        if (er == null) {
                            er = new StringBuilder(128);
                        }
                        addValidErrMsg(er, violations);
                    }
                    if (er != null) {
                        excelCheckErrorMap.put(batchIndex, er.toString());
                        continue;
                    }
                }

                CreateTopicDto createTopicDto = excelFolderDto.createTopic();

                CreateTopicDto topicInExcel = topicMap.get(createTopicDto.getTopic());
                if (topicInExcel != null) {
                    // excel 中存在重复的topic
                    log.warn(I18nUtils.getMessage("uns.excel.duplicate.item", String.format("%s|%s", ExcelTypeEnum.Folder.getCode(), createTopicDto.getTopic())));
                    continue;
                }

                if (StringUtils.isNotBlank(excelFolderDto.getAlias()) && !aliasInExcel.add(excelFolderDto.getAlias())) {
                    excelCheckErrorMap.put(batchIndex, I18nUtils.getMessage("uns.alias.has.exist"));
                    continue;
                }

                Pair<Boolean, List<FieldDefineVo>> checkFieldResult = checkField(batchIndex, dataMap, excelCheckErrorMap);
                if (checkFieldResult.getLeft()) {
                    if (checkFieldResult.getRight() != null) {
                        FieldDefine[] fieldDefines = checkFieldResult.getRight().stream().map(FieldDefineVo::convert).toArray(n -> new FieldDefine[n]);
                        createTopicDto.setFields(fieldDefines);
                    } else {
                        createTopicDto.setFields(new FieldDefine[]{});
                    }
                } else {
                    continue;
                }

                for (String topicField : createFolderFieldNames) {
                    dataMap.remove(topicField);
                }

                createTopicDto.setProtocol(JsonMapConvertUtils.convertMap(dataMap));
                createTopicDto.setDataType(ExcelTypeEnum.Folder.getDataType());
                createTopicDto.setIndex(skipRow + i);// 指定序号
                topicList.add(createTopicDto);
                topicMap.put(createTopicDto.getTopic(), createTopicDto);
                if (createTopicDto.getAlias() != null) {
                    aliasInExcel.add(createTopicDto.getAlias());
                }
            }
            return topicList;
        }
    }

    public static class TopicParser extends AbstractParser {

        static final List<String> createTopicFieldNames;

        static {
            createTopicFieldNames = Arrays.stream(BeanUtils.getPropertyDescriptors(ExcelNameSpaceDto.class))
                    .map(f -> f.getName()).filter(f -> !"class".equals(f)).collect(Collectors.toList());
        }

        public List<CreateTopicDto> parseExcelDataList(int batch, int skipRow, ExcelTypeEnum excelType, List<Map<String, Object>> dataList, Map<String, CreateTopicDto> topicMap, Map<String, String[]> labelsMap, Map<String, String> excelCheckErrorMap, Set<String> aliasInExcel) {
            ArrayList<CreateTopicDto> topicList = new ArrayList<>(dataList.size());
            for (int i = 0, sz = dataList.size(); i < sz; i++) {
                Map<String, Object> dataMap = dataList.get(i);
                if (isEmptyRow(dataMap)) {
                    continue;
                }
                ExcelNameSpaceDto excelNameSpaceDto = BeanUtil.copyProperties(dataMap, ExcelNameSpaceDto.class, "fields");
                excelNameSpaceDto.setBatch(batch);
                excelNameSpaceDto.setIndex(i + skipRow);
                String batchIndex = excelNameSpaceDto.gainBatchIndex();
                {
                    StringBuilder er = null;
                    Set<ConstraintViolation<Object>> violations = validator.validate(excelNameSpaceDto);
                    if (!violations.isEmpty()) {
                        if (er == null) {
                            er = new StringBuilder(128);
                        }
                        addValidErrMsg(er, violations);
                    }
                    if (er != null) {
                        excelCheckErrorMap.put(batchIndex, er.toString());
                        continue;
                    }
                }

                CreateTopicDto createTopicDto = excelNameSpaceDto.createTopic();
                createTopicDto.setDataType(excelType.getDataType());

                CreateTopicDto topicInExcel = topicMap.get(createTopicDto.getTopic());
                if (topicInExcel != null) {
                    // excel 中存在重复的topic
                    log.warn(I18nUtils.getMessage("uns.excel.duplicate.item", String.format("%s|%s", excelType.getCode(), createTopicDto.getTopic())));
                    continue;
                }

                if (StringUtils.isNotBlank(excelNameSpaceDto.getAlias()) && !aliasInExcel.add(excelNameSpaceDto.getAlias())) {
                    excelCheckErrorMap.put(batchIndex, I18nUtils.getMessage("uns.alias.has.exist"));
                    continue;
                }

                Boolean autoFlow = getBoolean(dataMap, "autoFlow", false);
                if (autoFlow == null) {
                    excelCheckErrorMap.put(batchIndex, I18nUtils.getMessage("uns.excel.autoFlow.invalid"));
                    continue;
                }
                createTopicDto.setAddFlow(autoFlow);
                dataMap.remove("autoFlow");

                Boolean autoDashboard = getBoolean(dataMap, "autoDashboard", false);
                if (autoDashboard == null) {
                    excelCheckErrorMap.put(batchIndex, I18nUtils.getMessage("uns.excel.autoDashboard.invalid"));
                    continue;
                }
                createTopicDto.setAddDashBoard(autoDashboard);
                dataMap.remove("autoDashboard");

                Boolean persistence = getBoolean(dataMap, "persistence", false);
                if (persistence == null) {
                    excelCheckErrorMap.put(batchIndex, I18nUtils.getMessage("uns.excel.persistence.invalid"));
                    continue;
                }
                createTopicDto.setSave2db(persistence);
                dataMap.remove("persistence");

                if (UnsManagerService.isInstance(createTopicDto.getTopic())) {
                    String labelStr = getString(dataMap, "label", "");
                    if (StringUtils.isNotBlank(labelStr)) {
                        String[] labels = StringUtils.split(labelStr, ',');
                        labelsMap.put(createTopicDto.getTopic(), labels);
                    }
                }
                dataMap.remove("label");

                Pair<Boolean, List<FieldDefineVo>> checkFieldResult = checkField(batchIndex, dataMap, excelCheckErrorMap);
                if (checkFieldResult.getLeft()) {
                    if (checkFieldResult.getRight() != null) {
                        FieldDefine[] fieldDefines = checkFieldResult.getRight().stream().map(FieldDefineVo::convert).toArray(n -> new FieldDefine[n]);
                        createTopicDto.setFields(fieldDefines);
                    }
                } else {
                    continue;
                }

                for (String topicField : createTopicFieldNames) {
                    dataMap.remove(topicField);
                }
                Map<String, Object> protocolMap = JsonMapConvertUtils.convertMap(dataMap);
                String topic = createTopicDto.getTopic();

                if (UnsManagerService.isInstance(topic)) {
                    Object protocolBean = null;
                    switch (excelType) {
                        case TIMESERIES_MODBUS: {
                            protocolMap.put("protocol", IOTProtocol.MODBUS.getName());
                            protocolBean = BeanUtil.toBean(protocolMap, IOTProtocol.MODBUS.protocolClass);
                            break;
                        }
                        case TIMESERIES_OPCUA: {
                            protocolMap.put("protocol", IOTProtocol.OPC_UA.getName());
                            protocolBean = BeanUtil.toBean(protocolMap, IOTProtocol.OPC_UA.protocolClass);
                            break;
                        }
                        case TIMESERIES_OPCDA: {
                            protocolMap.put("protocol", IOTProtocol.OPC_DA.getName());
                            protocolBean = BeanUtil.toBean(protocolMap, IOTProtocol.OPC_DA.protocolClass);
                            break;
                        }
                        case RELATION_RESTAPI:
                        case RELATION:{
                            // 有协议定义时 类型用 rest, 否则用 relation
                            if (!protocolMap.isEmpty()) {
                                protocolMap.put("protocol", IOTProtocol.REST.getName());
                                protocolBean = BeanUtil.toBean(protocolMap, IOTProtocol.REST.protocolClass);
                            } else {
                                protocolMap.put("protocol", ExcelTypeEnum.RELATION.getCode());
                            }
                            break;
                        }
                        case RELATION_MQTT:
                        case TIMESERIES_MQTT:{
                            protocolMap.put("protocol", IOTProtocol.MQTT.getName());
                            protocolBean = BeanUtil.toBean(protocolMap, IOTProtocol.MQTT.protocolClass);
                            break;
                        }
                    }

                    if (protocolBean != null) {
                        createTopicDto.setProtocolBean(protocolBean);

                        Set<ConstraintViolation<Object>> violations = validator.validate(protocolBean);
                        if (!violations.isEmpty()) {
                            StringBuilder er = new StringBuilder(128);
                            addValidErrMsg(er, violations);
                            excelCheckErrorMap.put(batchIndex, er.toString());
                            continue;
                        }
                        if (ArrayUtil.isEmpty(createTopicDto.getFields())) {
                            excelCheckErrorMap.put(batchIndex, I18nUtils.getMessage("uns.fieldsIsEmpty", excelType.name()));
                            continue;
                        }
                        boolean hasIndex = false;
                        for (FieldDefine f : createTopicDto.getFields()) {
                            if (f.getIndex() != null) {
                                hasIndex = true;
                                break;
                            }
                        }
                        if (!hasIndex) {
                            if (excelType != ExcelTypeEnum.RELATION
                                    && excelType != ExcelTypeEnum.RELATION_MQTT
                                    && excelType != ExcelTypeEnum.TIMESERIES_MQTT) {
                                excelCheckErrorMap.put(batchIndex, I18nUtils.getMessage("uns.fieldsIndexAllEmpty", excelType.name()));
                                continue;
                            }
                        }
                    }
                }

                createTopicDto.setProtocol(protocolMap);
                createTopicDto.setDataType(excelType.getDataType());
                topicList.add(createTopicDto);
                topicMap.put(createTopicDto.getTopic(), createTopicDto);
                if (createTopicDto.getAlias() != null) {
                    aliasInExcel.add(createTopicDto.getAlias());
                }
            }
            return topicList;
        }
    }

    static class AbstractParser {

        static final Validator validator;
        static {
            ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
            validator = factory.getValidator();
        }

        protected Pair<Boolean, List<FieldDefineVo>> checkField(String batchIndex, Map<String, Object> dataMap, Map<String, String> excelCheckErrorMap) {
            String fields = (String) dataMap.remove("fields");
            if (StringUtils.isNotBlank(fields)) {
                List<FieldDefineVo> defineList;
                try {
                    defineList = JSONUtil.toList(fields, FieldDefineVo.class);
                } catch (Exception ex) {
                    excelCheckErrorMap.put(batchIndex, "field json Err:" + ex.getMessage());
                    return Pair.of(false, null);
                }
                StringBuilder er = null;
                for (FieldDefineVo define : defineList) {
                    Set<ConstraintViolation<Object>> violations = validator.validate(define);
                    if (!violations.isEmpty()) {
                        if (er == null) {
                            er = new StringBuilder(128);
                        }
                        addValidErrMsg(er, violations);
                    }
                }
                if (er != null) {
                    excelCheckErrorMap.put(batchIndex, er.toString());
                    return Pair.of(false, null);
                }
                String validateMsg =  FieldUtils.validateFields(FieldDefineVo.convert(defineList.toArray(new FieldDefineVo[defineList.size()])), true);
                if (validateMsg != null) {
                    excelCheckErrorMap.put(batchIndex, validateMsg);
                    return Pair.of(false, null);
                }
                return Pair.of(true, defineList);
            }
            return Pair.of(true, null);
        }
    }

    @Data
    class ExportNode {
        UnsPo unsPo;
        ExportNode parent;
        com.supos.uns.util.ExcelUtil.RowWrapper rowWrapper;

        public ExportNode(UnsPo unsPo) {
            this.unsPo = unsPo;
        }
    }
}

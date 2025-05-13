package com.supos.uns.service;


import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.net.URLDecoder;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Lists;
import com.supos.common.Constants;
import com.supos.common.dto.JsonResult;
import com.supos.common.enums.ExcelTypeEnum;
import com.supos.common.exception.BuzException;
import com.supos.common.utils.I18nUtils;
import com.supos.uns.bo.RunningStatus;
import com.supos.uns.dao.po.UnsLabelPo;
import com.supos.uns.dao.po.UnsLabelRefPo;
import com.supos.uns.dao.po.UnsPo;
import com.supos.uns.service.exportimport.core.DataImporter;
import com.supos.uns.service.exportimport.core.ExcelExportContext;
import com.supos.uns.service.exportimport.core.ExcelImportContext;
import com.supos.uns.service.exportimport.core.ExportNode;
import com.supos.uns.service.exportimport.excel.ExcelDataExporter;
import com.supos.uns.service.exportimport.excel.ExcelDataImporter;
import com.supos.uns.service.exportimport.json.JsonDataExporter;
import com.supos.uns.service.exportimport.json.JsonDataImporter;
import com.supos.uns.util.ExportImportUtil;
import com.supos.uns.util.FileUtils;
import com.supos.uns.vo.ExportParam;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
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

    public void downloadTemplate(String fileType, HttpServletResponse response) {
        if (StringUtils.equals(fileType, "excel")) {
            doDownloadExcelTemplate(response);
        } else if (StringUtils.equals(fileType, "json")) {
            doDownloadJsonTemplate(response);
        }
    }

    public void doDownloadExcelTemplate(HttpServletResponse response) {
        ClassPathResource classPathResource = new ClassPathResource(Constants.EXCEL_TEMPLATE_PATH);
        InputStream inputStream = null;
        String targetPath = null;
        InputStream targetInputStream = null;
        try {
            inputStream = classPathResource.getInputStream();

            // 生成临时文件
            String datePath = DateUtil.format(new Date(), "yyyyMMddHHmmss");
            String path = String.format("%s%s%s", Constants.EXCEL_ROOT, datePath, Constants.EXCEL_OUT_PATH);
            targetPath = String.format("%s%s", FileUtils.getFileRootPath(), path);
            FileUtil.touch(targetPath);

            com.alibaba.excel.ExcelWriter excelWriter = EasyExcel.write(targetPath).withTemplate(new ClassPathResource(Constants.EXCEL_TEMPLATE_PATH).getInputStream()).build();
            excelWriter.finish();

            targetInputStream = FileUtil.getInputStream(targetPath);

            FileUtils.downloadFile(response, path.split("/")[path.split("/").length - 1], targetInputStream);
        } catch (IOException e) {
            log.error("downloadByPath Exception",e);
        } finally {
            if (inputStream != null) {
                IoUtil.close(inputStream);
            }
            if (targetInputStream != null) {
                IoUtil.close(targetInputStream);
            }
            if (targetPath != null) {
                FileUtil.del(targetPath);
            }
        }
    }

    public void doDownloadJsonTemplate(HttpServletResponse response) {
        ClassPathResource classPathResource = new ClassPathResource(Constants.JSON_TEMPLATE_PATH);
        InputStream inputStream = null;
        try {
            inputStream = classPathResource.getInputStream();
            String path = Constants.JSON_TEMPLATE_PATH;
            FileUtils.downloadFile(response, path.split("/")[path.split("/").length - 1], inputStream);
        } catch (IOException e) {
            log.error("downloadByPath Exception",e);
        } finally {
            if (inputStream != null) {
                IoUtil.close(inputStream);
            }
        }
    }

    public void excelDownload(HttpServletResponse response, String path) {
        try {
            path = URLDecoder.decode(path, StandardCharsets.UTF_8);
            File file = new File(FileUtils.getFileRootPath(), path);
            if (!file.exists()) {
                throw new BuzException("uns.file.not.exist");
            }

            String extName = FileUtil.extName(file.getName());
            String datePath = DateUtil.format(new Date(), "yyyyMMddHHmmss");
            String fileName = null;
            if ("xlsx".equals(extName)) {
                fileName = String.format("%s%s%s", "namespace-", datePath, ".xlsx");
            } else if ("json".equals(extName)) {
                fileName = String.format("%s%s%s", "namespace-", datePath, ".json");
            }
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

        String extName = FileUtil.extName(file.getName());
        ExcelImportContext context = new ExcelImportContext(file.toString());
        DataImporter dataImporter = null;
        try {
            if ("xlsx".equals(extName)) {
                dataImporter = new ExcelDataImporter(context);
            } else if ("json".equals(extName)) {
                dataImporter = new JsonDataImporter(context);
            }
            dataImporter.importData(file);
        } catch (Throwable ex) {
            log.error("UnsImportErr:{}", file.getPath(), ex);
            importFinish(dataImporter, extName, file.getPath(), consumer, file, context, ex);
            return;
        }
        importFinish(dataImporter, extName, file.getPath(), consumer, file, context, null);
    }

    private void importFinish(DataImporter dataImporter, String extName, String excelFilePath, Consumer<RunningStatus> consumer, File file, ExcelImportContext context, Throwable ex) {
        try {
            if (context.dataEmpty()) {
                String message = I18nUtils.getMessage("uns.import.excel.empty");
                consumer.accept(new RunningStatus(400, message));
                return;
            } else {
                LogWrapperConsumer wrapperConsumer = new LogWrapperConsumer(consumer);
                String finalTask = I18nUtils.getMessage("uns.create.task.name.final");

                if (ex != null) {
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

                if (context.getExcelCheckErrorMap().isEmpty()) {
                    String message = I18nUtils.getMessage("uns.import.rs.ok");
                    consumer.accept(new RunningStatus(200, message)
                            .setTask(finalTask)
                            .setProgress(100.0));
                    return;
                }

                Map<Integer, Map<Integer, String>> error = new HashMap<>();
                for (Map.Entry<String, String> entry : context.getExcelCheckErrorMap().entrySet()) {
                    String[] keyArr = entry.getKey().split("-");
                    Map<Integer, String> subError = error.computeIfAbsent(Integer.valueOf(keyArr[0]), k -> new HashMap<>());
                    subError.put(Integer.valueOf(keyArr[1]), entry.getValue());
                }
                context.getExcelCheckErrorMap().clear();
                context.getError().putAll(error);

                File outFile = destFile("err_" + file.getName().replace(' ', '-'));
                log.info("create error file:{}", outFile.toString());
                dataImporter.writeError(file, outFile);

                String message = I18nUtils.getMessage("uns.import.rs.hasErr");
                consumer.accept(new RunningStatus(206, message, FileUtils.getRelativePath(outFile.getAbsolutePath()))
                        .setTask(finalTask)
                        .setProgress(100.0));
            }
        } catch (Throwable e) {
            log.error("导入失败", e);
            throw new BuzException(e.getMessage());
        }

    }

    public JsonResult<String> templateImport(MultipartFile file) {
        String extName = FileUtil.extName(file.getOriginalFilename());
        if (!"xlsx".equals(extName) && !"json".equals(extName)) {
            throw new BuzException("uns.import.not.xlsx");
        }
        try {
            if ("xlsx".equals(extName)) {
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
                    if (CollectionUtils.isEmpty(heads) || !ExportImportUtil.checkHead(excelType, heads)) {
                        throw new BuzException("uns.import.head.error", sheetName);
                    }

/*                    List<Map<String, Object>> dataList = reader.read(0, 4, 7);
                    if (!CollectionUtils.isEmpty(dataList)) {
                        hasData = true;
                    }*/
                    sheetIndex++;
                    if (sheetIndex < sheetCount) {
                        reader.setSheet(sheetIndex);
                    }
                } while (sheetIndex < sheetCount);

/*                if (!hasData) {
                    String msg = I18nUtils.getMessage("uns.import.excel.empty");
                    return new JsonResult<>(500, msg);
                }*/
            }

            File destFile = destFile(file.getOriginalFilename());
            file.transferTo(destFile);
            return new JsonResult<String>().setData(FileUtils.getRelativePath(destFile.getAbsolutePath()));
        } catch (BuzException e) {
            throw e;
        } catch (Exception e) {
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

    public JsonResult<String> dataExport(ExportParam exportParam) {
        StopWatch stopWatch = new StopWatch();
        try {
            ExcelExportContext context = new ExcelExportContext();
            // 1.获取要导出的数据
            final List<ExportNode> exportFolderList = new LinkedList<>();
            final List<ExportNode> exportFileList = new LinkedList<>();
            final Map<String, ExportNode> exportFolderMap = new HashMap<>();
            final Map<String, ExportNode> exportFileMap = new HashMap<>();
            final Set<String> exportTemplateIds = new HashSet<>();
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
                                exportTemplateIds.add(folderOrFile.getModelId());
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
                                    exportTemplateIds.add(folder.getModelId());
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
                                exportTemplateIds.add(file.getModelId());
                            }
                        });
                    }
                }
            }

            // 查询模板
            stopWatch.start("load template");
            if (StringUtils.equals(ExportParam.EXPORT_TYPE_ALL, exportParam.getExportType())) {
                // 导出所有模板
                List<UnsPo> templates = unsManagerService.list(Wrappers.lambdaQuery(UnsPo.class).eq(UnsPo::getPathType, 1).ne(UnsPo::getDataType, 5));
                if (CollectionUtils.isNotEmpty(templates)) {
                    context.putAllTemplate(templates.stream().collect(Collectors.toMap(UnsPo::getId, Function.identity(), (k1, k2) -> k2)));
                }
            } else if (CollectionUtils.isNotEmpty(exportTemplateIds)) {
                List<UnsPo> templates = unsManagerService.list(Wrappers.lambdaQuery(UnsPo.class).eq(UnsPo::getPathType, 1).ne(UnsPo::getDataType, 5).in(UnsPo::getId, exportTemplateIds));
                if (CollectionUtils.isNotEmpty(templates)) {
                    context.putAllTemplate(templates.stream().collect(Collectors.toMap(UnsPo::getId, Function.identity(), (k1, k2) -> k2)));
                }
            }
            stopWatch.stop();

            // 查询标签
            stopWatch.start("load label");
            List<UnsLabelPo> labels = new ArrayList<>();
            if (StringUtils.equals(ExportParam.EXPORT_TYPE_ALL, exportParam.getExportType())) {
                // 导出所有标签
                labels.addAll(unsLabelService.list(Wrappers.lambdaQuery(UnsLabelPo.class)));
                List<UnsLabelRefPo> labelRefPos = unsLabelRefService.list(Wrappers.lambdaQuery(UnsLabelRefPo.class));
                if (CollectionUtils.isNotEmpty(labelRefPos)) {
                    Map<Long, UnsLabelPo> labelMap = labels.stream().collect(Collectors.toMap(UnsLabelPo::getId, Function.identity(), (k1, k2) -> k2));
                    Map<String, List<UnsLabelRefPo>> unsLabelMap = labelRefPos.stream().collect(Collectors.groupingBy(UnsLabelRefPo::getUnsId));
                    for (Map.Entry<String, List<UnsLabelRefPo>> e : unsLabelMap.entrySet()) {
                        for (UnsLabelRefPo labelRefPo : e.getValue()) {
                            UnsLabelPo unsLabelPo = labelMap.get(labelRefPo.getLabelId());
                            context.computeIfAbsentLabel(e.getKey(), unsLabelPo.getLabelName());
                        }
                    }
                }
            } else if (CollectionUtils.isNotEmpty(fileIds)) {
                List<UnsLabelRefPo> labelRefPos = unsLabelRefService.list(Wrappers.lambdaQuery(UnsLabelRefPo.class).in(UnsLabelRefPo::getUnsId, fileIds));
                if (CollectionUtils.isNotEmpty(labelRefPos)) {
                    Set<Long> labelIds = labelRefPos.stream().map(UnsLabelRefPo::getLabelId).collect(Collectors.toSet());
                    if (CollectionUtils.isNotEmpty(labelIds)) {
                        labels.addAll(unsLabelService.list(Wrappers.lambdaQuery(UnsLabelPo.class).in(UnsLabelPo::getId, labelIds)));
                    }
                    Map<Long, UnsLabelPo> labelMap = labels.stream().collect(Collectors.toMap(UnsLabelPo::getId, Function.identity(), (k1, k2) -> k2));
                    Map<String, List<UnsLabelRefPo>> unsLabelMap = labelRefPos.stream().collect(Collectors.groupingBy(UnsLabelRefPo::getUnsId));
                    for (Map.Entry<String, List<UnsLabelRefPo>> e : unsLabelMap.entrySet()) {
                        for (UnsLabelRefPo labelRefPo : e.getValue()) {
                            UnsLabelPo unsLabelPo = labelMap.get(labelRefPo.getLabelId());
                            context.computeIfAbsentLabel(e.getKey(), unsLabelPo.getLabelName());
                        }
                    }
                }
            }
            stopWatch.stop();

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
            stopWatch.start("write data");
            String path = null;
            if (StringUtils.equals(exportParam.getFileType(), "excel")) {
                path = new ExcelDataExporter().exportData(context, exportFolderList, exportFileList, labels);
            } else if (StringUtils.equals(exportParam.getFileType(), "json")) {
                path = new JsonDataExporter().exportData(context, exportFolderList, exportFileList, labels);
            }
            stopWatch.stop();
            return new JsonResult<String>().setData(path);
        } catch (Exception e) {
            log.error("导出异常", e);
            String msg = I18nUtils.getMessage("uns.export.error");
            return new JsonResult<>(500, msg);
        }
    }
}

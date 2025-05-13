package com.supos.uns.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.supos.common.Constants;
import com.supos.common.dto.FieldDefine;
import com.supos.common.dto.InstanceField;
import com.supos.common.enums.ExcelTypeEnum;
import com.supos.common.utils.JsonUtil;
import com.supos.uns.dao.po.UnsLabelPo;
import com.supos.uns.dao.po.UnsPo;
import com.supos.uns.service.exportimport.core.ExcelExportContext;
import com.supos.uns.service.exportimport.core.data.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author sunlifang
 * @version 1.0
 * @description: ExcelUtil
 * @date 2025/1/10 13:54
 */
public class ExportImportUtil {
    private static List<String> TEMPLATE_INDEX = new LinkedList<>();
    private static List<String> LABEL_INDEX = new LinkedList<>();
    private static List<String> FOLDER_INDEX = new LinkedList<>();
    private static List<String> FILE_TIMESERIES_INDEX = new LinkedList<>();
    private static List<String> FILE_RELATION_INDEX = new LinkedList<>();
/*    private static List<String> FILE_CALCULATE_INDEX = new LinkedList<>();
    private static List<String> FILE_AGGREGATION_INDEX = new LinkedList<>();
    private static List<String> FILE_REFERENCE_INDEX = new LinkedList<>();

    public static List<String> EXPLANATION = new LinkedList<>();*/

    static {
        // 模板列
        TEMPLATE_INDEX.addAll(getFields(ExcelTypeEnum.Template));

        // 标签列
        LABEL_INDEX.addAll(getFields(ExcelTypeEnum.Label));

        // 文件夹列
        FOLDER_INDEX.addAll(getFields(ExcelTypeEnum.Folder));

        FILE_TIMESERIES_INDEX.addAll(getFields(ExcelTypeEnum.FILE_TIMESERIES));

        FILE_RELATION_INDEX.addAll(getFields(ExcelTypeEnum.FILE_RELATION));

/*        FILE_CALCULATE_INDEX.addAll(getFields(ExcelTypeEnum.FILE_CALCULATE));

        FILE_AGGREGATION_INDEX.addAll(getFields(ExcelTypeEnum.FILE_AGGREGATION));

        FILE_REFERENCE_INDEX.addAll(getFields(ExcelTypeEnum.FILE_REFERENCE));

        EXPLANATION.add("uns.excel.explanation.template.alias");
        EXPLANATION.add("uns.excel.explanation.folder.path");
        EXPLANATION.add("uns.excel.explanation.folder.alias");
        EXPLANATION.add("uns.excel.explanation.folder.templatealias");
        EXPLANATION.add("uns.excel.explanation.file.alias");
        EXPLANATION.add("uns.excel.explanation.file.templatealias");
        EXPLANATION.add("uns.excel.explanation.file.refers");
        EXPLANATION.add("uns.excel.explanation.file.expression");
        EXPLANATION.add("uns.excel.explanation.file.frequency");
        EXPLANATION.add("uns.excel.explanation.file.label");*/
    }

    public static int errorIndex(ExcelTypeEnum excelTypeEnum) {
        List<String> fields = getFields(excelTypeEnum);
        return fields.size();
    }

    private static List<String> getFields(ExcelTypeEnum excelTypeEnum) {
        Class<? extends ExportImportData> clazz = null;
        switch (excelTypeEnum) {
            case Template:
                clazz = TemplateDataBase.class;
                break;
            case Label:
                clazz = LabelDataBase.class;
                break;
            case Folder:
                clazz = FolderDataBase.class;
                break;
            case FILE_TIMESERIES:
                clazz = FileTimeseriesBase.class;
                break;
            case FILE_RELATION:
                clazz = FileRelationBase.class;
                break;
/*            case FILE_CALCULATE:
                clazz = FileCalculateBase.class;
                break;
            case FILE_AGGREGATION:
                clazz = FileAggregationBase.class;
                break;
            case FILE_REFERENCE:
                clazz = FileReferenceBase.class;
                break;*/
        }
        return Arrays.stream(clazz.getDeclaredFields()).map(Field::getName).collect(Collectors.toList());
    }

    /**
     * 校验表头是否正确
     *
     * @param excelType
     * @param heads
     * @return
     */
    public static boolean checkHead(ExcelTypeEnum excelType, List<Object> heads) {
        List<String> needHeads = new ArrayList<>();
        List<String> tempHeads = heads != null ? heads.stream().map(Object::toString).collect(Collectors.toList()) : new ArrayList<>();
        switch (excelType) {
            case Template:
                needHeads = TEMPLATE_INDEX;
                break;
            case Label:
                needHeads = LABEL_INDEX;
                break;
            case Folder:
                needHeads = FOLDER_INDEX;
                break;
            case FILE_TIMESERIES:
                needHeads = FILE_TIMESERIES_INDEX;
                break;
            case FILE_RELATION:
                needHeads = FILE_RELATION_INDEX;
                break;
/*            case FILE_CALCULATE:
                needHeads = FILE_CALCULATE_INDEX;
                break;
            case FILE_AGGREGATION:
                needHeads = FILE_AGGREGATION_INDEX;
                break;
            case FILE_REFERENCE:
                needHeads = FILE_REFERENCE_INDEX;
                break;*/
        }

        for (String needHead : needHeads) {
            if (!tempHeads.contains(needHead)) {
                return false;
            }
        }
        return true;
    }

    public static RowWrapper createRow(UnsPo unsPo, ExcelExportContext context) {
        RowWrapper rowWrapper = null;
        if (unsPo.getPathType() == 0) {
            // 解析文件夹
            rowWrapper = createRowForFolder(unsPo, context);
        } else if (unsPo.getPathType() == 1) {
            // 解析模板
            rowWrapper = createRowForTemplate(unsPo);
        } else {
            //解析文件
            switch (unsPo.getDataType()) {
                case Constants.TIME_SEQUENCE_TYPE:
                    rowWrapper = createRowForFileTimeseries(unsPo, context);
                    break;
                case Constants.RELATION_TYPE:
                    rowWrapper = createRowForFileRelation(unsPo, context);
                    break;
/*                case Constants.CALCULATION_REAL_TYPE:
                    rowWrapper = createRowForFileCalculate(unsPo, context);
                    break;
                case Constants.MERGE_TYPE:
                    rowWrapper = createRowForFileAggregation(unsPo, context);
                    break;
                case Constants.CITING_TYPE:
                    rowWrapper = createRowForFileReference(unsPo, context);
                    break;*/
            }
        }

        return rowWrapper;
    }

    /**
     * 模板
     *
     * @param unsPo
     * @return
     */
    private static RowWrapper createRowForTemplate(UnsPo unsPo) {
        TemplateDataBase excelData = new TemplateDataBase();
        excelData.setName(unsPo.getPath());
        excelData.setAlias(unsPo.getAlias() != null ? unsPo.getAlias() : "");
        excelData.setFields(field(unsPo.getFields()));
        excelData.setDescription(StringUtils.isNotBlank(unsPo.getDescription()) ? unsPo.getDescription() : "");
        return new RowWrapper(ExcelTypeEnum.Template, excelData);
    }

    /**
     * 标签
     * @param label
     * @return
     */
    public static RowWrapper createRow(UnsLabelPo label) {
        LabelDataBase excelData = new LabelDataBase();
        excelData.setName(StringUtils.isNotBlank(label.getLabelName()) ? label.getLabelName() : "");
        return new RowWrapper(ExcelTypeEnum.Label, excelData);
    }

    /**
     * 文件夹
     *
     * @param unsPo
     * @return
     */
    private static RowWrapper createRowForFolder(UnsPo unsPo, ExcelExportContext context) {
        Map<String, UnsPo> templateMap = context.getTemplateMap();

        FolderDataBase excelData = new FolderDataBase();
        excelData.setPath(unsPo.getPath());
        excelData.setAlias(unsPo.getAlias() != null ? unsPo.getAlias() : "");
        if (unsPo.getModelId() != null) {
            UnsPo template = templateMap.get(unsPo.getModelId());
            excelData.setTemplateAlias(template != null ? template.getAlias() : "");
        } else {
            excelData.setTemplateAlias("");
        }
        //excelData.setDisplayName(StringUtils.isNotBlank(unsPo.getDisplayName()) ? unsPo.getDisplayName() : "");
        excelData.setFields(field(unsPo.getFields()));
        excelData.setDescription(unsPo.getDescription());

        return new RowWrapper(ExcelTypeEnum.Folder, excelData);
    }

    /**
     * 时序文件
     *
     * @param unsPo
     * @return
     */
    private static RowWrapper createRowForFileTimeseries(UnsPo unsPo, ExcelExportContext context) {
        Map<String, UnsPo> templateMap = context.getTemplateMap();
        Map<String, Set<String>> unsLabelNamesMap = context.getUnsLabelNamesMap();

        FileTimeseriesBase excelData = new FileTimeseriesBase();
        excelData.setPath(StringUtils.isNotBlank(unsPo.getPath()) ? unsPo.getPath() : "");
        excelData.setAlias(unsPo.getAlias() != null ? unsPo.getAlias() : "");
        //excelData.setDisplayName(unsPo.getDisplayName() != null ? unsPo.getDisplayName() : "");
        if (unsPo.getModelId() != null) {
            UnsPo template = templateMap.get(unsPo.getModelId());
            excelData.setTemplateAlias(template != null ? template.getAlias() : "");
        } else {
            excelData.setTemplateAlias("");
        }
        excelData.setFields(field(unsPo.getFields()));
        excelData.setDescription(StringUtils.isNotBlank(unsPo.getDescription()) ? unsPo.getDescription() : "");

        if (unsPo.getWithFlags() != null) {
            excelData.setMockData(Constants.withFlow(unsPo.getWithFlags()) ? "TRUE" : "FALSE");
        } else {
            excelData.setMockData("");
        }

        if (unsPo.getWithFlags() != null) {
            excelData.setAutoDashboard(Constants.withDashBoard(unsPo.getWithFlags()) ? "TRUE" : "FALSE");
        } else {
            excelData.setAutoDashboard("");
        }

        if (unsPo.getWithFlags() != null) {
            excelData.setPersistence(Constants.withSave2db(unsPo.getWithFlags()) ? "TRUE" : "FALSE");
        } else {
            excelData.setPersistence("");
        }

        Set<String> labels = unsLabelNamesMap.get(unsPo.getId());
        if (CollectionUtils.isNotEmpty(labels)) {
            excelData.setLabel(StringUtils.join(labels, ','));
        } else {
            excelData.setLabel("");
        }

        return new RowWrapper(ExcelTypeEnum.FILE_TIMESERIES, excelData);
    }

    /**
     * 关系文件
     *
     * @param unsPo
     * @return
     */
    private static RowWrapper createRowForFileRelation(UnsPo unsPo, ExcelExportContext context) {
        Map<String, UnsPo> templateMap = context.getTemplateMap();
        Map<String, Set<String>> unsLabelNamesMap = context.getUnsLabelNamesMap();

        FileRelationBase excelData = new FileRelationBase();
        excelData.setPath(StringUtils.isNotBlank(unsPo.getPath()) ? unsPo.getPath() : "");
        excelData.setAlias(unsPo.getAlias() != null ? unsPo.getAlias() : "");
        //excelData.setDisplayName(unsPo.getDisplayName() != null ? unsPo.getDisplayName() : "");
        if (unsPo.getModelId() != null) {
            UnsPo template = templateMap.get(unsPo.getModelId());
            excelData.setTemplateAlias(template != null ? template.getAlias() : "");
        } else {
            excelData.setTemplateAlias("");
        }
        excelData.setFields(field(unsPo.getFields()));
        excelData.setDescription(StringUtils.isNotBlank(unsPo.getDescription()) ? unsPo.getDescription() : "");

        if (unsPo.getWithFlags() != null) {
            excelData.setMockData(Constants.withFlow(unsPo.getWithFlags()) ? "TRUE" : "FALSE");
        } else {
            excelData.setMockData("");
        }

        if (unsPo.getWithFlags() != null) {
            excelData.setAutoDashboard(Constants.withDashBoard(unsPo.getWithFlags()) ? "TRUE" : "FALSE");
        } else {
            excelData.setAutoDashboard("");
        }

        if (unsPo.getWithFlags() != null) {
            excelData.setPersistence(Constants.withSave2db(unsPo.getWithFlags()) ? "TRUE" : "FALSE");
        } else {
            excelData.setPersistence("");
        }

        Set<String> labels = unsLabelNamesMap.get(unsPo.getId());
        if (CollectionUtils.isNotEmpty(labels)) {
            excelData.setLabel(StringUtils.join(labels, ','));
        } else {
            excelData.setLabel("");
        }
        return new RowWrapper(ExcelTypeEnum.FILE_RELATION, excelData);
    }

    /**
     * 计算文件
     *
     * @param unsPo
     * @return
     */
/*    private static RowWrapper createRowForFileCalculate(UnsPo unsPo, ExcelExportContext context) {
        Map<Long, Set<String>> unsLabelNamesMap = context.getUnsLabelNamesMap();

        FileCalculateBase excelData = new FileCalculateBase();
        excelData.setPath(StringUtils.isNotBlank(unsPo.getPath()) ? unsPo.getPath() : "");
        excelData.setAlias(unsPo.getAlias() != null ? unsPo.getAlias() : "");
        excelData.setDisplayName(unsPo.getDisplayName() != null ? unsPo.getDisplayName() : "");
        excelData.setFields(field(unsPo.getFields()));
        excelData.setDescription(StringUtils.isNotBlank(unsPo.getDescription()) ? unsPo.getDescription() : "");

        if (unsPo.getWithFlags() != null) {
            excelData.setAutoDashboard(Constants.withDashBoard(unsPo.getWithFlags()) ? "TRUE" : "FALSE");
        } else {
            excelData.setAutoDashboard("");
        }

        if (unsPo.getWithFlags() != null) {
            excelData.setPersistence(Constants.withSave2db(unsPo.getWithFlags()) ? "TRUE" : "FALSE");
        } else {
            excelData.setPersistence("");
        }

        Set<String> labels = unsLabelNamesMap.get(unsPo.getId());
        if (CollectionUtils.isNotEmpty(labels)) {
            excelData.setLabel(StringUtils.join(labels, ','));
        } else {
            excelData.setLabel("");
        }

        excelData.setExpression(StringUtils.isNotBlank(unsPo.getExpression()) ? unsPo.getExpression() : "");

        RowWrapper rowWrapper = new RowWrapper(ExcelTypeEnum.FILE_CALCULATE, excelData);
        if (ArrayUtils.isNotEmpty(unsPo.getRefers())) {
            for (InstanceField refer : unsPo.getRefers()) {
                context.addRefer(refer);
            }
            rowWrapper.setRefers(unsPo.getRefers());
        }
        excelData.setRefers("");

        return rowWrapper;
    }*/

    /**
     * 聚合文件
     *
     * @param unsPo
     * @return
     */
/*    private static RowWrapper createRowForFileAggregation(UnsPo unsPo, ExcelExportContext context) {
        Map<Long, Set<String>> unsLabelNamesMap = context.getUnsLabelNamesMap();

        FileAggregationBase excelData = new FileAggregationBase();
        excelData.setPath(StringUtils.isNotBlank(unsPo.getPath()) ? unsPo.getPath() : "");
        excelData.setAlias(unsPo.getAlias() != null ? unsPo.getAlias() : "");
        excelData.setDisplayName(unsPo.getDisplayName() != null ? unsPo.getDisplayName() : "");
        excelData.setDescription(StringUtils.isNotBlank(unsPo.getDescription()) ? unsPo.getDescription() : "");

        if (unsPo.getWithFlags() != null) {
            excelData.setAutoDashboard(Constants.withDashBoard(unsPo.getWithFlags()) ? "TRUE" : "FALSE");
        } else {
            excelData.setAutoDashboard("");
        }

        if (unsPo.getWithFlags() != null) {
            excelData.setPersistence(Constants.withSave2db(unsPo.getWithFlags()) ? "TRUE" : "FALSE");
        } else {
            excelData.setPersistence("");
        }

        Set<String> labels = unsLabelNamesMap.get(unsPo.getId());
        if (CollectionUtils.isNotEmpty(labels)) {
            excelData.setLabel(StringUtils.join(labels, ','));
        } else {
            excelData.setLabel("");
        }

        String frequencyStr = "";
        String protocol = unsPo.getProtocol();
        if (JSONObject.isValidObject(protocol)) {
            JSONObject jsonObject = JSONObject.parseObject(protocol);
            Object frequency =jsonObject.get("frequency");
            if (frequency != null) {
                frequencyStr = frequency.toString();
            }
        }
        excelData.setFrequency(frequencyStr);

        RowWrapper rowWrapper = new RowWrapper(ExcelTypeEnum.FILE_AGGREGATION, excelData);
        if (ArrayUtils.isNotEmpty(unsPo.getRefers())) {
            for (InstanceField refer : unsPo.getRefers()) {
                context.addRefer(refer);
            }
            rowWrapper.setRefers(unsPo.getRefers());
        }
        excelData.setRefers("");

        return rowWrapper;
    }*/

    /**
     * RestAPI协议文件
     *
     * @param
     * @return
     */
/*    private static RowWrapper createRowForFileReference(UnsPo unsPo, ExcelExportContext context) {
        Map<Long, Set<String>> unsLabelNamesMap = context.getUnsLabelNamesMap();

        FileReferenceBase excelData = new FileReferenceBase();
        excelData.setPath(StringUtils.isNotBlank(unsPo.getPath()) ? unsPo.getPath() : "");
        excelData.setAlias(unsPo.getAlias() != null ? unsPo.getAlias() : "");
        excelData.setDisplayName(unsPo.getDisplayName() != null ? unsPo.getDisplayName() : "");
        excelData.setDescription(StringUtils.isNotBlank(unsPo.getDescription()) ? unsPo.getDescription() : "");

        if (unsPo.getWithFlags() != null) {
            excelData.setAutoDashboard(Constants.withDashBoard(unsPo.getWithFlags()) ? "TRUE" : "FALSE");
        } else {
            excelData.setAutoDashboard("");
        }

        if (unsPo.getWithFlags() != null) {
            excelData.setPersistence(Constants.withSave2db(unsPo.getWithFlags()) ? "TRUE" : "FALSE");
        } else {
            excelData.setPersistence("");
        }

        Set<String> labels = unsLabelNamesMap.get(unsPo.getId());
        if (CollectionUtils.isNotEmpty(labels)) {
            excelData.setLabel(StringUtils.join(labels, ','));
        } else {
            excelData.setLabel("");
        }

        RowWrapper rowWrapper = new RowWrapper(ExcelTypeEnum.FILE_REFERENCE, excelData);
        if (ArrayUtils.isNotEmpty(unsPo.getRefers())) {
            for (InstanceField refer : unsPo.getRefers()) {
                context.addRefer(refer);
            }
            rowWrapper.setRefers(unsPo.getRefers());
        }
        excelData.setRefers("");

        return rowWrapper;
    }*/

    private static final String field(String fs) {
        if (fs != null && fs.length() > 3 && fs.charAt(0) == '[') {
            List<FieldDefine> list = JsonUtil.fromJson(fs, new TypeReference<List<FieldDefine>>() {
            }.getType());
            return JsonUtil.toJson(list.stream().filter(f -> !f.getName().startsWith(Constants.SYSTEM_FIELD_PREV)).collect(Collectors.toList()));
        }
        return "";
    }

    @Data
    @AllArgsConstructor
    public static class RowWrapper {
        private ExcelTypeEnum excelType;
        private ExportImportData exportImportData;

        private InstanceField[] refers;

        public RowWrapper(ExcelTypeEnum excelType, ExportImportData exportImportData) {
            this.excelType = excelType;
            this.exportImportData = exportImportData;
        }

/*        public void handleRefer(Map<Long, UnsPo> referFileIdMap, Map<String, UnsPo> referFileAliadMap) {
            if (refers != null) {
                JSONArray jsonArray = new JSONArray();
                for (InstanceField field : refers) {
                    UnsPo ref = null;
                    if (field.getId() == null) {
                        ref = referFileIdMap.get(field.getId());
                    } else if (field.getAlias() != null) {
                        ref = referFileAliadMap.get(field.getAlias());
                    }

                    if (ref != null) {
                        JSONObject jsonObject = new JSONObject();

                        if (excelType == ExcelTypeEnum.FILE_CALCULATE) {
                            jsonObject.put("field", field.getField());
                            jsonObject.put("path", ref.getPath());
                            jsonObject.put("alias", ref.getAlias());
                        } else if (excelType == ExcelTypeEnum.FILE_AGGREGATION || excelType == ExcelTypeEnum.FILE_REFERENCE) {
                            jsonObject.put("path", ref.getPath());
                            jsonObject.put("alias", ref.getAlias());
                        }

                        jsonArray.add(jsonObject);
                    }
                }
                exportImportData.handleRefers(jsonArray.toString());
            }
        }*/
    }
}

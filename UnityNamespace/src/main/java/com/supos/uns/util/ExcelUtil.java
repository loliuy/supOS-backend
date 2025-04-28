package com.supos.uns.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.supos.common.Constants;
import com.supos.common.dto.protocol.*;
import com.supos.common.enums.ExcelTypeEnum;
import com.supos.common.enums.IOTProtocol;
import com.supos.common.utils.JsonUtil;
import com.supos.common.vo.FieldDefineVo;
import com.supos.uns.dao.po.UnsPo;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author sunlifang
 * @version 1.0
 * @description: TODO
 * @date 2025/1/10 13:54
 */
public class ExcelUtil {
    private static List<String> TEMPLATE_INDEX = new LinkedList<>();
    private static List<String> FOLDER_INDEX = new LinkedList<>();

    private static List<String> TIMESERIES_INDEX = new LinkedList<>();
    private static List<String> RELATION_INDEX = new LinkedList<>();

    static {
        TEMPLATE_INDEX.add("name");
        TEMPLATE_INDEX.add("fields");
        TEMPLATE_INDEX.add("description");

        FOLDER_INDEX.add("name");
        FOLDER_INDEX.add("alias");
        FOLDER_INDEX.add("template");
        FOLDER_INDEX.add("fields");
        FOLDER_INDEX.add("description");

        TIMESERIES_INDEX.add("topic");
        TIMESERIES_INDEX.add("alias");
        TIMESERIES_INDEX.add("template");
        TIMESERIES_INDEX.add("fields");
        TIMESERIES_INDEX.add("description");
        TIMESERIES_INDEX.add("mockData");
        TIMESERIES_INDEX.add("autoDashboard");
        TIMESERIES_INDEX.add("persistence");
        TIMESERIES_INDEX.add("label");

        RELATION_INDEX.add("topic");
        RELATION_INDEX.add("alias");
        RELATION_INDEX.add("template");
        RELATION_INDEX.add("fields");
        RELATION_INDEX.add("description");
        RELATION_INDEX.add("mockData");
        RELATION_INDEX.add("autoDashboard");
        RELATION_INDEX.add("persistence");
        RELATION_INDEX.add("label");
    }

    /**
     * 校验表头是否正确
     * @param excelType
     * @param heads
     * @return
     */
    public static boolean checkHead(ExcelTypeEnum excelType, List<Object> heads) {
        List<String> needHeads = new ArrayList<>();
        List<String> tempHeads = heads != null ? heads.stream().map(Object::toString).collect(Collectors.toList()) : new ArrayList<>();
        switch (excelType) {
            case Template:
                needHeads = TEMPLATE_INDEX;break;
            case Folder:
                needHeads = FOLDER_INDEX;break;
            case RELATION:
                needHeads = RELATION_INDEX;break;
            case TIMESERIES:
                needHeads = TIMESERIES_INDEX;break;
        }

        for (String needHead : needHeads) {
            if (!tempHeads.contains(needHead)) {
                return false;
            }
        }
        return true;
    }
    
    public static RowWrapper createRow(UnsPo unsPo, Map<String, UnsPo> templateMap, Map<String, Set<String>> unsLabelNamesMap) {
        List<Object> dataList = null;
        IOTProtocol protocol = null;

        if (unsPo.getPathType() == 0) {
            // 解析文件夹
            dataList = createRowForModel(unsPo, templateMap);
        } else if (unsPo.getPathType() == 1) {
            // 解析模板
            dataList = createRowForTemplate(unsPo);
        } else {
            //解析文件
            protocol = IOTProtocol.getByName(unsPo.getProtocolType());
            if (unsPo.getDataType() == Constants.TIME_SEQUENCE_TYPE) {
                //时序数据解析
                dataList = createRowForBlankInstance(unsPo, templateMap, unsLabelNamesMap);

            } else if (unsPo.getDataType() == Constants.RELATION_TYPE) {
                //关系数据解析
                dataList = createRowForBlankRelationInstance(unsPo, templateMap, unsLabelNamesMap);
            }
        }

        return new RowWrapper(protocol, dataList);
    }

    /**
     * 文件夹
     * @param unsPo
     * @return
     */
    private static List<Object> createRowForModel(UnsPo unsPo, Map<String, UnsPo> templateMap) {
        List<Object> dataList = new ArrayList<>(3);
        dataList.add(unsPo.getPath());
        dataList.add(unsPo.getAlias() != null ? unsPo.getAlias() : "");
        if (unsPo.getModelId() != null) {
            UnsPo template = templateMap.get(unsPo.getModelId());
            dataList.add(template != null ? template.getPath() : "");
        } else {
            dataList.add("");
        }
        dataList.add(field(unsPo.getFields()));

        dataList.add(unsPo.getDescription());
        return dataList;
    }

    /**
     * 空白文件
     * @param unsPo
     * @return
     */
    private static List<Object> createRowForBlankInstance(UnsPo unsPo, Map<String, UnsPo> templateMap, Map<String, Set<String>> unsLabelNamesMap) {
        List<Object> dataList = new ArrayList<>(3);
        dataList.add(unsPo.getPath());
        dataList.add(unsPo.getAlias() != null ? unsPo.getAlias() : "");
        if (unsPo.getModelId() != null) {
            UnsPo template = templateMap.get(unsPo.getModelId());
            dataList.add(template != null ? template.getPath() : "");
        } else {
            dataList.add("");
        }
        dataList.add(field(unsPo.getFields()));
        dataList.add(unsPo.getDescription());
        if (unsPo.getWithFlags() != null) {
            dataList.add(Constants.withFlow(unsPo.getWithFlags()) ? "TRUE" : "FALSE");
            dataList.add(Constants.withDashBoard(unsPo.getWithFlags()) ? "TRUE" : "FALSE");
            dataList.add(Constants.withSave2db(unsPo.getWithFlags()) ? "TRUE" : "FALSE");
        } else {
            dataList.add("");
            dataList.add("");
            dataList.add("");
        }

        Set<String> labels = unsLabelNamesMap.get(unsPo.getId());
        if (CollectionUtils.isNotEmpty(labels)) {
            dataList.add(StringUtils.join(labels, ','));
        } else {
            dataList.add("");
        }
        return dataList;
    }


    /**
     * 空白关系实例
     * @param unsPo
     * @return
     */
    private static List<Object> createRowForBlankRelationInstance(UnsPo unsPo, Map<String, UnsPo> templateMap, Map<String, Set<String>> unsLabelNamesMap) {
        List<Object> dataList = new ArrayList<>(RELATION_INDEX.size());
        for(String key : RELATION_INDEX) {
            if ("topic".equals(key)) {
                dataList.add(StringUtils.isNotBlank(unsPo.getPath()) ? unsPo.getPath() : "");
            }
            if ("alias".equals(key)) {
                dataList.add(unsPo.getAlias() != null ? unsPo.getAlias() : "");
            }
            if ("template".equals(key)) {
                if (unsPo.getModelId() != null) {
                    UnsPo template = templateMap.get(unsPo.getModelId());
                    dataList.add(template != null ? template.getPath() : "");
                } else {
                    dataList.add("");
                }
            }
            if ("fields".equals(key)) {
                dataList.add(field(unsPo.getFields()));
            }
            if ("description".equals(key)) {
                dataList.add(StringUtils.isNotBlank(unsPo.getDescription()) ? unsPo.getDescription() : "");
            }
            if ("mockData".equals(key)) {
                if (unsPo.getWithFlags() != null) {
                    dataList.add(Constants.withFlow(unsPo.getWithFlags()) ? "TRUE" : "FALSE");
                } else {
                    dataList.add("");
                }
            }
            if ("autoDashboard".equals(key)) {
                if (unsPo.getWithFlags() != null) {
                    dataList.add(Constants.withDashBoard(unsPo.getWithFlags()) ? "TRUE" : "FALSE");
                } else {
                    dataList.add("");
                }
            }
            if ("persistence".equals(key)) {
                if (unsPo.getWithFlags() != null) {
                    dataList.add(Constants.withSave2db(unsPo.getWithFlags()) ? "TRUE" : "FALSE");
                } else {
                    dataList.add("");
                }
            }
            if ("label".equals(key)) {
                Set<String> labels = unsLabelNamesMap.get(unsPo.getId());
                if (CollectionUtils.isNotEmpty(labels)) {
                    dataList.add(StringUtils.join(labels, ','));
                } else {
                    dataList.add("");
                }
            }
        }
        return dataList;
    }

    public static List<Object> fillForWrite(List<Object> dataList, ExcelTypeEnum excelType) {
        List<String> head = null;
        switch (excelType) {
            case TIMESERIES:head = TIMESERIES_INDEX;break;
            case RELATION:head = RELATION_INDEX;break;
            case Folder:head = FOLDER_INDEX;break;
            case Template:head = TEMPLATE_INDEX;break;
        }
        for (int i = dataList.size(); i < head.size(); i++) {
            dataList.add("");
        }
        return dataList;
    }

    /**
     * 文件夹
     * @param unsPo
     * @return
     */
    private static List<Object> createRowForTemplate(UnsPo unsPo) {
        List<Object> dataList = new ArrayList<>(TEMPLATE_INDEX.size());
        for(String key : TEMPLATE_INDEX) {
            if ("name".equals(key)) {
                dataList.add(unsPo.getPath());
            }
            if ("fields".equals(key)) {
                dataList.add(field(unsPo.getFields()));
            }
            if ("description".equals(key)) {
                dataList.add(StringUtils.isNotBlank(unsPo.getDescription()) ? unsPo.getDescription() : "");
            }
        }
        return dataList;
    }

    private static String field(String fieldStr) {
        if (StringUtils.isNotBlank(fieldStr) && !StringUtils.equals(fieldStr, "null") && !StringUtils.equals(fieldStr, "[]")) {
            List<FieldDefineVo> list = JsonUtil.fromJson(fieldStr, new TypeReference<List<FieldDefineVo>>() {
            }.getType());
            FieldDefineVo[] field = list.stream().filter(f -> !f.getName().startsWith(Constants.SYSTEM_FIELD_PREV)).toArray(n -> new FieldDefineVo[n]);
            if (field != null && field.length > 0) {
                return JsonUtil.toJson(field);
            }
        }
        return "";
    }

    @Data
    @AllArgsConstructor
    public static class RowWrapper {
        private IOTProtocol protocol;
        private List<Object> dataList;

    }
}

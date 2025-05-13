package com.supos.uns.service.exportimport.core;

import com.supos.uns.service.exportimport.core.dto.ExcelUnsWrapDto;
import com.supos.uns.vo.CreateTemplateVo;
import lombok.Getter;

import java.util.*;

/**
 * @author sunlifang
 * @version 1.0
 * @description: TODO
 * @date 2025/4/22 19:23
 */
@Getter
public class ExcelImportContext {

    private String file;

    private Map<String, String> excelCheckErrorMap = new HashMap<>(4);
    private Map<Integer, Map<Integer, String>> error = new HashMap<>();

    private List<CreateTemplateVo> templateVoList = new LinkedList<>();

    private Set<String> labels = new HashSet<>();

    private Set<String> aliasInExcel = new HashSet<>();
    private Set<String> pathInExcel = new HashSet<>();

    //uns
    private Map<String, ExcelUnsWrapDto> unsMap = new HashMap<>();
    private List<ExcelUnsWrapDto> unsList = new LinkedList<>();


    private Set<String> checkTemplateAlias = new HashSet<>();
    private Set<String> checkLabels = new HashSet<>();

    public ExcelImportContext(String file) {
        this.file = file;
    }

    public boolean dataEmpty() {
        //TODO 待完善
        return false;
/*        return CollectionUtils.isNotEmpty(templateVoList) && CollectionUtils.isNotEmpty(topicList)
                && CollectionUtils.isNotEmpty(labels) && MapUtils.isEmpty(excelCheckErrorMap);*/
    }

    public void addError(String key, String error) {
        excelCheckErrorMap.put(key, error);
    }

    public void addAllError(Map<String, String> errorMap) {
        excelCheckErrorMap.putAll(errorMap);
    }

    public void addTemplateVo(CreateTemplateVo templateVo) {
        templateVoList.add(templateVo);
    }

    public boolean addAlias(String alias) {
        return aliasInExcel.add(alias);
    }
    public boolean addPath(String path) {
        return pathInExcel.add(path);
    }

    public void addLabel(String label) {
        labels.add(label);
    }

    public void addCheckTemplateAlias(String templateAlias) {
        checkTemplateAlias.add(templateAlias);
    }

    public void addCheckLabel(String label) {
        checkLabels.add(label);
    }

    public void clear() {
        templateVoList.clear();
        labels.clear();
        unsMap.clear();
        unsList.clear();

        checkTemplateAlias.clear();
        checkLabels.clear();
    }
}

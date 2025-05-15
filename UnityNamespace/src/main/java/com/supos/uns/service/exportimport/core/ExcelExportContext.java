package com.supos.uns.service.exportimport.core;

import com.supos.common.dto.InstanceField;
import com.supos.uns.dao.po.UnsPo;
import lombok.Getter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author sunlifang
 * @version 1.0
 * @description: ExcelExportContext
 * @date 2025/4/23 16:40
 */
@Getter
public class ExcelExportContext {

    private Map<String, UnsPo> templateMap = new HashMap<>();

    Map<String, Set<String>> unsLabelNamesMap = new HashMap<>();

    private Set<InstanceField> refers = new HashSet<>();

    public void putAllTemplate(Map<String, UnsPo> templateMap) {
        this.templateMap.putAll(templateMap);
    }

    public void computeIfAbsentLabel(String unsId, String labelName) {
        unsLabelNamesMap.computeIfAbsent(unsId, k -> new HashSet<>()).add(labelName);
    }
}

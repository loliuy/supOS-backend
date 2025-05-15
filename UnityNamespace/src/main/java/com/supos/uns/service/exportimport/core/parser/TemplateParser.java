package com.supos.uns.service.exportimport.core.parser;

import cn.hutool.core.bean.BeanUtil;
import com.supos.common.dto.FieldDefine;
import com.supos.common.enums.ExcelTypeEnum;
import com.supos.common.utils.I18nUtils;
import com.supos.common.vo.FieldDefineVo;
import com.supos.uns.service.exportimport.core.ExcelImportContext;
import com.supos.uns.service.exportimport.core.data.ExportImportData;
import com.supos.uns.service.exportimport.core.dto.ExcelTemplateDto;
import com.supos.uns.vo.CreateTemplateVo;
import jakarta.validation.ConstraintViolation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author sunlifang
 * @version 1.0
 * @description: TemplateParser
 * @date 2025/4/22 15:27
 */
@Slf4j
public class TemplateParser extends AbstractParser {

    @Override
    public void parseExcel(int batch, int index, Map<String, Object> dataMap, ExcelImportContext context) {
        Map<String, CreateTemplateVo> templateMap = new HashMap<>();

        if (isEmptyRow(dataMap)) {
            return;
        }

        ExcelTemplateDto excelTemplateDto = BeanUtil.copyProperties(dataMap, ExcelTemplateDto.class);
        excelTemplateDto.setBatch(batch);
        excelTemplateDto.setIndex(index);
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
                context.addError(batchIndex, er.toString());
                return;
            }
        }

        CreateTemplateVo templateVo = new CreateTemplateVo();
        templateVo.setPath(excelTemplateDto.getName());
        templateVo.setAlias(excelTemplateDto.getAlias());
        templateVo.setDescription(excelTemplateDto.getDescription());
        templateVo.setBatch(excelTemplateDto.getBatch());
        templateVo.setIndex(excelTemplateDto.getIndex());

        CreateTemplateVo templateInExcel = templateMap.get(templateVo.getPath());
        if (templateInExcel != null) {
            // excel 中存在重复的topic
            log.warn(I18nUtils.getMessage("uns.excel.duplicate.item", String.format("%s|%s", ExcelTypeEnum.Template.getCode(), templateInExcel.getPath())));
            return;
        }

        Pair<Boolean, FieldDefine[]> checkFieldResult = checkFields(batchIndex, excelTemplateDto.getFields(), context);
        if (checkFieldResult.getLeft()) {
            if (checkFieldResult.getRight() != null) {
                templateVo.setFields(FieldDefineVo.convert(checkFieldResult.getRight()));
            } else {
                context.addError(batchIndex, I18nUtils.getMessage("uns.field.empty"));
                return;
            }
        } else {
            return;
        }

        templateMap.put(templateVo.getPath(), templateVo);
        context.addTemplateVo(templateVo);
    }

    @Override
    public void parseJson(int batch, int index, ExportImportData data, ExcelImportContext context) {
        Map<String, CreateTemplateVo> templateMap = new HashMap<>();

        if (data == null) {
            return;
        }

        ExcelTemplateDto excelTemplateDto = BeanUtil.copyProperties(data, ExcelTemplateDto.class);
        excelTemplateDto.setBatch(batch);
        excelTemplateDto.setIndex(index);
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
                context.addError(batchIndex, er.toString());
                return;
            }
        }

        CreateTemplateVo templateVo = new CreateTemplateVo();
        templateVo.setPath(excelTemplateDto.getName());
        templateVo.setAlias(excelTemplateDto.getAlias());
        templateVo.setDescription(excelTemplateDto.getDescription());
        templateVo.setBatch(excelTemplateDto.getBatch());
        templateVo.setIndex(excelTemplateDto.getIndex());

        CreateTemplateVo templateInExcel = templateMap.get(templateVo.getPath());
        if (templateInExcel != null) {
            // excel 中存在重复的topic
            log.warn(I18nUtils.getMessage("uns.excel.duplicate.item", String.format("%s|%s", ExcelTypeEnum.Template.getCode(), templateInExcel.getPath())));
            return;
        }

        Pair<Boolean, FieldDefine[]> checkFieldResult = checkFields(batchIndex, excelTemplateDto.getFields(), context);
        if (checkFieldResult.getLeft()) {
            if (checkFieldResult.getRight() != null) {
                templateVo.setFields(FieldDefineVo.convert(checkFieldResult.getRight()));
            } else {
                context.addError(batchIndex, I18nUtils.getMessage("uns.field.empty"));
                return;
            }
        } else {
            return;
        }

        templateMap.put(templateVo.getPath(), templateVo);
        context.addTemplateVo(templateVo);
    }
}

package com.supos.adapter.mqtt.dto;

import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.core.util.ArrayUtil;
import com.supos.common.Constants;
import com.supos.common.SrcJdbcType;
import com.supos.common.dto.*;
import com.supos.common.enums.FieldType;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Data
public class TopicDefinition {
    FieldDefines fieldDefines;
    ConcurrentHashMap<String, Object> lastMsg;
    ConcurrentHashMap<String, Long> lastDt;
    Long lastDateTime;
    Set<String> referCalcTopics;// 被引用的计算实例topic
    CreateTopicDto createTopicDto;
    boolean save2db;

    public TopicDefinition(CreateTopicDto createTopicDto) {
        this.initByCreateTopicDto(createTopicDto, true);
    }

    public void addReferCalcTopic(String topic) {
        if (referCalcTopics == null) {
            referCalcTopics = new ConcurrentHashSet<>(4);
        }
        referCalcTopics.add(topic);
    }

    public void removeReferCalcTopic(String topic) {
        if (referCalcTopics != null) {
            referCalcTopics.remove(topic);
        }
    }

    public String getTopic() {
        return createTopicDto.getTopic();
    }

    public String getTable() {
        return createTopicDto.getTable();
    }

    public SrcJdbcType getJdbcType() {
        return createTopicDto.getDataSrcId();
    }

    public int getDataType() {
        return createTopicDto.getDataType();
    }

    public AlarmRuleDefine getAlarmRuleDefine() {
        return createTopicDto.getAlarmRuleDefine();
    }

    public InstanceField[] getRefers() {
        return createTopicDto.getRefers();
    }

    public Object getCompileExpression() {
        return createTopicDto.getCompileExpression();
    }

    public void setCreateTopicDto(CreateTopicDto dto) {
        initByCreateTopicDto(dto, false);
    }

    private void initByCreateTopicDto(CreateTopicDto dto, boolean init) {
        FieldDefine[] fields = dto.getFields();
        if (fields != null && fields.length > 0) {
            Map<String, FieldDefine> fieldDefineMap = Arrays.stream(fields).collect(Collectors.toMap(FieldDefine::getName, p -> p,
                    (a, b) -> a, LinkedHashMap::new));
            if (lastMsg != null && !lastMsg.isEmpty()) {
                for (String k : lastMsg.keySet()) {
                    if (!fieldDefineMap.containsKey(k)) {
                        lastMsg.remove(k);
                        if (lastDt != null) {
                            lastDt.remove(k);
                        }
                    }
                }
            }
            this.fieldDefines = new FieldDefines(fieldDefineMap);
        } else if (init) {
            this.fieldDefines = new FieldDefines();
        } else if (createTopicDto != null) {
            dto.setFields(createTopicDto.getFields());
        }
        this.createTopicDto = dto;

        Integer flags = dto.getFlags();
        if (flags != null) {
            this.save2db = Constants.withSave2db(flags);
        } else if (init) {
            this.save2db = true;
        }
        Object expr = dto.getCompileExpression();
        if (expr != null || ArrayUtil.isNotEmpty(dto.getRefers())) {
            if (dto.getDataType() == Constants.ALARM_RULE_TYPE) {
                FieldDefine rsField = fieldDefines.getFieldsMap().get(AlarmRuleDefine.FIELD_IS_ALARM);
                if (rsField == null) {
                    log.error("告警表结构错误！");
                    rsField = new FieldDefine(AlarmRuleDefine.FIELD_IS_ALARM, FieldType.BOOLEAN);
                }
                fieldDefines.setCalcField(rsField);
            } else {
                fieldDefines.setCalcField(getCalcField(fieldDefines));
            }
        }
    }

    static FieldDefine getCalcField(FieldDefines calc) {
        FieldDefine calcField = null;
        for (FieldDefine cv : calc.getFieldsMap().values()) {
            if (!cv.getName().startsWith(Constants.SYSTEM_FIELD_PREV)) {
                calcField = cv;
                break;
            }
        }
        return calcField;
    }
}

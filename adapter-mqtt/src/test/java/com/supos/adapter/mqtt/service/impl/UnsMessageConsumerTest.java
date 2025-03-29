package com.supos.adapter.mqtt.service.impl;

import com.supos.adapter.mqtt.dto.TopicDefinition;
import com.supos.common.Constants;
import com.supos.common.dto.*;
import com.supos.common.enums.FieldType;
import com.supos.common.utils.ExpressionFunctions;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class UnsMessageConsumerTest {
    Map<String, TopicDefinition> topicDefinitionMap = new HashMap<>();

    {
        for (int i = 1; i <= 3; i++) {
            String topic = "/dev/" + i;
            FieldDefine[] fields = new FieldDefine[]{new FieldDefine("tm", FieldType.DOUBLE, false, "1", null, null)};
            CreateTopicDto dto = new CreateTopicDto(topic, topic, fields);
            dto.setDataType(Constants.TIME_SEQUENCE_TYPE);
            UnsMessageConsumer.addTopicFields(topicDefinitionMap, dto);
        }
        FieldDefine[] fields = new FieldDefine[]{new FieldDefine("cv", FieldType.DOUBLE, false, "1", null, null)};
        CreateTopicDto dto = new CreateTopicDto("/calc/test", "calc", fields);
        dto.setDataType(Constants.CALCULATION_REAL_TYPE);
        InstanceField[] refers = new InstanceField[]{
                new InstanceField("/dev/1", "tm"),
                new InstanceField("/dev/2", "tm"),
        };
        dto.setRefers(refers);
        dto.setCompileExpression(ExpressionFunctions.compileExpression("a1 * 100 + a2"));
        UnsMessageConsumer.addTopicFields(topicDefinitionMap, dto);
    }

    @Test
    public void test_add2Json() {
        FieldDefine[] fields = new FieldDefine[]{
                new FieldDefine("ct", FieldType.LONG),
                new FieldDefine("tm", FieldType.DOUBLE),
                new FieldDefine("desc", FieldType.STRING),
        };
        FieldDefines fieldDefines = new FieldDefines(fields);
        Map<String, Object> bean = new LinkedHashMap<>();
        bean.put("ct", 123.45);
        bean.put("tm", 3.1415);
        bean.put("desc", "LiLei \"me mei\"");

        StringBuilder sb = new StringBuilder();
        add2Json(fieldDefines, bean, sb);

        System.out.println(sb);
    }

    static void add2Json(FieldDefines fieldDefines, Map<String, Object> bean, StringBuilder sb) {
        sb.append('{');
        final int len = sb.length();
        Map<String, FieldDefine> fieldDefineMap = fieldDefines.getFieldsMap();
        for (Map.Entry<String, Object> entry : bean.entrySet()) {
            String name = entry.getKey();
            Object v = entry.getValue();
            FieldDefine fieldDefine = fieldDefineMap.get(name);
            if (fieldDefine != null && v != null) {
                sb.append("\\\"").append(name).append("\\\":");
                FieldType fieldType = fieldDefine.getType();
                boolean isZ = fieldType == FieldType.INT || fieldType == FieldType.LONG;
                if (Number.class.isAssignableFrom(v.getClass())) {
                    sb.append(isZ ? ((Number) v).longValue() : v).append(',');
                } else {
                    sb.append("\\\"").append(v.toString().replace("\"", "\\\\\\\"")).append("\\\",");
                }
            }
        }
        if (sb.length() > len) {
            sb.setCharAt(sb.length() - 1, '}');
        } else {
            sb.append('}');
        }
    }

    @Test
    public void testCalc() {
        HashMap<String, SaveDataDto> topicData = new HashMap<>();
        final int DATA_SIZE = 3;
        ArrayList<Long>[] times = new ArrayList[2];
        for (int i = 1; i <= 2; i++) {
            times[i - 1] = new ArrayList<>(DATA_SIZE);
            String topic = "/dev/" + i;
            List<Map<String, Object>> list = new ArrayList<>(3);
            for (int k = 0; k < DATA_SIZE; k++) {
                Map<String, Object> m = new HashMap<>();
                long time = System.nanoTime() + k;
                times[i - 1].add(time);
                m.put(Constants.SYS_FIELD_CREATE_TIME, time);
                double v = 0;
                if (i == 1) {
                    v = 2 * k + 1;
                } else {
                    v = 2 * (k + 1);
                }
                m.put("tm", v);
                list.add(m);
            }
            topicData.put(topic, new SaveDataDto(topic, null, topicDefinitionMap.get(topic).getFieldDefines(), list));
        }
        TopicDefinition calc = topicDefinitionMap.get("/calc/test");
        SaveDataDto cur = topicData.get("/dev/1");
        AtomicInteger c = new AtomicInteger(0);
        Map<Long, Object[]> rs = UnsMessageConsumer.tryCalc(topicDefinitionMap, calc, cur, topicData, c);
        Assert.assertNotNull(rs);
        Assert.assertEquals(DATA_SIZE, rs.size());
        System.out.println(rs);
        for (int i = 0; i < times.length; i++) {
            System.out.printf("times[%d] = %s \n", i, times[i]);
        }

        Assert.assertEquals(rs.keySet().toString(), times[0].toString());
        Assert.assertEquals(rs.values().stream().map(ar -> ar[0]).toString(), Arrays.asList("102.0", "304.0", "506.0").toString());

        cur = topicData.get("/dev/2");
        c.set(0);
        rs = UnsMessageConsumer.tryCalc(topicDefinitionMap, calc, cur, topicData, c);
        Assert.assertNotNull(rs);
        Assert.assertEquals(DATA_SIZE, rs.size());
        System.out.println(rs);
    }
}

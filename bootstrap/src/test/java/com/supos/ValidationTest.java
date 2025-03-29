package com.supos;

import cn.hutool.core.bean.BeanUtil;
import com.supos.common.Constants;
import com.supos.common.dto.StreamOptions;
import com.supos.common.dto.StreamWindowOptions;
import com.supos.common.dto.StreamWindowOptionsCountWindow;
import com.supos.common.dto.StreamWindowOptionsInterval;
import com.supos.common.enums.StreamWindowType;
import org.junit.Assert;
import org.junit.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.HashMap;
import java.util.Set;

public class ValidationTest {

    @Test
    public void testValid_IntervalWindow() {
        System.out.println(Constants.withSave2db(7));
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        {
            StreamOptions streamOptions = new StreamOptions();
            StreamWindowOptions ops = new StreamWindowOptions();

            StreamWindowOptionsInterval window = new StreamWindowOptionsInterval();
            window.setIntervalValue("3m");
            window.setIntervalOffset("1h");

            HashMap<String, Object> opMap = BeanUtil.copyProperties(window, HashMap.class);
            ops.setOptions(opMap);
            ops.setWindowType(StreamWindowType.INTERVAL.name());
            streamOptions.setWindow(ops);
            streamOptions.setWaterMark("100s");
            Set<ConstraintViolation<Object>> violations = validator.validate(streamOptions);
            System.out.println(violations);
            Assert.assertFalse(violations.isEmpty());
        }
    }
    @Test
    public void testValid() {

        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        {
            StreamOptions streamOptions = new StreamOptions();
            StreamWindowOptions ops = new StreamWindowOptions();

            StreamWindowOptionsCountWindow countWindow = new StreamWindowOptionsCountWindow();
            countWindow.setCountValue(10);
            countWindow.setSlidingValue(3);

            HashMap<String, Object> opMap = BeanUtil.copyProperties(countWindow, HashMap.class);
            ops.setOptions(opMap);
            ops.setWindowType(StreamWindowType.COUNT_WINDOW.name());
            streamOptions.setWindow(ops);
            streamOptions.setWaterMark("100");
            Set<ConstraintViolation<Object>> violations = validator.validate(streamOptions);
            System.out.println(violations);
        }
        {
            StreamOptions streamOptions = new StreamOptions();
            StreamWindowOptions ops = new StreamWindowOptions();
            HashMap<String, Object> opMap = new HashMap<>();
            ops.setOptions(opMap);
            opMap.put("intervalValue", "190s");
            ops.setWindowType(StreamWindowType.INTERVAL.name());
            streamOptions.setWindow(ops);
            Set<ConstraintViolation<Object>> violations = validator.validate(streamOptions);
            System.out.println(violations);
        }
    }
}

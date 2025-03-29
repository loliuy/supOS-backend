package com.supos.common.annotation;

import com.supos.common.enums.TimeUnits;
import com.supos.common.utils.IntegerUtils;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import static com.supos.common.utils.ConstraintErrTipUtils.setErrMsg;

public class StreamTimeConstraint implements ConstraintValidator<StreamTimeValidator, String> {
    StreamTimeValidator validator;

    @Override
    public void initialize(StreamTimeValidator constraintAnnotation) {
        validator = constraintAnnotation;
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.length() == 0) {
            return true;
        }
        boolean ok = false;
        if (value.length() >= 2) {
            value = value.trim();
            Integer timeNum = IntegerUtils.parseInt(value.substring(0, value.length() - 1).trim());
            if (timeNum != null) {
                char unit = value.charAt(value.length() - 1);
                ok = TimeUnits.of(unit) != null;
            }
        }
        if (!ok) {
            setErrMsg(context, validator.field(), "uns.invalid.stream.time", value);
        }
        return ok;
    }
}
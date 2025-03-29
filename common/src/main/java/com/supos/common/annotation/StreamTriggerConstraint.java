package com.supos.common.annotation;

import com.supos.common.enums.TimeUnits;
import com.supos.common.utils.IntegerUtils;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import static com.supos.common.utils.ConstraintErrTipUtils.setErrMsg;

public class StreamTriggerConstraint implements ConstraintValidator<StreamTriggerValidator, String> {

    StreamTriggerValidator validator;

    @Override
    public void initialize(StreamTriggerValidator constraintAnnotation) {
        validator = constraintAnnotation;
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.length() == 0) {
            return true;
        }
        boolean ok = false;
        if ("AT_ONCE".equals(value) || "WINDOW_CLOSE".equals(value)) {
            ok = true;
        } else if (value.startsWith("MAX_DELAY ")) {
            int b = value.indexOf(' ');
            String time = value.substring(b + 1).trim();
            if (time.length() >= 2) {
                Integer num = IntegerUtils.parseInt(time.substring(0, time.length() - 1));
                if (num != null) {
                    char unit = time.charAt(time.length() - 1);
                    ok = TimeUnits.of(unit) != null;
                }
            }
        }
        if (!ok) {
            setErrMsg(context, validator.field(), "uns.invalid.stream.trigger", value);
        }
        return ok;
    }
}
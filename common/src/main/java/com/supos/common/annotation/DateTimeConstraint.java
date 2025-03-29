package com.supos.common.annotation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

import static com.supos.common.utils.ConstraintErrTipUtils.setErrMsg;

public class DateTimeConstraint implements ConstraintValidator<DateTimeValidator, String> {

    DateTimeValidator validator;

    @Override
    public void initialize(DateTimeValidator constraintAnnotation) {
        validator = constraintAnnotation;
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.length() == 0) {
            return true;
        }
        boolean ok = false;
        if (value.length() >= 4) {
            value = value.trim();
            ok = parseDate(value) != null;
        }
        if (!ok) {
            setErrMsg(context, validator.field(), "uns.stream.invalid.datetime", value);
        }
        return ok;
    }

    static final String[] formats = new String[]{"yyyy-MM-dd", "yyyy-MM-dd HH", "yyyy-MM-dd HH:mm", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm:ss.SSSSSSSSS"};
    static final DateTimeFormatter[] formatters = new DateTimeFormatter[formats.length];

    static {
        for (int i = 0; i < formats.length; i++) {
            formatters[i] = DateTimeFormatter.ofPattern(formats[i]);
        }
    }

    public static String parseDate(String value) {
        for (int i = formats.length - 1; i >= 0; i--) {
            String fmt = formats[i];
            if (value.length() >= fmt.length()) {
                value = value.substring(0, fmt.length());
                TemporalAccessor time = null;
                DateTimeFormatter formatter = formatters[i];
                try {
                    time = formatter.parse(value);
                    String rs = formatters[formatters.length - 1].format(time);
                    return rs;
                } catch (Exception ex) {
                    if (time != null) {
                        try {
                            String rs = formatter.format(time);
                            return rs;
                        } catch (Exception e) {
                        }
                    }
                }
            }
        }
        return null;
    }
}
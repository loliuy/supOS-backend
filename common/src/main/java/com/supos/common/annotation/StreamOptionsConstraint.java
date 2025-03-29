package com.supos.common.annotation;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.supos.common.dto.StreamOptions;
import com.supos.common.dto.StreamWindowOptions;
import com.supos.common.dto.StreamWindowOptionsCountWindow;
import com.supos.common.dto.StreamWindowOptionsInterval;
import com.supos.common.enums.StreamWindowType;
import com.supos.common.enums.TimeUnits;
import com.supos.common.utils.ExpressionUtils;
import com.supos.common.utils.IntegerUtils;

import jakarta.validation.*;
import java.util.Map;
import java.util.Set;

import static com.supos.common.annotation.DateTimeConstraint.parseDate;
import static com.supos.common.utils.ConstraintErrTipUtils.setErrMsg;

public class StreamOptionsConstraint implements ConstraintValidator<StreamOptionsValidator, StreamOptions> {
    private Validator validator;

    @Override
    public void initialize(StreamOptionsValidator constraintAnnotation) {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
    }


    @Override
    public boolean isValid(StreamOptions options, ConstraintValidatorContext context) {
        if (options == null || options.getWindow() == null) {
            return false;
        }
        StreamWindowOptions window = options.getWindow();
        StreamWindowType windowType = StreamWindowType.of(window.getWindowType());
        if (windowType == null) {
            setErrMsg(context, "uns.invalid.stream.window.type", window.getWindowType());
            return false;
        }
        window.setStreamWindowType(windowType);
        Map<String, Object> opt = window.getOptions();
        if (opt == null || opt.isEmpty()) {
            setErrMsg(context, "window.options", "uns.invalid.stream.window.emptyOptions");
            return false;
        }
        Object opObj = windowType.buildOptions();
        BeanUtil.copyProperties(opt, opObj);
        // Validate the options object
        Set<ConstraintViolation<Object>> violations = validator.validate(opObj);
        if (violations.isEmpty()) {
            if (!checkSQLExpr(context, options.getWhereCondition(), "whereCondition", false)) {
                return false;
            }
            if (!checkSQLExpr(context, options.getHavingCondition(), "havingCondition", true)) {
                return false;
            }
            if (!checkTrigger(context, options.getTrigger())) {
                return false;
            }
            if (!checkMark(context, options.getWaterMark(), "waterMark")) {
                return false;
            }
            if (!checkMark(context, options.getDeleteMark(), "deleteMark")) {
                return false;
            }
            if (!checkDateTime(context, options.getStartTime(), "startTime")) {
                return false;
            }
            if (!checkDateTime(context, options.getEndTime(), "endTime")) {
                return false;
            }
        }
        // If there are violations, handle them as needed
        if (!violations.isEmpty()) {
            context.disableDefaultConstraintViolation();
            for (ConstraintViolation violation : violations) {
                context.buildConstraintViolationWithTemplate(violation.getPropertyPath() + ": " + violation.getMessage())
                        .addConstraintViolation();
            }
            return false; // The main object is not valid
        } else if (opObj instanceof StreamWindowOptionsCountWindow) {
            StreamWindowOptionsCountWindow countWindow = (StreamWindowOptionsCountWindow) opObj;
            options.setIgnoreExpired(null);
            Integer sliding = countWindow.getSlidingValue();
            if (sliding != null) {
                int sd = sliding.intValue();
                String propertyPath = "window.options.slidingValue";
                if (sd > countWindow.getCountValue()) {
                    setErrMsg(context, propertyPath, "uns.invalid.stream.window.slidingGtCount");
                    return false;
                } else if (sd == 0) {
                    setErrMsg(context, propertyPath, "uns.invalid.stream.window.slidingLt1");
                    return false;
                }
            }
            if (options.getWaterMark() == null || options.getWaterMark().startsWith("0")) {
                setErrMsg(context, "waterMark", "uns.invalid.stream.window.countWindow.waterMarkEmpty");
                return false;
            }
        } else if (opObj instanceof StreamWindowOptionsInterval) {
            StreamWindowOptionsInterval intervalOpt = (StreamWindowOptionsInterval) opObj;
            Long offset = TimeUnits.toNanoSecond(intervalOpt.getIntervalOffset());
            if (offset != null) {
                Long interval = TimeUnits.toNanoSecond(intervalOpt.getIntervalValue());
                if (interval == null || offset >= interval) {
                    setErrMsg(context, "options.intervalValue", "uns.invalid.stream.window.intervalGtOffset");
                    return false;
                }
            }
        }
        window.setOptionBean(opObj);
        return true;
    }

    private boolean checkSQLExpr(ConstraintValidatorContext context, String value, String field, boolean isHaving) {
        if (value == null || value.isEmpty()) {
            return true;
        }
        boolean ok = false;
        try {
            ExpressionUtils.ParseResult rs = ExpressionUtils.parseExpression(value);
            if (rs.isBooleanResult) {
                if (isHaving) {// having 条件， 只能含有聚合函数
                    if (!rs.functions.isEmpty() && CollectionUtil.isEqualList(rs.functions, rs.aggregateFunctions)) {
                        ok = true;
                    }
                } else {// where 条件，不能含有聚合函数
                    if (rs.aggregateFunctions.isEmpty()) {
                        ok = true;
                    }
                }
            } else {
                setErrMsg(context, field, "uns.invalid.stream.NotBoolCondition", value);
                return false;
            }
        } catch (Exception ex) {
        }
        if (!ok) {
            setErrMsg(context, field, "uns.invalid.stream.invalidCondition", value);
        }
        return ok;
    }

    private static final long min_MAX_DELAY = TimeUnits.Second.toNanoSecond(5);

    private boolean checkTrigger(ConstraintValidatorContext context, String value) {
        if (value == null || value.isEmpty()) {
            return true;
        }
        boolean ok = false;
        if ("AT_ONCE".equals(value) || "WINDOW_CLOSE".equals(value)) {
            ok = true;
        } else if (value.startsWith("MAX_DELAY ")) {
            int b = value.indexOf(' ');
            String time = value.substring(b + 1).trim();
            if (time.length() >= 2) {
                final long nanoSecond = TimeUnits.toNanoSecond(time);
                if (nanoSecond > min_MAX_DELAY) {
                    ok = true;
                } else {
                    setErrMsg(context, "trigger", "uns.invalid.stream.minDelay", value);
                    return false;
                }
            }
        }
        if (!ok) {
            setErrMsg(context, "trigger", "uns.invalid.stream.trigger", value);
        }
        return ok;
    }


    private boolean checkMark(ConstraintValidatorContext context, String value, String field) {
        if (value == null || value.isEmpty()) {
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
            setErrMsg(context, field, "uns.invalid.stream.time", value);
        }
        return ok;
    }

    private boolean checkDateTime(ConstraintValidatorContext context, String value, String field) {
        if (value == null || value.isEmpty()) {
            return true;
        }
        boolean ok = false;
        if (value.length() >= 4) {
            value = value.trim();
            ok = parseDate(value) != null;
        }
        if (!ok) {
            setErrMsg(context, field, "uns.stream.invalid.datetime", value);
        }
        return ok;
    }
}
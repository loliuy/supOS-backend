package com.supos.common.utils;

import jakarta.validation.ConstraintValidatorContext;

public class ConstraintErrTipUtils {

    public static void setErrMsg(ConstraintValidatorContext context, String propertyPath, String messageKey, String... args) {
        String message = propertyPath + ": " + I18nUtils.getMessage(messageKey, args);
        context.disableDefaultConstraintViolation();
        ConstraintValidatorContext.ConstraintViolationBuilder ctx = context.buildConstraintViolationWithTemplate(message);
        ctx.addConstraintViolation();
    }
}

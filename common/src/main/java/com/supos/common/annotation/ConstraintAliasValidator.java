package com.supos.common.annotation;

import com.supos.common.Constants;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

public class ConstraintAliasValidator implements ConstraintValidator<AliasValidator, String> {

    private static final Pattern ALIAS_PATTERN = Pattern.compile(Constants.ALIAS_REG);

    @Override
    public boolean isValid(String alias, ConstraintValidatorContext context) {
        if (StringUtils.isBlank(alias)) {
            return true;
        }
        if (ALIAS_PATTERN.matcher(alias).matches()) {
            return true;
        }
        return false;
    }
}

package com.supos.common.annotation;

import com.supos.common.Constants;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DataTypeConstraint implements ConstraintValidator<DataTypeValidator, Integer> {
    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return Constants.isValidDataType(value);
    }
}

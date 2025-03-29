package com.supos.common.annotation;

import com.supos.common.enums.FieldType;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ConstraintFieldTypeValidator implements ConstraintValidator<FieldTypeValidator, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return FieldType.getByName(value) != null;
    }
}

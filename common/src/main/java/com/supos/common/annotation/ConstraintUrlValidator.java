package com.supos.common.annotation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.net.MalformedURLException;
import java.net.URL;

public class ConstraintUrlValidator implements ConstraintValidator<UrlValidator, String> {
    @Override
    public void initialize(UrlValidator annotation) {
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        if (!value.startsWith("http")) {
            return false;
        }
        try {
            new URL(value);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }
}
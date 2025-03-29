package com.supos.common.annotation;

import com.supos.common.utils.PathUtil;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ConstraintTopicNameValidator implements ConstraintValidator<TopicNameValidator, String> {

    @Override
    public boolean isValid(String topic, ConstraintValidatorContext context) {
        if (topic == null) {
            return true;
        }
        return PathUtil.validTopicFormate(topic, null);
    }
}
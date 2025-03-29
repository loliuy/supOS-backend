package com.supos.common.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ConstraintTopicNameValidator.class)
public @interface TopicNameValidator {
    String message() default "uns.invalid.topic";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

package com.supos.common.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ConstraintFieldTypeValidator.class)
public @interface FieldTypeValidator {
    String message() default "uns.invalid.field.type";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

package com.supos.common.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD,ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = StreamOptionsConstraint.class)
public @interface StreamOptionsValidator {
    String message() default "uns.invalid.stream.window";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

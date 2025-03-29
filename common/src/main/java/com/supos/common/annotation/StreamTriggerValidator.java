package com.supos.common.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = StreamTriggerConstraint.class)
public @interface StreamTriggerValidator {
    String message() default "uns.invalid.stream.trigger";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String field() default "";
}

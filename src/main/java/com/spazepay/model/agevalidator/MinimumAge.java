package com.spazepay.model.agevalidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = MinimumAgeValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface MinimumAge {
    String message() default "Must be at least {value} years old";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    int value() default 18; // Default minimum age
}

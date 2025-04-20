package com.spazepay.model.agevalidator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import java.time.Period;

public class MinimumAgeValidator implements ConstraintValidator<MinimumAge, LocalDate> {
    private int minAge;

    @Override
    public void initialize(MinimumAge constraintAnnotation) {
        this.minAge = constraintAnnotation.value();
    }

    @Override
    public boolean isValid(LocalDate dateOfBirth, ConstraintValidatorContext context) {
        if (dateOfBirth == null) {
            return false; // @NotNull will handle this, but we include it for safety
        }
        return Period.between(dateOfBirth, LocalDate.now()).getYears() >= minAge;
    }
}

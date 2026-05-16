package com.easyschedule.backend.shared.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;
import java.time.Period;

public class EdadRangeValidator implements ConstraintValidator<EdadRange, LocalDate> {
    private int min;
    private int max;

    @Override
    public void initialize(EdadRange constraintAnnotation) {
        this.min = constraintAnnotation.min();
        this.max = constraintAnnotation.max();
    }

    @Override
    public boolean isValid(LocalDate value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        LocalDate today = LocalDate.now();
        if (value.isAfter(today)) {
            return false;
        }

        int age = Period.between(value, today).getYears();
        return age >= min && age <= max;
    }
}

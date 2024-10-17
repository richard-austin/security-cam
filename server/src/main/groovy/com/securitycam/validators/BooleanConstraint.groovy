package com.securitycam.validators

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class BooleanConstraint implements ConstraintValidator<IsBoolean, Boolean> {
    @Override
    void initialize(IsBoolean isBoolean) {
        def x = isBoolean
    }

    @Override
    boolean isValid(Boolean booleanField, ConstraintValidatorContext ctx) {
        return booleanField != null
    }
}

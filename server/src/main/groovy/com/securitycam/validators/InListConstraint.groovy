package com.securitycam.validators


import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class InListConstraint   implements ConstraintValidator<InListStr, String> {
    String[] values = []
    @Override
    void initialize(InListStr constraintAnnotation) {
        super.initialize(constraintAnnotation)

        values = constraintAnnotation.values()
    }

    @Override
    boolean isValid(String value, ConstraintValidatorContext context) {
        final boolean retVal = values.contains(value)
        return retVal
    }
}

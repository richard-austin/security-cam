package com.securitycam.validators

import org.springframework.validation.BindingResult
import org.springframework.validation.DataBinder
import org.springframework.validation.Validator

class GeneralValidator {
    Object target
    Validator validator
    GeneralValidator(Object target, Validator validator) {
        this.target = target
        this.validator = validator
    }

    BindingResult validate() {
        DataBinder binder = new DataBinder(target)
        binder.setValidator(validator)
        // validate the target object
        binder.validate()
        return binder.getBindingResult()
    }
}

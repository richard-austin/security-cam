package com.securitycam.validators

import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError

class BadRequestResult extends HashMap<String, String>{
    private final BindingResult results

    BadRequestResult(BindingResult results) {
        this.results = results
        if(results.hasErrors())
        {
            results.allErrors.forEach {
                if(it instanceof FieldError) {
                    FieldError fe = it
                    put(fe.field, fe.code)
                }
            }
        }
    }
}

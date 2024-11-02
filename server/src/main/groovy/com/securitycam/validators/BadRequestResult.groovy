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

    @Override
    String toString() {
        StringBuilder errors = new StringBuilder()
        errors.append("{")
        boolean first = true;
        results.allErrors.forEach {
            if (first) {
                errors.append(', ')
                first = false
            }
            if (it instanceof FieldError) {
                FieldError fe = it
                errors.append(fe.field + ": ")
                errors.append(fe.code)
            }
        }
        errors.append("}")
        return errors.toString()
    }
}

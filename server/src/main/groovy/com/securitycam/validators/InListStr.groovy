package com.securitycam.validators

import jakarta.validation.Constraint
import org.springframework.messaging.handler.annotation.Payload

import java.lang.annotation.Documented
import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

@Documented
@Constraint(validatedBy = InListConstraint.class)
@Target([ElementType.METHOD, ElementType.FIELD])
@Retention(RetentionPolicy.RUNTIME)
@interface InListStr {
    String message() default "Not a valid value";
    Class<?>[] groups() default []
    Class<? extends Payload>[] payload() default []
    String [] values() default []
}

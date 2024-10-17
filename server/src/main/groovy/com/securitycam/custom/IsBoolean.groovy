package com.securitycam.custom

import jakarta.validation.Constraint
import org.springframework.messaging.handler.annotation.Payload

import java.lang.annotation.*

@Documented
@Constraint(validatedBy = BooleanConstraint.class)
@Target([ElementType.METHOD, ElementType.FIELD])
@Retention(RetentionPolicy.RUNTIME)
@interface  IsBoolean {
    String message() default "Boolean value should not be null";
    Class<?>[] groups() default []
    Class<? extends Payload>[] payload() default []

}

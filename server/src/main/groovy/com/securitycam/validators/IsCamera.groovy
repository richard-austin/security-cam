package com.securitycam.validators

import jakarta.validation.Constraint
import org.springframework.messaging.handler.annotation.Payload

import java.lang.annotation.Documented
import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

@Documented
@Constraint(validatedBy = CameraConstraint.class)
@Target([ElementType.METHOD, ElementType.FIELD])
@Retention(RetentionPolicy.RUNTIME)
@interface IsCamera {
    String message() default "Camera data is required";
    Class<?>[] groups() default []
    Class<? extends Payload>[] payload() default []
}

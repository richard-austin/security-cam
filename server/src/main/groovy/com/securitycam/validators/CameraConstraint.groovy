package com.securitycam.validators

import com.securitycam.controllers.Camera
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class CameraConstraint  implements ConstraintValidator<IsCamera, Camera> {
    @Override
    void initialize(IsCamera isCamera) {
    }

    @Override
    boolean isValid(Camera cam, ConstraintValidatorContext context) {
        // TODO: Make this more comprehensive
        return cam != null && cam.name != ""
    }
}

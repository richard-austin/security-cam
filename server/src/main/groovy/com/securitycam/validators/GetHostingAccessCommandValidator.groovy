package com.securitycam.validators


import com.securitycam.commands.GetHostingAccessCommand
import org.springframework.validation.Errors
import org.springframework.validation.Validator

class GetHostingAccessCommandValidator implements Validator {
    @Override
    boolean supports(Class<?> clazz) {
        return GetHostingAccessCommand.class == clazz
    }

    @Override
    void validate(Object target, Errors errors) {
        if (target instanceof GetHostingAccessCommand) {

            if (target.host == null)
                errors.rejectValue("host", "=host must not be null")
            else if (!target.host.matches(CameraParamsCommandValidator.hostNameRegex) &&
                    !target.host.matches(CameraParamsCommandValidator.ipV4RegEx) &&
                    !target.host.matches(CameraParamsCommandValidator.ipV6RegEx))
                errors.rejectValue("host", "Camera http host address format is invalid (${target.host})")
            if (target.port < 1 || target.port > 65535)
                errors.rejectValue("port", "port number is invalid")
        }
    }
}

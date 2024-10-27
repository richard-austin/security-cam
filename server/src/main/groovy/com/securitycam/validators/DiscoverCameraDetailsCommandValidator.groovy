package com.securitycam.validators

import com.securitycam.commands.DiscoverCameraDetailsCommand
import org.springframework.validation.Errors
import org.springframework.validation.Validator

class DiscoverCameraDetailsCommandValidator implements Validator {
    @Override
    boolean supports(Class<?> clazz) {
        return DiscoverCameraDetailsCommand.class == clazz
    }

    @Override
    void validate(Object cmd, Errors errors) {
        if (cmd instanceof DiscoverCameraDetailsCommand) {
            if (cmd.onvifUrl != null && cmd.onvifUrl != "") {
                if (!cmd.onvifUrl.matches(/^(?:http:\/\/)?[\w.-]+(?:\.[\w.-]+)+[\w\-._~:\/?#@!$&'()*+,;=]+$/))
                    errors.rejectValue("onvifUrl", "onvifUrl is not a valid URL")
            }
            validateOnvifUserName(cmd, errors)
            validateOnvifPassword(cmd, errors)
        }
    }

    private static void validateOnvifUserName(DiscoverCameraDetailsCommand cmd, Errors errors) {
        if (cmd.onvifUserName == null || cmd.onvifUserName == "")
            return
        if (cmd.onvifUserName.length() > 20)
            errors.rejectValue("onvifUserName", "onvifUserName exceeds maximum length (20)")
        if (cmd.onvifPassword == null || cmd.onvifPassword == "") {
            errors.rejectValue("onvifUserName", "If onvifUserName is empty, onvifPassword must also be empty")
        } else if (!cmd.onvifUserName.matches(/^[a-zA-Z0-9](_(?!([._]))|\.(?!([_.]))|[a-zA-Z0-9]){3,18}[a-zA-Z0-9]$/))
            errors.rejectValue("onvifUserName", "Invalid user name, must be 5-20 characters containing a-z, A-Z, 0-9 . and _ and starting with an alpha numeric character.")
    }

    private static void validateOnvifPassword(DiscoverCameraDetailsCommand cmd, Errors errors) {
        if (cmd.onvifPassword == null || cmd.onvifPassword == "")
            return
        if (cmd.onvifPassword.length() > 25)
            errors.rejectValue("onvifPassword", "onvifPassword exceeds maximum length (25)")
        if (cmd.onvifUserName == null || cmd.onvifUserName == "")
            errors.rejectValue("onvifPassword", "If onvifPassword is empty, onvifUserName must also be empty")
        if (!cmd.onvifPassword.matches(/^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&])[A-Za-z\d@$!%*#?&]{8,64}$/))
            errors.rejectValue("onvifPassword", "Invalid password, must be minimum eight characters, at least one letter, one number and one special character.")
    }
}


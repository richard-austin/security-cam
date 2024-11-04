package com.securitycam.validators

import com.securitycam.commands.SendResetPasswordLinkCommand
import com.securitycam.services.UtilsService
import org.springframework.validation.Errors
import org.springframework.validation.Validator

class SendResetPasswordLinkCommandValidator implements Validator {
    @Override
    boolean supports(Class<?> clazz) {
        return SendResetPasswordLinkCommandValidator.class == clazz
    }

    @Override
    void validate(Object target, Errors errors) {
        if (target instanceof SendResetPasswordLinkCommand) {
            if (target.email == null || target.email == "")
                errors.rejectValue("email", "email address must not be null or blank")
            else if (!target.email.matches(UtilsService.emailRegex))
                errors.rejectValue("email", "Invalid email address")

            if (target.clientUri == null || target.clientUri == "")
                errors.rejectValue("clientUri", "clientUri must not be null or blank")
            else {
                try {
                    new URI(target.clientUri)
                }
                catch (URISyntaxException ignored) {
                    errors.rejectValue("clientUri", "Badly formed URL")
                }
            }
        }
    }
}


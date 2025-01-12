package com.securitycam.validators

import com.securitycam.commands.ResetPasswordFromLinkCommand
import com.securitycam.services.UtilsService
import org.springframework.validation.Errors
import org.springframework.validation.Validator

class ResetPasswordFromLinkCommandValidator implements Validator{
    @Override
    boolean supports(Class<?> clazz) {
        return ResetPasswordFromLinkCommand.class == clazz
    }

    @Override
    void validate(Object target, Errors errors) {
        if(target instanceof ResetPasswordFromLinkCommand) {

            if (target.resetKey == null || target.resetKey == "")
                errors.rejectValue("resetKey", "resetKey must not be null or blank")

            if (target.newPassword == null || target.newPassword == "")
                errors.rejectValue("newPassword", "newPassword must not be null or blank")
            else {
                if (!target.newPassword.matches(UtilsService.passwordRegex))
                    errors.rejectValue("newPassword", "Invalid password, must be minimum eight characters, at least one letter, one number and one special character. (must be <= 64 characters)")
            }
            if (target.confirmNewPassword != target.newPassword)
                errors.rejectValue("newPassword", "New passwords do not match")
        }
    }
}

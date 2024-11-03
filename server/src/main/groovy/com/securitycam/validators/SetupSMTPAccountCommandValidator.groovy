package com.securitycam.validators

import com.securitycam.commands.SetupSMTPAccountCommand
import com.securitycam.services.UserAdminService
import com.securitycam.services.UtilsService
import org.springframework.validation.Errors
import org.springframework.validation.Validator

class SetupSMTPAccountCommandValidator extends CheckNotGuestCommandValidator {
    final UserAdminService userAdminService

    SetupSMTPAccountCommandValidator(UserAdminService userAdminService) {
        super(userAdminService)
        this.userAdminService = userAdminService
    }

    @Override
    boolean supports(Class<?> clazz) {
        return SetupSMTPAccountCommand.class == clazz
    }

    @Override
    void validate(Object target, Errors errors) {
        super.validate(target, errors)
        if (target instanceof SetupSMTPAccountCommand) {
            def response = userAdminService.isGuest()
            if (response.responseObject.guestAccount)
                errors.rejectValue("auth", "Guest not authorised to administer SMTP client account")

            if (target.auth) {
                if (target.username == null || target.username == "")
                    errors.rejectValue("username", "username is required")
                else if (target.username.size() > 50)
                    errors.rejectValue("usernamee", "Maximum username length is 50 characters")

                if (target.password == null || target.password == "")
                    errors.rejectValue("password", "password is required")
                else if (target.password.size() > 50)
                    errors.rejectValue("password", "Maximum password length is 50 characters")
                if (target.confirmPassword != target.password)
                    errors.rejectValue("password", "password and confirmPassword must match")
            }

            if (target.enableStartTLS) {
                if (target.sslProtocols != "TLSv1.2" && target.sslProtocols != "TLSv1.3") {
                    errors.rejectValue("sslProtocols", "sslProtocols should be TLSv1.2 or TLSv1.3 if enableStartTLS is true")

                    if (target.sslTrust == null || target.sslTrust == "") {
                        errors.rejectValue("sslTrust", "sslTrust is required if enableStartTLS is true")
                    }
                }
            }
            if (target.host == null || target.host == "")
                errors.rejectValue("host", "host must not be null or blank")
            else if (target.host.length() < 1 || target.host.length() > 50)
                errors.rejectValue("host", "host length must be between 1 and 50")
            if (target.port < 1 || target.port > 65535)
                errors.rejectValue("port", "port should be between 1 and 65536")

            if (target.fromAddress == null || target.fromAddress == "")
                errors.rejectValue("fromAddress", "fromAddress must not be null or empty")
            else if (!target.fromAddress.matches(UtilsService.emailRegex))
                errors.rejectValue("fromAddress", "Email format is not valid")
        }
    }
}

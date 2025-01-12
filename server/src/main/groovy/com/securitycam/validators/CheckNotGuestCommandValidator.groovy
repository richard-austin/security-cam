package com.securitycam.validators

import com.securitycam.commands.CheckNotGuestCommand
import com.securitycam.services.UserAdminService
import org.springframework.validation.Errors
import org.springframework.validation.Validator

class CheckNotGuestCommandValidator implements Validator {
    UserAdminService userAdminService
    CheckNotGuestCommandValidator(UserAdminService userAdminService) {
        this.userAdminService = userAdminService
    }

    @Override
    boolean supports(Class<?> clazz) {
        return CheckNotGuestCommand.class == clazz
    }

    @Override
    void validate(Object target, Errors errors) {
        if(CheckNotGuestCommand.isAssignableFrom(target.class)) {
            def result = userAdminService.isGuest()
            if(result.responseObject.guestAccount)
                errors.rejectValue("userAccount", "Guest not authorised to use this function")        }
    }
}

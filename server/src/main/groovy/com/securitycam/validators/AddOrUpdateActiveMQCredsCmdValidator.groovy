package com.securitycam.validators

import com.securitycam.commands.AddOrUpdateActiveMQCredsCmd
import com.securitycam.services.UserAdminService
import com.securitycam.services.UtilsService
import org.springframework.validation.Errors

class AddOrUpdateActiveMQCredsCmdValidator extends CheckNotGuestCommandValidator {
    UserAdminService userAdminService

    AddOrUpdateActiveMQCredsCmdValidator(UserAdminService userAdminService) {
        super(userAdminService)
        this.userAdminService = userAdminService
    }

    public static final activeMQPasswordRegex = /^$|^[A-Za-z0-9]{20}$/

    @Override
    boolean supports(Class clazz) {
        return AddOrUpdateActiveMQCredsCmd.class == clazz
    }

    @Override
    void validate(Object target, Errors errors) {

        if (target instanceof AddOrUpdateActiveMQCredsCmd) {
            def response = userAdminService.isGuest()
            if (response.responseObject.guestAccount)
                errors.rejectValue("guest", "Guest not authorised to administer ActiveMQ Credentials")

            if (target.username != null && target.username != "") {
                if (target.username == null)
                    target.username = target.username = ""
                if (!target.username.matches(UtilsService.usernameRegex))
                    errors.rejectValue("username", "Format or length of username is incorrect")
            }
            if (target.password != null && target.password != "") {
                if (target.password == null)
                    target.password = target.password = ""
                if (!target.password.matches(activeMQPasswordRegex))
                    errors.rejectValue("password", "Password is invalid")
            }

            if (target.username == "" && target.password != "")
                errors.rejectValue("password", "Password must be blank if username is blank")
            else if (target.username != "" && target.password == "")
                errors.rejectValue("username", "Password cannot be empty if username is not empty")


            if (target.confirmPassword == null)
                target.confirmPassword = target.confirmPassword = ""
            if (target.confirmPassword != target.password)
                errors.rejectValue("password", "Password and confirm password do not match")

            if (target.mqHost == null || target.mqHost == "")
                errors.rejectValue("mqHost", "mqHost must not be null or empty")
            else {
                if (!target.mqHost.matches(CameraParamsCommandValidator.hostNameRegex) &&
                        !target.mqHost.matches(CameraParamsCommandValidator.ipV4RegEx) &&
                        !target.mqHost.matches(CameraParamsCommandValidator.ipV6RegEx)) {
                    errors.rejectValue("mqHost", "Host name is invalid")
                }
            }
        }
    }
}

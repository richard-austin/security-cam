package com.securitycam.validators

import com.fasterxml.jackson.databind.ObjectMapper
import com.securitycam.commands.PtzCommand
import com.securitycam.controllers.CameraAdminCredentials
import com.securitycam.interfaceobjects.Asymmetric
import com.securitycam.services.LogService
import com.securitycam.services.UtilsService
import org.springframework.validation.Errors
import org.springframework.validation.Validator

class PtzCommandValidator implements Validator {
    LogService logService

    PtzCommandValidator(LogService logService) {
        this.logService = logService
    }

    @Override
    boolean supports(Class<?> clazz) {
        return PtzCommand.class == clazz
    }

    @Override
    void validate(Object target, Errors errors) {
        if (PtzCommand.isAssignableFrom(target.class)) {
            PtzCommand cmd = target as PtzCommand
            if (cmd.creds != null && cmd.creds != "") {
                // Decrypt creds and set the user name and password
                Asymmetric asym = new Asymmetric()
                String jsonCreds = asym.decrypt(cmd.creds)
                ObjectMapper mapper = new ObjectMapper()
                logService.cam.debug("Before decryption")
                if (jsonCreds.length() > 0) {
                    CameraAdminCredentials cac = mapper.readValue(jsonCreds, CameraAdminCredentials.class)
                    cmd.user = cac.userName
                    cmd.password = cac.password
                    logService.cam.debug("After decryption")
                }
            }

            if(cmd.onvifBaseAddress == null)
                errors.rejectValue("onvifBaseAddress", "onvifBaseAddress cannot be null")
            else if(cmd.onvifBaseAddress == "")
                errors.rejectValue("onvifBaseAddress", "onvifBaseAddress cannot be empty")
            else if(!cmd.onvifBaseAddress.matches(UtilsService.onvifBaseAddressRegex))
                errors.rejectValue("onvifBaseAddress", "Invalid onvifBaseAddress ${cmd.onvifBaseAddress}")
        }
    }
}

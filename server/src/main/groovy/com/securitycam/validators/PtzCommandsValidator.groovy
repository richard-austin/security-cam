package com.securitycam.validators

import com.fasterxml.jackson.databind.ObjectMapper
import com.securitycam.commands.PtzCommands
import com.securitycam.controllers.CameraAdminCredentials
import com.securitycam.interfaceobjects.Asymmetric
import com.securitycam.services.LogService
import org.springframework.validation.Errors
import org.springframework.validation.Validator

class PtzCommandsValidator implements Validator {
    LogService logService
    PtzCommandsValidator(LogService logService) {
        this.logService = logService
    }

    @Override
    boolean supports(Class<?> clazz) {
        return PtzCommandsValidator.class == clazz
    }

    @Override
    void validate(Object target, Errors errors) {
        if (PtzCommands.isAssignableFrom(target.class)) {
            PtzCommands cmd = target as PtzCommands
            if (cmd.creds != null && cmd.creds != "") {
                // Decrypt creds and set the user name and password
                Asymmetric asym = new Asymmetric()
                String jsonCreds = asym.decrypt(cmd.creds)
                ObjectMapper mapper = new ObjectMapper()
                logService.cam.info("Before decryption")
                if (jsonCreds.length() > 0) {
                    CameraAdminCredentials cac = mapper.readValue(jsonCreds, CameraAdminCredentials.class)
                    cmd.user = cac.userName
                    cmd.password = cac.password
                    logService.cam.info("After decryption")
                }
            }
        }
    }
}

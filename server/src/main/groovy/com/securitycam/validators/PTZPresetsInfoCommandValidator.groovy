package com.securitycam.validators

import com.securitycam.commands.PTZPresetsInfoCommand
import com.securitycam.services.LogService
import com.securitycam.services.UtilsService
import org.springframework.validation.Errors

class PTZPresetsInfoCommandValidator  extends PtzCommandsValidator {
    PTZPresetsInfoCommandValidator(LogService logService) {
        super(logService)
    }

    @Override
    boolean supports(Class<?> clazz) {
        return PTZPresetsInfoCommand.class == clazz
    }

    @Override
    void validate(Object cmd, Errors errors) {
        super.validate(cmd, errors)
        if (cmd instanceof PTZPresetsInfoCommand) {
            if(cmd.onvifBaseAddress == null)
                errors.rejectValue("onvifBaseAddress", "onvifBaseAddress cannot be null")
            else if (cmd.onvifBaseAddress == "")
                errors.rejectValue("onvifBaseAddress", "onvifBaseAddress cannot be empty")
            else if(!cmd.onvifBaseAddress.matches(UtilsService.onvifBaseAddressRegex))
                errors.rejectValue("onvifBaseAddress", "Invalid onvifBaseAddress ${onvifBaseAddress} ")
         }
    }
}

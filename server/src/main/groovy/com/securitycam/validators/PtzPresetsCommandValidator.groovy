package com.securitycam.validators

import com.securitycam.commands.PTZPresetsCommand
import com.securitycam.services.LogService
import com.securitycam.services.UtilsService
import org.springframework.validation.Errors

class PtzPresetsCommandValidator extends PtzCommandsValidator{
    final def operations = Set<PTZPresetsCommand.ePresetOperations>.of(
            PTZPresetsCommand.ePresetOperations.moveTo,
            PTZPresetsCommand.ePresetOperations.saveTo,
            PTZPresetsCommand.ePresetOperations.clearFrom)

    PtzPresetsCommandValidator(LogService logService) {
        super(logService)
    }

    @Override
    boolean supports(Class<?> clazz) {
        return PTZPresetsCommand.class == clazz
    }

    @Override
    void validate(Object cmd, Errors errors) {
        super.validate(cmd, errors)
        if (cmd instanceof PTZPresetsCommand) {
            if(cmd.operation == null)
                errors.rejectValue("operation", "operation cannot be null")
            else {
                if(!operations.contains(cmd.operation))
                    errors.rejectValue("operation", "operation is not a valid value")
            }

            if(cmd.onvifBaseAddress == null)
                errors.rejectValue("onvifBaseAddress", "onvifBaseAddress cannot be null")
            else if(cmd.onvifBaseAddress == "")
                errors.rejectValue("onvifBaseAddress", "onvifBaseAddress cannot be empty")
            else if(!cmd.onvifBaseAddress.matches(UtilsService.onvifBaseAddressRegex))
                errors.rejectValue("onvifBaseAddress", "Invalid onvifBaseAddress ${cmd.onvifBaseAddress}")

            if(cmd.preset == null)
                errors.rejectValue("preset", "preset cannot be null")
            else if(cmd.preset == "")
                errors.rejectValue("preset", "preset cannot be empty")
        }
    }
}

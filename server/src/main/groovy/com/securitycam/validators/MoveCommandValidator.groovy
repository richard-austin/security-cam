package com.securitycam.validators

import com.securitycam.commands.MoveCommand
import com.securitycam.commands.PtzCommand
import com.securitycam.services.LogService
import org.springframework.validation.Errors

class MoveCommandValidator extends PtzCommandValidator {

    Set<MoveCommand.eMoveDirections> commands = Set<MoveCommand.eMoveDirections>.of(
            MoveCommand.eMoveDirections.tiltUp,
            MoveCommand.eMoveDirections.tiltDown,
            MoveCommand. eMoveDirections.panLeft,
            MoveCommand.eMoveDirections.panRight,
            MoveCommand.eMoveDirections.zoomIn,
            MoveCommand.eMoveDirections.zoomOut)

    MoveCommandValidator(LogService logService) {
        super(logService)
    }

    @Override
    boolean supports(Class<?> clazz) {
        return super.supports(PtzCommand.class) && MoveCommand.class == clazz
    }

    @Override
    void validate(Object cmd, Errors errors) {
        super.validate(cmd, errors)
        if (cmd instanceof MoveCommand) {
            if (cmd.moveDirection == null)
                errors.rejectValue("moveDirection", "moveDirectionCannot be null")
            else if (!commands.contains(cmd.moveDirection))
                errors.rejectValue("moveDirection", "moveDirection is not a valid value (${cmd.moveDirection})")
        }
    }
}

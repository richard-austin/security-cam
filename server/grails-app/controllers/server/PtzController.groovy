package server

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import grails.validation.ValidationErrors
import security.cam.LogService
import security.cam.OnvifService
import security.cam.ValidationErrorService
import security.cam.commands.MoveCommand
import security.cam.commands.StopCommand
import security.cam.enums.PassFail
import security.cam.interfaceobjects.ObjectCommandResponse

class PtzController {
    OnvifService onvifService
    ValidationErrorService validationErrorService
    LogService logService

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD'])
    def move(MoveCommand cmd)
    {
        if(cmd.hasErrors())
        {
            def errorsMap = validationErrorService.commandErrors(cmd.errors as ValidationErrors, 'move')
            logService.cam.error "move: Validation error: " + errorsMap.toString()
            render(status: 400, text: errorsMap as JSON)
        }
        else {
            ObjectCommandResponse response = onvifService.move(cmd)
            if(response.status != PassFail.PASS)
                render(status: 500, text: response.error)
            else
                render(status: 200, text: 'Move successful')
        }

    }

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD'])
    def stop(StopCommand cmd)
    {
        if(cmd.hasErrors())
        {
            def errorsMap = validationErrorService.commandErrors(cmd.errors as ValidationErrors, 'stop')
            logService.cam.error "stop: Validation error: " + errorsMap.toString()
            render(status: 400, text: errorsMap as JSON)
        }
        else {
            ObjectCommandResponse response = onvifService.stop(cmd)
            if(response.status != PassFail.PASS)
                render(status: 500, text: response.error)
            else
                render(status: 200, text: 'Stop successful')
        }

    }
}

package server

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import grails.validation.ValidationErrors
import security.cam.LogService
import security.cam.ValidationErrorService
import security.cam.commands.SetUpWifiCommand
import security.cam.interfaceobjects.ObjectCommandResponse

class WifiUtilsController {
    ValidationErrorService validationErrorService
    LogService logService

    @Secured(['ROLE_CLIENT'])
    def setUpWifi(SetUpWifiCommand cmd)
    {
        ObjectCommandResponse response = new ObjectCommandResponse()

        if(cmd.hasErrors())
        {
            def errorsMap = validationErrorService.commandErrors(cmd.errors as ValidationErrors, 'setUpWifi')
            logService.cam.error "setUpWifi: Validation error: " + errorsMap.toString()
            render(status: 400, text: errorsMap as JSON)
        }
        else {
            render (status: 200, text: '')
        }
    }
}

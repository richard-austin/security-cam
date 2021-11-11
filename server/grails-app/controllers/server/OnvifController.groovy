package server

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import grails.validation.ValidationErrors
import security.cam.LogService
import security.cam.OnvifService
import security.cam.ValidationErrorService
import security.cam.commands.GetSnapshotCommand
import security.cam.enums.PassFail
import security.cam.interfaceobjects.ObjectCommandResponse

class OnvifController {
    LogService logService
    OnvifService onvifService
    ValidationErrorService validationErrorService
    @Secured(['ROLE_CLIENT'])
    def discover() {
        ObjectCommandResponse resp = onvifService.getMediaProfiles()

        if(resp.status == PassFail.PASS)
            render resp.responseObject as JSON
        else
            render (status: 500, text: resp.error)
    }

    @Secured(['ROLE_CLIENT'])
    def getSnapshot(GetSnapshotCommand cmd)
    {
        if(cmd.hasErrors())
        {
            def errorsMap = validationErrorService.commandErrors(cmd.errors as ValidationErrors, 'getSnapshot')
            logService.cam.error "getSnapshot: Validation error: " + errorsMap.toString()
            render(status: 400, text: errorsMap as JSON)
        }
        else {
            def result = onvifService.getSnapshot(cmd.url)
            if (result.status == PassFail.PASS)
                render result.responseObject
            else {
                render(status: 500, text: result.error)
            }
        }
    }
}

package server

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import grails.validation.ValidationErrors
import security.cam.LogService
import security.cam.OnvifService
import security.cam.ValidationErrorService
import security.cam.commands.DiscoverCameraDetailsCommand
import security.cam.commands.GetSnapshotCommand
import security.cam.enums.PassFail
import security.cam.interfaceobjects.ObjectCommandResponse

class OnvifController {
    LogService logService
    OnvifService onvifService
    ValidationErrorService validationErrorService
    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD'])
    def discover() {
        ObjectCommandResponse resp = onvifService.getMediaProfiles()

        if(resp.status == PassFail.PASS)
            render resp.responseObject as JSON
        else
            render (status: 500, text: resp.error)
    }

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD'])
    def discoverCameraDetails(DiscoverCameraDetailsCommand cmd) {
        if(cmd.hasErrors()) {
            def errorsMap = validationErrorService.commandErrors(cmd.errors as ValidationErrors, 'discoverCameraDetails')
            logService.cam.error "discoverCameraDetails: Validation error: " + errorsMap.toString()
            render(status: 400, text: errorsMap as JSON)
        }
        else {
            ObjectCommandResponse resp = onvifService.getMediaProfiles(cmd.onvifUrl)

            if (resp.status == PassFail.PASS)
                render resp.responseObject as JSON
            else
                render(status: 500, text: resp.error)
        }
    }

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD', 'ROLE_GUEST'])
    def getSnapshot(GetSnapshotCommand cmd)
    {
        if(cmd.hasErrors())
        {
            def errorsMap = validationErrorService.commandErrors(cmd.errors as ValidationErrors, 'getSnapshot')
            logService.cam.error "getSnapshot: Validation error: " + errorsMap.toString()
            render(status: 400, text: errorsMap as JSON)
        }
        else {
            def result = onvifService.getSnapshot(cmd.url, cmd.cred)
            if (result.status == PassFail.PASS)
                render result.responseObject
            else if(result.errno == 401)
            {
                render(status: 401, text: result.error)
            }
            else {
                render(status: 500, text: result.error)
            }
        }
    }
}

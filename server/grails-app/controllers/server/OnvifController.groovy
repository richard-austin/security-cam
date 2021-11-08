package server

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import security.cam.LogService
import security.cam.OnvifService
import security.cam.enums.PassFail
import security.cam.interfaceobjects.ObjectCommandResponse

class OnvifController {
    LogService logService
    OnvifService onvifService

    @Secured(['ROLE_CLIENT'])
    def discover() {
        ObjectCommandResponse resp = onvifService.getMediaProfiles()

        if(resp.status == PassFail.PASS)
            render resp.responseObject as JSON
        else
            render (status: 500, text: resp.error)
    }
}

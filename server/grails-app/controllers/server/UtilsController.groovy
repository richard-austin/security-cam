package server

import grails.plugin.springsecurity.annotation.Secured
import security.cam.UtilsService
import security.cam.enums.PassFail
import security.cam.interfaceobjects.ObjectCommandResponse

class UtilsController {
    UtilsService utilsService

    @Secured(['ROLE_CLIENT'])
    def getTemperature() {
        ObjectCommandResponse response = utilsService.getTemperature()

        if(response.status != PassFail.PASS)
            render(status: 500, text: response.error)
        else
            render response.responseObject
    }
}

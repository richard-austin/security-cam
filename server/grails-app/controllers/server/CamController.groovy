package server

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import security.cam.CamService
import security.cam.interfaceobjects.ObjectCommandResponse
import security.cam.enums.PassFail

class CamController {
    CamService camService

    @Secured(['ROLE_CLIENT'])
    def getCameras() {
        ObjectCommandResponse cameras = camService.getCameras()//  grailsApplication.config.cameras //configService.getCameras()

        if(cameras.status != PassFail.PASS)
            render (status: 500, text: cameras.error)
        else
            render cameras.responseObject as JSON
    }
}

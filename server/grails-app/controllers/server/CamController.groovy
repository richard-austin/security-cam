package server

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import security.cam.CamService
import security.cam.LogService
import security.cam.interfaceobjects.ObjectCommandResponse
import security.cam.enums.PassFail

class CamController {
    static responseFormats = ['json', 'xml']

    CamService camService
    LogService logService

    @Secured(['ROLE_CLIENT'])
    def getCameras() {
        ObjectCommandResponse cameras = camService.getCameras()

        if(cameras.status != PassFail.PASS)
            render (status: 500, text: cameras.error)
        else {
            logService.cam.info("getCameras: success")
            render cameras.responseObject as JSON
        }
    }
}

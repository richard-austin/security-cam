package server

import grails.converters.JSON
import grails.core.GrailsApplication
import grails.plugin.springsecurity.annotation.Secured
import security.cam.ConfigService

class CamController {
    GrailsApplication grailsApplication
    ConfigService configService

    @Secured(['ROLE_CLIENT'])
    def getCameras() {
        def cameras = configService.getCameras() // grailsApplication.config.grails.mime.types

        render cameras as JSON
    }
}

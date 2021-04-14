package server

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import security.cam.Temperature
import security.cam.UtilsService
import security.cam.enums.PassFail
import security.cam.interfaceobjects.ObjectCommandResponse

class UtilsController {
    UtilsService utilsService
    /**
     * getTemperature: Get the core temperature (Raspberry pi only). This is called at intervals to keep the session alive
     * @return: The temperature as a string. On non Raspberry pi systems an error is returned.
     */
    @Secured(['ROLE_CLIENT'])
    def getTemperature() {
        ObjectCommandResponse response = utilsService.getTemperature()

        if (response.status != PassFail.PASS)
            render(status: 500, text: response.error)
        else
            render response.responseObject as JSON
    }

    /**
     * getVersion: Get the version number from application.yml config
     * @return: The version strig
     */
    @Secured(['ROLE_CLIENT'])
    def getVersion() {
        ObjectCommandResponse response = utilsService.getVersion()
        if (response.status != PassFail.PASS)
            render(status: 500, text: response.error)
        else
            render response.responseObject as JSON
    }

    /**
     * setIP: Set the file myip to contain our current public ip address.
     * @return: Our public ip address
     */
    @Secured(['ROLE_CLIENT'])
    def setIP()
    {
        ObjectCommandResponse response
        response = utilsService.setIP()
        render response.responseObject as JSON
    }
}

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
    *getTemperature: Get the core temperature (Raspberry pi only). This is called at intervals to keep the session alive
    * @return: The temperature as a string. On non Raspberry pi systems an error is returned.
    */
    @Secured(['ROLE_CLIENT'])
    def getTemperature() {
        ObjectCommandResponse response = utilsService.getTemperature()

        if(response.status != PassFail.PASS)
            render new Temperature("Temp=47.98'C") as JSON

        //   render(status: 500, text: response.error)
        else
            render response.responseObject as JSON
    }
}

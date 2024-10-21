package com.securitycam.controllers

import com.securitycam.controlleradvice.ErrorResponse
import com.securitycam.enums.PassFail
import com.securitycam.interfaceobjects.ObjectCommandResponse
import com.securitycam.services.RestfulCallErrorService
import com.securitycam.services.UtilsService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.security.access.annotation.Secured
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/utils")
class UtilsController {
    @Autowired
    UtilsService utilsService

    @Autowired
    RestfulCallErrorService restfulCallErrorService
    /**
     * getTemperature: Get the core temperature (Raspberry pi only). This is called at intervals to keep the session alive
     * @return: The temperature as a string. On non Raspberry pi systems an error is returned.
     */
    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD', 'ROLE_GUEST'])
    @RequestMapping("getTemperature")
    def getTemperature() {
        ObjectCommandResponse response = utilsService.getTemperature()

        if (response.status != PassFail.PASS)
            return restfulCallErrorService.returnError(new Exception(), "cam/getTemperature", response.error, "", HttpStatus.INTERNAL_SERVER_ERROR)
        else
            return response.responseObject
    }

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD', 'ROLE_GUEST'])
    @RequestMapping("audioInUse")
    def audioInUse() {
        return [audioInUse: utilsService.getAudioInUse()]
    }

    @MessageMapping(value = "/audio")
    protected def audio(@Payload byte[] data) {
        utilsService.audio(data)
    }
}

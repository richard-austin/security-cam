package com.securitycam.controllers

import com.securitycam.commands.CameraParamsCommand
import com.securitycam.commands.SetCameraParamsCommand
import com.securitycam.commands.StartAudioOutCommand
import com.securitycam.enums.PassFail
import com.securitycam.enums.RestfulResponseStatusEnum
import com.securitycam.error.NVRRestMethodException
import com.securitycam.interfaceobjects.ObjectCommandResponse
import com.securitycam.interfaceobjects.RestfulResponse
import com.securitycam.services.LogService
import com.securitycam.services.RestfulInterfaceService
import com.securitycam.services.UtilsService
import com.securitycam.validators.BadRequestResult
import com.securitycam.validators.CameraParamsCommandValidator
import com.securitycam.validators.GeneralValidator
import com.securitycam.validators.SetCameraParamsCommandValidator
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.security.access.annotation.Secured
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/utils")
class UtilsController {
    @Autowired
    UtilsService utilsService

    @Autowired
    LogService logService

    @Autowired
    RestfulInterfaceService restfulInterfaceService

    /**
     * getTemperature: Get the core temperature (Raspberry pi only). This is called at intervals to keep the session alive
     * @return: The temperature as a string. On non Raspberry pi systems an error is returned.
     */
    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD', 'ROLE_GUEST'])
    @RequestMapping("getTemperature")
    def getTemperature() {
        ObjectCommandResponse response = utilsService.getTemperature()

        if (response.status != PassFail.PASS)
            throw new NVRRestMethodException(response.error, "utils/getTemperature", "See logs")
        else
            return response.responseObject
    }

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD', 'ROLE_GUEST'])
    @RequestMapping("audioInUse")
    def audioInUse() {
        return [audioInUse: utilsService.getAudioInUse()]
    }

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD'])
    @RequestMapping("startAudioOut")
    def startAudioOut(@Valid @RequestBody StartAudioOutCommand cmd) {
        ObjectCommandResponse response = utilsService.startAudioOut(cmd)

        if (response.status != PassFail.PASS)
            throw new NVRRestMethodException(response.error, "utils/startAudioOut", "See logs")
        else
            return ResponseEntity.ok(response.responseObject)
    }

    @MessageMapping(value = "/audio")
    protected def audio(@Payload byte[] data) {
        utilsService.audio(data)
    }

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD'])
    @PostMapping("/stopAudioOut")
    def stopAudioOut() {
        ObjectCommandResponse response = utilsService.stopAudioOut()
        if (response.status != PassFail.PASS)
            return new NVRRestMethodException(response.error, "/utile/stopAudioOut", "See logs")
        else
            return response.responseObject
    }


    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD', 'ROLE_GUEST'])
    @PostMapping("/cameraParams")
    def cameraParams(@RequestBody CameraParamsCommand cmd) {
        def gv = new GeneralValidator(cmd, new CameraParamsCommandValidator())
        BindingResult result = gv.validate()

        if (result.hasErrors()) {
            def retVal = new BadRequestResult(result)
            return new ResponseEntity<BadRequestResult>(retVal, HttpStatus.BAD_REQUEST)
        } else {
            logService.cam.info("Getting parameters for camera at ${cmd.address}")
            RestfulResponse response = restfulInterfaceService.sendRequest(cmd.address, cmd.uri, cmd.params)

            if (response.status != RestfulResponseStatusEnum.PASS) {
                logService.cam.error "cameraParams: error: ${response.errorMsg}"
                throw new NVRRestMethodException(response.errorMsg, "utils/cameraParams: Failed to get camera parameters ${response.errorMsg}: for camera ${cmd.address}")
            } else
                return response.responseObject
        }
    }

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD'])
    @PostMapping("/setCameraParams")
    def setCameraParams(@RequestBody SetCameraParamsCommand cmd) {
        def gv = new GeneralValidator(cmd, new SetCameraParamsCommandValidator())
        BindingResult result = gv.validate()
        if (result.hasErrors()) {
            def retVal = new BadRequestResult(result)
            return new ResponseEntity<BadRequestResult>(retVal, HttpStatus.BAD_REQUEST)
        } else {
            RestfulResponse response = restfulInterfaceService.sendRequest(cmd.address, cmd.uri, cmd.params, true)

            if (response.status != RestfulResponseStatusEnum.PASS) {
                logService.cam.error "setCameraParams: error: ${response.errorMsg}"
                throw new NVRRestMethodException(response.errorMsg, "utils/setCameraParams")
            } else
                return response.responseObject
        }
    }
}

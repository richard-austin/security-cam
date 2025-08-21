package com.securitycam.controllers

import com.securitycam.commands.CameraParamsCommand
import com.securitycam.commands.CheckNotGuestCommand
import com.securitycam.commands.SetCameraParamsCommand
import com.securitycam.commands.SetupSMTPAccountCommand
import com.securitycam.commands.StartAudioOutCommand
import com.securitycam.commands.UpdateAdHocDeviceListCommand
import com.securitycam.commands.UpdateCamerasCommand
import com.securitycam.enums.PassFail
import com.securitycam.enums.RestfulResponseStatusEnum
import com.securitycam.error.NVRRestMethodException
import com.securitycam.interfaceobjects.ObjectCommandResponse
import com.securitycam.interfaceobjects.RestfulResponse
import com.securitycam.services.LogService
import com.securitycam.services.RestfulInterfaceService
import com.securitycam.services.UserAdminService
import com.securitycam.services.UtilsService
import com.securitycam.validators.BadRequestResult
import com.securitycam.validators.CameraParamsCommandValidator
import com.securitycam.validators.CheckNotGuestCommandValidator
import com.securitycam.validators.GeneralValidator
import com.securitycam.validators.SetCameraParamsCommandValidator
import com.securitycam.validators.SetupSMTPAccountCommandValidator
import com.securitycam.validators.UpdateAdHocDeviceListCommandValidator
import com.securitycam.validators.UpdateCamerasCommandValidator
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
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
    UserAdminService userAdminService

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
            throw new NVRRestMethodException(response.error, "See logs")
        else
            return response.responseObject
    }

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD', 'ROLE_GUEST'])
    @RequestMapping("audioInUse")
    def audioInUse() {
        return [audioInUse: utilsService.getAudioInUse()]
    }

    @PostMapping(value="/setupSMTPClientLocally")
    def setupSMTPClientLocally(@RequestBody SetupSMTPAccountCommand cmd) {
        def gv = new GeneralValidator(cmd, new SetupSMTPAccountCommandValidator(userAdminService))
        def result = gv.validate()

        if (result.hasErrors()) {
            BadRequestResult retVal = new BadRequestResult(result)
            logService.cam.error "setupSMTPClientLocally: Validation error: " + retVal.toString()
            return ResponseEntity
                    .badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(retVal)
        } else {
            ObjectCommandResponse response = utilsService.setupSMTPClient(cmd)
            if (response.status != PassFail.PASS)
                return new ResponseEntity<Object>(response.error, HttpStatus.INTERNAL_SERVER_ERROR)
            else
                return ResponseEntity.ok("")
        }
    }

    @PostMapping("/getSMTPClientParamsLocally")
    def getSMTPClientParamsLocally(CheckNotGuestCommand cmd) {
        def gv = new GeneralValidator(cmd, new CheckNotGuestCommandValidator(userAdminService))
        def result = gv.validate()

        if (result.hasErrors()) {
            BadRequestResult retVal = new BadRequestResult(result)
            return ResponseEntity
                    .badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(retVal)
        } else {
            ObjectCommandResponse response = utilsService.getSMTPClientParams()
            if (response.status != PassFail.PASS)
                return new ResponseEntity<Object>(response.error, HttpStatus.INTERNAL_SERVER_ERROR)
            else if (response.response != null)
                return ResponseEntity
                        .badRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(response.response)
            else
                return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(response.responseObject)
        }
    }

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD'])
    @RequestMapping("startAudioOut")
    def startAudioOut(@Valid @RequestBody StartAudioOutCommand cmd) {
        ObjectCommandResponse response = utilsService.startAudioOut(cmd)

        if (response.status != PassFail.PASS)
            throw new NVRRestMethodException(response.error, "See logs")
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
            return new NVRRestMethodException(response.error, "See logs")
        else
            return response.responseObject
    }


    /**
     * setIP: Set the file myip to contain our current public ip address.
     * @return: Our public ip address
     */
    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD'])
    @PostMapping("/setIP")
    def setIP()
    {
        ObjectCommandResponse response
        response = utilsService.setIP()
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
                throw new NVRRestMethodException(response.errorMsg, "Failed to get camera parameters for camera ${cmd.address}")
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
            // The ZXTech camera name does not get changed when the commands are all sent in one single batch
            if(response.status == RestfulResponseStatusEnum.PASS && cmd.params2 != "")
                response = restfulInterfaceService.sendRequest(cmd.address, cmd.uri, cmd.params2, true)

            if(response.status == RestfulResponseStatusEnum.PASS && cmd.params3 != "")
                response = restfulInterfaceService.sendRequest(cmd.address, cmd.uri, cmd.params3, true)

            if (response.status != RestfulResponseStatusEnum.PASS) {
                logService.cam.error "setCameraParams: error: ${response.errorMsg}"
                throw new NVRRestMethodException(response.errorMsg, "utils/setCameraParams")
            } else
                return response.responseObject
        }
    }

    @Secured(['ROLE_CLIENT', 'ROLE_GUEST'])
    @PostMapping("/getUserAuthorities")
    def getUserAuthorities() {
        ObjectCommandResponse result
        result = utilsService.getUserAuthorities()
        if (result.status != PassFail.PASS) {
            logService.cam.error "getUserAuthorities: error: ${result.error}"
            throw new NVRRestMethodException(result.error, "utils/getUserAuthorities")
        } else {
            logService.cam.info("getUserAuthorities: success")
            return result.responseObject
        }
    }

    /**
     * getVersion: Get the version number from application.yml config
     * @return: The version strig
     */
    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD', 'ROLE_GUEST'])
    @PostMapping("/getVersion")
    def getVersion() {
        ObjectCommandResponse response = utilsService.getVersion()
        if (response.status != PassFail.PASS)
            throw new NVRRestMethodException(response.error, "utils/getVersion")
        else
            response.responseObject
    }

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD', 'ROLE_GUEST'])
    @PostMapping("/getOpenSourceInfo")
    def getOpenSourceInfo() {
        ObjectCommandResponse response = utilsService.getOpenSourceInfo()
        if (response.status != PassFail.PASS)
            throw new NVRRestMethodException(response.error, "utils/getOpenSourceInfo")
        else
            return response.response
    }

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD'])
    @RequestMapping('/loadAdHocDevices')
    def loadAdHocDevices() {
        ObjectCommandResponse devices = utilsService.loadAdHocDevices()
        if (devices.status != PassFail.PASS)
            throw new NVRRestMethodException(devices.error, "utils/loadAdHocDevices") //render(status: 500, text: cameras.error)
        else {
            logService.cam.info("loadAdHocDevices: success")
            return devices.responseObject
        }
    }

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD'])
    @PostMapping("/updateAdHocDeviceList")
    def updateAdHocDeviceList(@RequestBody UpdateAdHocDeviceListCommand cmd) {
        ObjectCommandResponse result
        def gv = new GeneralValidator(cmd, new UpdateAdHocDeviceListCommandValidator())
        BindingResult results = gv.validate()
        if (results.hasErrors()) {
            logService.cam.error "/utils/updateAdHocDeviceList: Validation error: " + results.toString()
            BadRequestResult retVal = new BadRequestResult(results)
            return ResponseEntity
                    .badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(retVal)
        } else {
            result = utilsService.updateAdHocDeviceList(cmd)
            if (result.status != PassFail.PASS)
                return new ResponseEntity<Object>(result.error, HttpStatus.INTERNAL_SERVER_ERROR)
            return result.responseObject
        }
    }
}

package com.securitycam.controllers

import com.securitycam.commands.CloseClientsCommand
import com.securitycam.commands.GetAccessTokenCommand
import com.securitycam.commands.ResetTimerCommand
import com.securitycam.controlleradvice.ErrorResponse
import com.securitycam.enums.PassFail
import com.securitycam.error.NVRRestMethodException
import com.securitycam.interfaceobjects.ObjectCommandResponse
import com.securitycam.services.CamService
import com.securitycam.services.CameraAdminPageHostingService
import com.securitycam.services.LogService
import com.securitycam.validators.BadRequestResult
import com.securitycam.validators.GeneralValidator
import com.securitycam.validators.GetAccessTokenCommandValidator
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.annotation.Secured
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/cam")
class CamController {
    @Autowired
    LogService logService

    @Autowired
    CamService camService

    @Autowired
    CameraAdminPageHostingService cameraAdminPageHostingService

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD', 'ROLE_GUEST'])
    @RequestMapping('/getCameras')
    def getCameras() {
        ObjectCommandResponse cameras = camService.getCameras()
        if (cameras.status != PassFail.PASS)
            throw new NVRRestMethodException(cameras.error, "cam/getCameras") //render(status: 500, text: cameras.error)
        else {
            logService.cam.info("getCameras: success")
            return cameras.responseObject
        }
    }

   @Secured(['ROLE_CLIENT', 'ROLE_CLOUD', 'ROLE_GUEST'])
    @RequestMapping('/getPublicKey')
    def getPublicKey() {
        ObjectCommandResponse response = camService.getPublicKey()
        if(response.status == PassFail.PASS)
            return response.responseObject
        else
            throw new NVRRestMethodException(response.error, "cam/getPublicKey")
    }

    /**
     * getAccessToken: Get an access token for a camera web admin page via the camera admin page hosting server.
     * @param cmd : Command object containing the camera host address and port.
     * @return The access token to use as the accessToken parameter in the initial get request to the hosting server,
     *          or error code.
     */
    @Secured(['ROLE_CLIENT'])
    @PostMapping("/getAccessToken")
    def getAccessToken(@RequestBody GetAccessTokenCommand cmd) {
        def gv = new GeneralValidator(cmd, new GetAccessTokenCommandValidator())
        def result = gv.validate()
        if (result.hasErrors()) {
            logService.cam.error "/cam/getAccessToken: Validation error: " + result.toString()
            BadRequestResult retVal = new BadRequestResult(result)
            return ResponseEntity
                    .badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(retVal)
        } else {
            ObjectCommandResponse response = cameraAdminPageHostingService.getAccessToken(cmd)
            if (response.status != PassFail.PASS)
                throw new NVRRestMethodException(response.error, "cam/getAccessToken")
            else
                return response.responseObject
        }
    }

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD'])
    @PostMapping("/resetTimer")
    def resetTimer(@Valid @RequestBody ResetTimerCommand cmd) {
            ObjectCommandResponse response = cameraAdminPageHostingService.resetTimer(cmd)
            if (response.status != PassFail.PASS)
                throw new NVRRestMethodException(response.error, "cam/resetTimer")
            else
                return response.responseObject
    }

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD'])
    @PostMapping("/closeClients")
    def closeClients(@Valid @RequestBody CloseClientsCommand cmd) {
            ObjectCommandResponse response = cameraAdminPageHostingService.closeClients(cmd)
            if (response.status != PassFail.PASS)
                throw new NVRRestMethodException(response.error, "cam/closeClients")
            else
                return response.responseObject
        }
}

package server

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import grails.validation.ValidationErrors
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import security.cam.LogService
import security.cam.RestfulInterfaceService
import security.cam.Sc_processesService
import security.cam.UtilsService
import security.cam.ValidationErrorService
import security.cam.commands.CameraParamsCommand
import security.cam.commands.CheckNotGuestCommand
import security.cam.commands.SetCameraParamsCommand
import security.cam.commands.SetupSMTPAccountCommand
import security.cam.commands.StartAudioOutCommand
import security.cam.enums.PassFail
import security.cam.enums.RestfulResponseStatusEnum
import security.cam.interfaceobjects.ObjectCommandResponse
import security.cam.interfaceobjects.RestfulResponse

class UtilsController {
    UtilsService utilsService
    RestfulInterfaceService restfulInterfaceService
    LogService logService
    ValidationErrorService validationErrorService

    /**
     * getTemperature: Get the core temperature (Raspberry pi only). This is called at intervals to keep the session alive
     * @return: The temperature as a string. On non Raspberry pi systems an error is returned.
     */
    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD', 'ROLE_GUEST'])
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
    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD', 'ROLE_GUEST'])
    def getVersion() {
        ObjectCommandResponse response = utilsService.getVersion()
        if (response.status != PassFail.PASS)
            render(status: 500, text: response.error)
        else
            render response.responseObject as JSON
    }

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD', 'ROLE_GUEST'])
    def getOpenSourceInfo() {
        ObjectCommandResponse response = utilsService.getOpenSourceInfo()
        if (response.status != PassFail.PASS)
            render(status: 500, text: response.error)
        else
            render response.response as String
    }

    /**
     * setIP: Set the file myip to contain our current public ip address.
     * @return: Our public ip address
     */
    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD'])
    def setIP()
    {
        ObjectCommandResponse response
        response = utilsService.setIP()
        render response.responseObject as JSON
    }

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD', 'ROLE_GUEST'])
    def cameraParams(CameraParamsCommand cmd)
    {
        logService.cam.info("Getting parameters for camera at ${cmd.address}")

        if(cmd.hasErrors())
        {
            def errorsMap = validationErrorService.commandErrors(cmd.errors as ValidationErrors, 'cameraParams')
            logService.cam.error "cameraParams: Validation error: " + errorsMap.toString()
            render(status: 400, text: errorsMap as JSON)
        }
        else
        {
            RestfulResponse response = restfulInterfaceService.sendRequest(cmd.address, cmd.uri, cmd.params)

            if(response.status != RestfulResponseStatusEnum.PASS)
            {
                logService.cam.error "cameraParams: error: ${response.errorMsg}"
                render(status: response.getResponseCode(), text: "Failed to get camera parameters ${response.errorMsg}: for camera ${cmd.address}")
            }
            else
                render response.responseObject as JSON
        }
    }

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD'])
    def setCameraParams(SetCameraParamsCommand cmd)
    {
        if(cmd.hasErrors())
        {
            def errorsMap = validationErrorService.commandErrors(cmd.errors as ValidationErrors, 'cameraParams')
            logService.cam.error "setCameraParams: Validation error: " + errorsMap.toString()
            render(status: 400, text: errorsMap as JSON)
        }
        else
        {
            RestfulResponse response = restfulInterfaceService.sendRequest(cmd.address, cmd.uri, cmd.params, true)

            if(response.status != RestfulResponseStatusEnum.PASS)
            {
                logService.cam.error "setCameraParams: error: ${response.errorMsg}"
                render(status: response.getResponseCode(), text: "Failed to set camera parameters ${response.errorMsg}: for camera ${cmd.address}")
            }
            else
                render response.responseObject as JSON
        }
    }

    def setupSMTPClientLocally(SetupSMTPAccountCommand cmd) {
        if(cmd.hasErrors())
        {
            def errorsMap = validationErrorService.commandErrors(cmd.errors as ValidationErrors, 'setupSMTPClientLocally')
            logService.cam.error "setupSMTPClientLocally: Validation error: " + errorsMap.toString()
            render(status: 400, text: errorsMap as JSON)
        }
        else {
            ObjectCommandResponse response = utilsService.setupSMTPClient(cmd)
            if(response.status != PassFail.PASS)
                render (status: 500, text: response.error)
            else
                render ""
        }
    }

    def getSMTPClientParamsLocally(CheckNotGuestCommand cmd) {
        if(cmd.hasErrors()) {
            def errorsMap = validationErrorService.commandErrors(cmd.errors as ValidationErrors, 'setupSMTPClientLocally')
            logService.cam.error "getSMTPClientParamsLocally: Validation error: " + errorsMap.toString()
            render(status: 400, text: errorsMap as JSON)
        }
        else {
            ObjectCommandResponse response = utilsService.getSMTPClientParams()
            if (response.status != PassFail.PASS)
                render(status: 500, text: response.error)
            else if (response.response != null)
                render(status: 400, text: response.response)  // Warning, no config file present
            else
                render(status: 200, text: response.responseObject as JSON)
        }
    }

    Sc_processesService sc_processesService
    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD'])
    def startProcs()
    {
        ObjectCommandResponse response = sc_processesService.startProcesses()
        if(response.status != PassFail.PASS)
            render (status: 500, text: response.error)
        else
            render "success"
    }

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD'])
    def stopProcs()
    {
        ObjectCommandResponse response = sc_processesService.stopProcesses()
        if(response.status != PassFail.PASS)
            render (status: 500, text: response.error)
        else
            render "success"

    }

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD'])
    def startAudioOut(StartAudioOutCommand cmd) {
        if(cmd.hasErrors()) {
            def errorsMap = validationErrorService.commandErrors(cmd.errors as ValidationErrors, 'setupSMTPClientLocally')
            logService.cam.error "startAudioOut: Validation error: " + errorsMap.toString()
            render(status: 400, text: errorsMap as JSON)
        }
        else {
            ObjectCommandResponse response = utilsService.startAudioOut(cmd)
            if (response.status != PassFail.PASS)
                render(status: 500, text: response.error)
            else
                render(status: 200, text: response.responseObject as JSON)
        }
    }

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD'])
    def stopAudioOut() {
        ObjectCommandResponse response = utilsService.stopAudioOut()
        if (response.status != PassFail.PASS)
            render(status: 500, text: response.error)
        else
            render(status: 200, text: response.responseObject as JSON)
    }

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD'])
    def audioInUse() {
         render(status: 200, text: [audioInUse: utilsService.getAudioInUse()] as JSON)
    }

    @MessageMapping(value = "/audio")
    protected def audio(@Payload byte[] data) {
        utilsService.audio(data)
    }
}

package server

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import grails.validation.ValidationErrors
import security.cam.ConfigurationUpdateService
import security.cam.LogService
import security.cam.RestfulInterfaceService
import security.cam.Sc_processesService
import security.cam.UtilsService
import security.cam.ValidationErrorService
import security.cam.commands.CameraParamsCommand
import security.cam.commands.SetCameraParamsCommand
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

    @Secured(['ROLE_CLIENT'])
    def cameraParams(CameraParamsCommand cmd)
    {
        ObjectCommandResponse result =  new ObjectCommandResponse()
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
                result.status = PassFail.FAIL
                result.error = response.errorMsg
                result.userError = response.userError
                render(status: 500, text: result)
            }
            else
                render response.responseObject as JSON
        }
    }

    @Secured(['ROLE_CLIENT'])
    def setCameraParams(SetCameraParamsCommand cmd)
    {
        ObjectCommandResponse result =  new ObjectCommandResponse()
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
                logService.cam.error "setCameraParams: error: " + response.errorMsg
                result.status = PassFail.FAIL
                result.error = response.errorMsg
                result.userError = response.userError
                render(status: 500, result as JSON)
            }
            else
                render response.responseObject as JSON
        }
    }

    Sc_processesService sc_processesService
    @Secured(['ROLE_CLIENT'])
    def startProcs()
    {
        ObjectCommandResponse response = sc_processesService.startProcesses()
        if(response.status != PassFail.PASS)
            render (status: 500, text: response.error)
        else
            render "success"
    }

    @Secured(['ROLE_CLIENT'])
    def stopProcs()
    {
        ObjectCommandResponse response = sc_processesService.stopProcesses()
        if(response.status != PassFail.PASS)
            render (status: 500, text: response.error)
        else
            render "success"

    }

    ConfigurationUpdateService configurationUpdateService

    @Secured(['ROLE_CLIENT'])
    def generateConfigs()
    {
        ObjectCommandResponse response = configurationUpdateService.parseConfig()
        if(response.status != PassFail.PASS)
            render (status: 500, text: response.error)
        else
            render "success"
    }
}

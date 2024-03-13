package server

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import grails.validation.ValidationErrors
import security.cam.CamService
import security.cam.CameraAdminPageHostingService
import security.cam.LogService
import security.cam.ValidationErrorService
import security.cam.commands.CloseClientsCommand
import security.cam.commands.GetAccessTokenCommand
import security.cam.commands.ResetTimerCommand
import security.cam.commands.SetOnvifCredentialsCommand
import security.cam.commands.UpdateCamerasCommand
import security.cam.commands.UploadMaskFileCommand
import security.cam.interfaceobjects.ObjectCommandResponse
import security.cam.enums.PassFail

class CamController {
    static responseFormats = ['json', 'xml']

    CamService camService
    LogService logService
    ValidationErrorService validationErrorService
    CameraAdminPageHostingService cameraAdminPageHostingService
    /**
     * getCameras: Get all cameras defined in the application.yml file
     * @return
     */
    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD', 'ROLE_GUEST'])
    def getCameras() {
        ObjectCommandResponse cameras = camService.getCameras()

        if (cameras.status != PassFail.PASS)
            render(status: 500, text: cameras.error)
        else {
            logService.cam.info("getCameras: success")
            render cameras.responseObject as JSON
        }
    }

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD'])
    def updateCameras(UpdateCamerasCommand cmd) {
        ObjectCommandResponse result

        if (cmd.hasErrors()) {
            def errorsMap = validationErrorService.commandErrors(cmd.errors as ValidationErrors, 'updateCameras')
            logService.cam.error "updateCameras: Validation error: " + errorsMap.toString()
            render(status: 400, text: errorsMap as JSON)
        } else {
            result = camService.updateCameras(cmd)
            if (result.status != PassFail.PASS)
                render(status: 500, text: result.error)
            render result.responseObject as JSON
        }
    }

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD'])
    def uploadMaskFile(UploadMaskFileCommand cmd) {
        logService.cam.debug "CamController.uploadMaskFile() called"
        ObjectCommandResponse result

        // Check for Command Object validation errors
        if (cmd.hasErrors()) {
            //cmd.errors.each { println it }
            def errorsMap = validationErrorService.commandErrors(cmd.errors as ValidationErrors, "uploadMaskFile")
            render(status: 400, text: (errorsMap as JSON))
        } else {
            result = camService.uploadMaskFile(cmd)
            if (result.status == PassFail.PASS)
                render(status: 200, text: [])
            else
                render(status: 500, text: result.error)
        }
    }

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD', 'ROLE_GUEST'])
    def getPublicKey() {
        ObjectCommandResponse response = camService.getPublicKey()
        if(response.status == PassFail.PASS)
            render response.responseObject
        else
            render(status: 500, text: response.error)
    }

    /**
     * setOnvifCredentials: Set the access credentials used for Onvif operations on the cameras
     * @param cmd : Command object containing the username and password
     * @return: Success/error state.
     */
    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD'])
    def setOnvifCredentials(SetOnvifCredentialsCommand cmd) {
        if (cmd.hasErrors()) {
            def errorsMap = validationErrorService.commandErrors(cmd.errors as ValidationErrors, "setOnvifCredentials")
            render(status: 400, text: (errorsMap as JSON))
        } else {
            ObjectCommandResponse response = camService.setOnvifCredentials(cmd)

            if (response.status != PassFail.PASS)
                render(status: 500, text: response.error)
            else
                render(status: 200, text: '')
        }
    }

    /**
     * getAccessToken: Get an access token for a camera web admin page via the camera admin page hosting server.
     * @param cmd : Command object containing the camera host address and port.
     * @return The access token to use as the accessToken parameter in the initial get request to the hosting server,
     *          or error code.
     */
    @Secured(['ROLE_CLIENT'])
    def getAccessToken(GetAccessTokenCommand cmd) {
        if (cmd.hasErrors()) {
            def errorsMap = validationErrorService.commandErrors(cmd.errors as ValidationErrors, "getAccessToken")
            render(status: 400, text: (errorsMap as JSON))
        } else {
            ObjectCommandResponse response = cameraAdminPageHostingService.getAccessToken(cmd)
            if (response.status != PassFail.PASS)
                render(status: 500, text: response.error)
            else
                render(status: 200, text: response.responseObject)
        }
    }

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD'])
    def resetTimer(ResetTimerCommand cmd) {
        if (cmd.hasErrors()) {
            def errorsMap = validationErrorService.commandErrors(cmd.errors as ValidationErrors, "resetTimer")
            render(status: 400, text: (errorsMap as JSON))
        } else {
            ObjectCommandResponse response = cameraAdminPageHostingService.resetTimer(cmd)
            if (response.status != PassFail.PASS)
                render(status: 500, text: response.error)
            else
                render(status: 200, text: (response.responseObject as JSON))
        }
    }

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD'])
    def closeClients(CloseClientsCommand cmd) {
        if (cmd.hasErrors()) {
            def errorsMap = validationErrorService.commandErrors(cmd.errors as ValidationErrors, "closeClients")
            render(status: 400, text: (errorsMap as JSON))
        } else {
            ObjectCommandResponse response = cameraAdminPageHostingService.closeClients(cmd)
            if (response.status != PassFail.PASS)
                render(status: 500, text: response.error)
            else
                render(status: 200, text: (response.responseObject as JSON))
        }
    }

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD', 'ROLE_GUEST'])
    def haveOnvifCredentials() {
        ObjectCommandResponse response = camService.haveOnvifCredentials()
        if (response.status == PassFail.PASS)
            render(status: 200, text: (response.responseObject ? 'true' : 'false'))
        else
            render(status: 500, text: response.error)
    }
}

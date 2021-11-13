package server

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import grails.util.Environment
import grails.validation.ValidationErrors
import security.cam.CamService
import security.cam.LogService
import security.cam.ValidationErrorService
import security.cam.commands.SetAccessCredentialsCommand
import security.cam.commands.UpdateCamerasCommand
import security.cam.commands.UploadMaskFileCommand
import security.cam.interfaceobjects.ObjectCommandResponse
import security.cam.enums.PassFail

class CamController {
    static responseFormats = ['json', 'xml']

    CamService camService
    LogService logService
    ValidationErrorService validationErrorService

    /**
     * getCameras: Get all cameras defined in the application.yml file
     * @return
     */
    @Secured(['ROLE_CLIENT'])
    def getCameras() {
        ObjectCommandResponse cameras = camService.getCameras()

        if(cameras.status != PassFail.PASS)
            render (status: 500, text: cameras.error)
        else {
            logService.cam.info("getCameras: success")
            render cameras.responseObject as JSON
        }
    }

    @Secured(['ROLE_CLIENT'])
    def updateCameras(UpdateCamerasCommand cmd)
    {
        ObjectCommandResponse result

        if(cmd.hasErrors())
        {
            def errorsMap = validationErrorService.commandErrors(cmd.errors as ValidationErrors, 'updateCameras')
            logService.cam.error "updateCameras: Validation error: " + errorsMap.toString()
            render(status: 400, text: errorsMap as JSON)
        }
        else {
            result = camService.updateCameras(cmd)
            render result.responseObject as JSON
        }
    }

    @Secured(['ROLE_CLIENT'])
    def uploadMaskFile(UploadMaskFileCommand cmd) {
        logService.cam.debug "CamController.uploadMaskFile() called"
        ObjectCommandResponse result

        // Check for Command Object validation errors
        if (cmd.hasErrors()) {
            //cmd.errors.each { println it }
            def errorsMap = validationErrorService.commandErrors(cmd.errors as ValidationErrors, "uploadMaskFile")
            render(status: 400, text: (errorsMap as JSON))
        }
        else {
            result = camService.uploadMaskFile(cmd)
            if(result.status == PassFail.PASS)
                render (status: 200, text: [])
            else
                render (status: 500, text: result.error)
        }
    }

    /**
     * setAccessCredentials: Set the access credentials used for administrative operations (and snapshot access)
     *                       on the cameras. Note that ths does not change credentials on any camera, just those
     *                       used on this software to access them. Ideally all cameras should use the same user
     *                       name and password.
     * @param cmd: Command object containing the username and password
     * @return: Success/error state.
     */
    def setAccessCredentials(SetAccessCredentialsCommand cmd)
    {
        if(cmd.hasErrors())
        {
            def errorsMap = validationErrorService.commandErrors(cmd.errors as ValidationErrors, "setAccessCredentials")
            render(status: 400, text: (errorsMap as JSON))
        }
        else
        {
            ObjectCommandResponse response = camService.setCameraAccessCredentials(cmd)

            if(response.status != PassFail.PASS)
                render (status: 500, text: response.error)
            else
                render (status: 200, text:'')
        }
    }
}

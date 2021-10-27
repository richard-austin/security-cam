package server

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import grails.validation.ValidationErrors
import security.cam.CamService
import security.cam.LogService
import security.cam.ValidationErrorService
import security.cam.commands.UpdateCamerasCommand
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
}

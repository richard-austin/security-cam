package server

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import grails.validation.ValidationErrors
import security.cam.LogService
import security.cam.ValidationErrorService
import security.cam.WifiUtilsService
import security.cam.commands.SetUpWifiCommand
import security.cam.enums.PassFail
import security.cam.enums.RestfulResponseStatusEnum
import security.cam.interfaceobjects.ObjectCommandResponse
import security.cam.interfaceobjects.RestfulResponse

class WifiUtilsController {
    ValidationErrorService validationErrorService
    LogService logService
    WifiUtilsService wifiUtilsService

    @Secured(['ROLE_CLIENT'])
    def scanWifi()
    {
        response.contentType = "application/json"
        ObjectCommandResponse result = wifiUtilsService.scanWifi()
        if(result.status == PassFail.PASS) {
            RestfulResponse rr = result.responseObject as RestfulResponse
            if(result.responseObject instanceof RestfulResponse) {
                if ((result.responseObject as RestfulResponse).status == RestfulResponseStatusEnum.PASS)
                    render(status: 200, text: rr.responseObject as JSON)
                else
                    render(status: rr.responseCode, text: rr.errorMsg)
            }
            else
            {
                logService.cam.error "scanWifi: error: ${result.errorMsg}"
                result.status = PassFail.FAIL
                result.error = result.errorMsg
                result.userError = result.userError
                render(status: 500, result as JSON)
            }
        }
        else
            render (status: 500, text: result.error)

    }

    @Secured(['ROLE_CLIENT'])
    def setUpWifi(SetUpWifiCommand cmd)
    {
        ObjectCommandResponse result = new ObjectCommandResponse()

        if(cmd.hasErrors())
        {
            def errorsMap = validationErrorService.commandErrors(cmd.errors as ValidationErrors, 'setUpWifi')
            logService.cam.error "setUpWifi: Validation error: " + errorsMap.toString()
            render(status: 400, text: errorsMap as JSON)
        }
        else {
            result = wifiUtilsService.setUpWifi(cmd)

            if(result.status == PassFail.PASS) {
                RestfulResponse rr = result.responseObject as RestfulResponse
                if(result.responseObject instanceof RestfulResponse) {
                    if ((result.responseObject as RestfulResponse).status == RestfulResponseStatusEnum.PASS)
                        render(status: 200, text: rr.responseObject)
                    else
                        render(status: rr.responseCode, text: rr.errorMsg)
                }
                else
                {
                    logService.cam.error "setUpWifi: error: ${result.errorMsg}"
                    result.status = PassFail.FAIL
                    result.error = result.errorMsg
                    result.userError = result.userError
                    render(status: 500, result as JSON)
                }
            }
            else
                render (status: 500, text: result.error)
        }
    }
}

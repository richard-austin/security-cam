package server

import com.google.common.collect.ImmutableMap
import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import grails.validation.ValidationErrors
import security.cam.LogService
import security.cam.ValidationErrorService
import security.cam.WifiUtilsService
import security.cam.commands.SetUpWifiCommand
import security.cam.commands.SetWifiStatusCommand
import security.cam.enums.PassFail
import security.cam.interfaceobjects.EthernetStatusEnum
import security.cam.interfaceobjects.ObjectCommandResponse

class WifiUtilsController {
    ValidationErrorService validationErrorService
    LogService logService
    WifiUtilsService wifiUtilsService

    @Secured(['ROLE_CLIENT'])
    def scanWifi() {
        response.contentType = "application/json"
        ObjectCommandResponse result = wifiUtilsService.scanWifi()
        if (result.status == PassFail.PASS) {
            render(status: 200, text: result.responseObject as JSON)
        } else
            render(status: 500, text: result.error)
    }

    @Secured(['ROLE_CLIENT'])
    def setUpWifi(SetUpWifiCommand cmd) {
        ObjectCommandResponse result

        if (cmd.hasErrors()) {
            def errorsMap = validationErrorService.commandErrors(cmd.errors as ValidationErrors, 'setUpWifi')
            logService.cam.error "setUpWifi: Validation error: " + errorsMap.toString()
            render(status: 400, text: errorsMap as JSON)
        } else {
            result = wifiUtilsService.setUpWifi(cmd)

            if (result.status == PassFail.PASS) {
                render(status: 200, text: result.responseObject)
            } else {
                logService.cam.error "setUpWifi: error: ${result.error}"
                result.status = PassFail.FAIL
                render(status: result.errno, text: result.responseObject as JSON)
            }
        }
    }

    @Secured(['ROLE_CLIENT'])
    def checkWifiStatus() {
        ObjectCommandResponse result = wifiUtilsService.checkWifiStatus()
        if (result.status == PassFail.PASS) {
            render(status: 200, text: result.responseObject)
        } else {
            logService.cam.error "checkWifiStatus: error: ${result.error}"
            result.status = PassFail.FAIL
            render(status: 500, text: result.error)
        }
    }

    @Secured(['ROLE_CLIENT'])
    def setWifiStatus(SetWifiStatusCommand cmd) {
        ObjectCommandResponse result = wifiUtilsService.setWifiStatus(cmd)
        if (result.status == PassFail.PASS) {
            render(status: 200, text: result.responseObject)
        } else {
            logService.cam.error "setWifiStatus: error: ${result.error}"
            result.status = PassFail.FAIL
            render(status: 500, text: result.error)
        }
    }

    @Secured(['ROLE_CLIENT'])
    def getCurrentWifiConnection()
    {
        ObjectCommandResponse result = wifiUtilsService.getCurrentWifiConnection()

        if(result.status == PassFail.PASS)
            render (status: 200, result.responseObject as JSON)
        else
        {
            logService.cam.error "getCurrentWifiConnection: error: ${result.error}"
            result.status = PassFail.FAIL
            render(status: 500, text: result.error)
        }
    }

    @Secured(['ROLE_CLIENT'])
    def getActiveIPAddresses() {
        ObjectCommandResponse result = wifiUtilsService.getActiveIPAddresses()

        if (result.status == PassFail.PASS)
            render(status: 200, text: result as JSON)
        else {
            logService.cam.error "checkWifiStatus: error: ${result.errorMsg}"
            render(status: 500, text: result.error)
        }
    }

    @Secured(['ROLE_CLIENT'])
    def checkConnectedThroughEthernet() {
        ObjectCommandResponse result = wifiUtilsService.checkConnectedThroughEthernet()
        if (result.status == PassFail.PASS) {
            render(status: 200, text: result.responseObject as JSON)
        } else {
            def errMsg = "An error occurred in checkConnectedThroughEthernet:- (${result.error})"
            logService.cam.error(errMsg)
            render(status: 500, text: errMsg)
        }
    }
}

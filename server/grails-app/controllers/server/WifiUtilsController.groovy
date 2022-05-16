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
import security.cam.enums.RestfulResponseStatusEnum
import security.cam.interfaceobjects.EthernetStatusEnum
import security.cam.interfaceobjects.ObjectCommandResponse
import security.cam.interfaceobjects.RestfulResponse

class WifiUtilsController {
    ValidationErrorService validationErrorService
    LogService logService
    WifiUtilsService wifiUtilsService

    private static final ImmutableMap<EthernetStatusEnum, String> ethernetConnectionStatus =
            ImmutableMap.of(
                    EthernetStatusEnum.connectedViaEthernet, "CONNECTED_VIA_ETHERNET",
                    EthernetStatusEnum.notConnectedViaEthernet, "NOT_CONNECTED_VIA_ETHERNET",
                    EthernetStatusEnum.noEthernet, "NO_ETHERNET")

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
                if (result.responseObject instanceof RestfulResponse) {
                    RestfulResponse rr = result.responseObject as RestfulResponse
                    if (rr.status == RestfulResponseStatusEnum.PASS)
                        render(status: 200, text: rr.responseObject)
                    else
                        render(status: rr.responseCode, text: rr.errorMsg)
                } else {
                    logService.cam.error "setUpWifi: error: ${result.error}"
                    result.status = PassFail.FAIL
                    render(status: 500, text: result.error)
                }
            } else
                render(status: 500, text: result.error)
        }
    }

    @Secured(['ROLE_CLIENT'])
    def checkWifiStatus() {
        ObjectCommandResponse result = wifiUtilsService.checkWifiStatus()
        if (result.responseObject instanceof RestfulResponse) {
            RestfulResponse rr = result.responseObject as RestfulResponse
            if (rr.status == RestfulResponseStatusEnum.PASS)
                render(status: 200, text: rr.responseObject)
            else
                render(status: rr.responseCode, text: rr.errorMsg)
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
            if (result.responseObject instanceof RestfulResponse) {
                RestfulResponse rr = result.responseObject as RestfulResponse
                if (rr.status == RestfulResponseStatusEnum.PASS)
                    render(status: 200, text: rr.responseObject)
                else
                    render(status: rr.responseCode, text: rr.errorMsg)
            } else {
                logService.cam.error "setWifiStatus: error: ${result.error}"
                result.status = PassFail.FAIL
                render(status: 500, text: result.error)
            }
        } else
            render(status: 500, text: result.error)
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
    def checkConnectedThroughEthernet()
    {
        EthernetStatusEnum result = wifiUtilsService.checkConnectedThroughEthernet()
        if (result != EthernetStatusEnum.error) {
            render(status: 200, text: ethernetConnectionStatus[result])
        }
        else {
            def errMsg = "An error occurred in checkConnectedThroughEthernet."
            logService.cam.error(errMsg)
            render(status: 500, text: errMsg)
        }
    }
}

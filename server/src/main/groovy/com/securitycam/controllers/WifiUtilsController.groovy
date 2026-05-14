package com.securitycam.controllers

import com.securitycam.commands.SetUpWifiCommand
import com.securitycam.commands.SetWifiStatusCommand
import com.securitycam.enums.PassFail
import com.securitycam.error.NVRRestMethodException
import com.securitycam.interfaceobjects.ObjectCommandResponse
import com.securitycam.interfaceobjects.WifiConnectResult
import com.securitycam.services.LogService
import com.securitycam.services.WifiUtilsService
import com.securitycam.validators.SetupWifiValidator
import groovy.json.JsonOutput
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*

@Controller
@RequestMapping("/wifiUtils")
class WifiUtilsController {
    @Autowired
    WifiUtilsService wifiUtilsService

    @Autowired
    SetupWifiValidator setupWifiValidator

    @Autowired
    SimpMessagingTemplate brokerMessagingTemplate
    @Autowired
    LogService logService

    @Secured(['ROLE_CLOUD', 'ROLE_CLIENT'])
    @PostMapping("/scanWifi")
    def scanWifi() {
        ObjectCommandResponse result = wifiUtilsService.scanWifi()
        if (result.status == PassFail.PASS) {
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(result.responseObject)
        } else {
            logService.cam.error "scanWifi: error: ${result.error}"
            throw new NVRRestMethodException(result.error, "wifiUtils/scanWifi")
        }
    }

    // TODO: Add forget wifi adapter and a facility to change the wifi password,

    @Secured(['ROLE_CLOUD', 'ROLE_CLIENT'])
    @PostMapping("/setUpWifi")
    def setUpWifi(@Valid @RequestBody SetUpWifiCommand cmd) {
        ObjectCommandResponse result
        result = wifiUtilsService.setUpWifi(cmd)

        if (result.status == PassFail.PASS) {
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(result.responseObject)
        } else {
            logService.cam.error "setUpWifi: error: ${result.error}"
         //  result.status = PassFail.FAIL
            return new ResponseEntity<WifiConnectResult>(result.responseObject as WifiConnectResult, HttpStatus.BAD_REQUEST)
        }
    }

    @Secured(['ROLE_CLOUD', 'ROLE_CLIENT'])
    @PostMapping("/checkWifiStatus")
    def checkWifiStatus() {
        ObjectCommandResponse result = wifiUtilsService.checkWifiStatus()
        if (result.status == PassFail.PASS) {
            return ResponseEntity.ok(result.responseObject)
        } else {
            logService.cam.error "checkWifiStatus: error: ${result.error}"
            result.status = PassFail.FAIL
            throw new NVRRestMethodException(result.error, "wifiUtils/checkWifiStatus")
        }
    }

    @Secured(['ROLE_CLOUD', 'ROLE_CLIENT'])
    @PostMapping("/setWifiStatus")
    def setWifiStatus(@Valid @RequestBody SetWifiStatusCommand cmd) {
        ObjectCommandResponse result = wifiUtilsService.setWifiStatus(cmd)
        if (result.status == PassFail.PASS) {
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(result.responseObject)
        } else {
            logService.cam.error "setWifiStatus: error: ${result.error}"
            result.status = PassFail.FAIL
            throw new NVRRestMethodException(result.error, "wifiUtils/setWifiStatus")
        }
    }

    @Secured(['ROLE_CLOUD', 'ROLE_CLIENT'])
    @PostMapping("/getCurrentWifiConnection")
    def getCurrentWifiConnection() {
        ObjectCommandResponse result = wifiUtilsService.getCurrentWifiConnection()

        if (result.status == PassFail.PASS)
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(result.responseObject)
        else {
            logService.cam.error "getCurrentWifiConnection: error: ${result.error}"
            result.status = PassFail.FAIL
            throw new NVRRestMethodException(result.error, "wifiUtils/getCurrentWifiConnection")
        }
    }

    @Secured(['ROLE_CLOUD', 'ROLE_CLIENT'])
    @PostMapping("/getActiveIPAddresses")
    def getActiveIPAddresses() {
        ObjectCommandResponse result = wifiUtilsService.getActiveIPAddresses()

        if (result.status == PassFail.PASS)
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(JsonOutput.toJson(result))
        else {
            logService.cam.error "checkWifiStatus: error: ${result.error}"
            throw new NVRRestMethodException(result.error, "wifiUtils/getActiveIPAddresses")
        }
    }

    @Secured(['ROLE_CLOUD'])
    @PostMapping("/checkEthernetInUse")
    def checkEthernetInUse() {
        ObjectCommandResponse result = wifiUtilsService.checkEthernetInUse()
        if (result.status == PassFail.PASS) {
            return ResponseEntity.ok(result.responseObject)
        } else {
            def errMsg = "An error occurred in checkEthernetInUse:- (${result.error})"
            logService.cam.error(errMsg)
            throw new NVRRestMethodException(errMsg, "wifiUtils/checkEthernetInUse")
        }
    }

    @Secured(['ROLE_CLIENT'])
    @PostMapping("/checkEthernetInUseNVR")
    def checkEthernetInUseNVR() {
        ObjectCommandResponse result = wifiUtilsService.checkEthernetInUse(false)
        if (result.status == PassFail.PASS) {
            return ResponseEntity.ok(result.responseObject)
        } else {
            def errMsg = "An error occurred in checkEthernetInUseNVR:- (${result.error})"
            logService.cam.error(errMsg)
            throw new NVRRestMethodException(errMsg, "wifiUtils/checkEthernetInUseNVR")
        }
    }
}

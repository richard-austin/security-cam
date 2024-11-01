package com.securitycam.controllers

import com.securitycam.commands.SetUpWifiCommand
import com.securitycam.enums.PassFail
import com.securitycam.error.NVRRestMethodException
import com.securitycam.interfaceobjects.Greeting
import com.securitycam.interfaceobjects.HelloMessage
import com.securitycam.interfaceobjects.ObjectCommandResponse
import com.securitycam.services.LogService
import com.securitycam.services.WifiUtilsService
import com.securitycam.validators.SetupWifiValidator
import groovy.json.JsonOutput
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.util.HtmlUtils

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
}

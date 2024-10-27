package com.securitycam.controllers

import com.securitycam.commands.DiscoverCameraDetailsCommand
import com.securitycam.commands.GetSnapshotCommand
import com.securitycam.commands.SetOnvifCredentialsCommand
import com.securitycam.enums.PassFail
import com.securitycam.interfaceobjects.CommandResponse
import com.securitycam.interfaceobjects.ObjectCommandResponse
import com.securitycam.validators.DiscoverCameraDetailsCommandValidator
import groovy.json.JsonOutput
import com.securitycam.services.LogService
import com.securitycam.services.OnvifService
import com.securitycam.validators.BadRequestResult
import com.securitycam.validators.GeneralValidator
import com.securitycam.validators.PtzPresetsCommandValidator
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.annotation.Secured
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/onvif")
class OnvifController {
    @Autowired
    LogService logService

    @Autowired
    OnvifService onvifService

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD'])
    @PostMapping("/discover")
    def discover() {
        ObjectCommandResponse resp = onvifService.getMediaProfiles()
        if (resp.status == PassFail.PASS) {
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(JsonOutput.toJson(resp.responseObject))
        } else
            return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(resp.error)
    }

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD'])
    @PostMapping(value="/discoverCameraDetails", consumes = MediaType.APPLICATION_JSON_VALUE)
    def discoverCameraDetails(@RequestBody DiscoverCameraDetailsCommand cmd) {
        GeneralValidator gv = new GeneralValidator(cmd, new DiscoverCameraDetailsCommandValidator())
        BindingResult results = gv.validate()
        if (results.hasErrors()) {
            logService.cam.error "/onvif/discoverCameraDetails: Validation error: " + results.toString()
            BadRequestResult retVal = new BadRequestResult(results)
            return ResponseEntity
                    .badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(retVal)
        } else {
            ObjectCommandResponse resp = onvifService.getMediaProfiles(cmd)

            if (resp.status == PassFail.PASS)
                return ResponseEntity.ok().body(JsonOutput.toJson(resp.responseObject))
            else
                return new ResponseEntity<Object>(resp.error, HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD', 'ROLE_GUEST'])
    @PostMapping("/getSnapshot")
    def getSnapshot(@Valid @RequestBody GetSnapshotCommand cmd) {
        def result = onvifService.getSnapshot(cmd.url, cmd.cred)
        if (result.status == PassFail.PASS)
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(result.responseObject)
        else if (result.errno == 401) {
            return new ResponseEntity<Object>(result.error, HttpStatus.UNAUTHORIZED)
        } else {
            return ResponseEntity.badRequest().body(result.error)
        }
    }

    /**
     * setOnvifCredentials: Set the access credentials used for Onvif operations on the cameras
     * @param cmd : Command object containing the username and password
     * @return: Success/error state.
     */
    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD'])
    @PostMapping("/setOnvifCredentials")
    def setOnvifCredentials(@Valid @RequestBody SetOnvifCredentialsCommand cmd) {
        ObjectCommandResponse response = onvifService.setOnvifCredentials(cmd)

        if (response.status != PassFail.PASS)
            return ResponseEntity.badRequest().body(response.error)
        else
            return ResponseEntity.ok()

    }

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD', 'ROLE_GUEST'])
    @PostMapping("/haveOnvifCredentials")
    def haveOnvifCredentials() {
        ObjectCommandResponse response = onvifService.haveOnvifCredentials()
        if (response.status == PassFail.PASS)
            ResponseEntity.ok(response.responseObject ? 'true' : 'false')
        else
            return ResponseEntity.badRequest().body(response.error)
    }
}

package com.securitycam.controllers

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.internal.LinkedTreeMap
import com.securitycam.commands.DiscoverCameraDetailsCommand
import com.securitycam.commands.GetSnapshotCommand
import com.securitycam.commands.SetOnvifCredentialsCommand
import com.securitycam.commands.UpdateCamerasCommand
import com.securitycam.configuration.Config
import com.securitycam.enums.PassFail
import com.securitycam.interfaceobjects.ObjectCommandResponse
import com.securitycam.validators.DiscoverCameraDetailsCommandValidator
import com.securitycam.validators.UpdateCamerasCommandValidator
import groovy.json.JsonOutput
import com.securitycam.services.LogService
import com.securitycam.services.OnvifService
import com.securitycam.validators.BadRequestResult
import com.securitycam.validators.GeneralValidator
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/onvif")
class OnvifController {
    @Autowired
    LogService logService

    @Autowired
    OnvifService onvifService

    @Autowired
    Config config

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
    @PostMapping(value="/getSnapshot", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    def getSnapshot(@Valid @RequestBody GetSnapshotCommand cmd) {
        def result = onvifService.getSnapshot(cmd.url, cmd.cred)
        if (result.status == PassFail.PASS) {
            byte[] bytes = result.responseObject as byte[]
            ResponseEntity ent = ResponseEntity
                    .ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(bytes.length)
                    .body(bytes)
            return ent
        }
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
    @PostMapping(value="/setOnvifCredentials", consumes = "application/json", produces = "text/html")
    def setOnvifCredentials(@Valid @RequestBody SetOnvifCredentialsCommand cmd) {
        ObjectCommandResponse response = onvifService.setOnvifCredentials(cmd)

        if (response.status != PassFail.PASS)
            return ResponseEntity.badRequest().body(response.error)
        else
            return ResponseEntity.ok("")

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

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD'])
    @PostMapping("/updateCameras")
    def updateCameras(@RequestBody UpdateCamerasCommand cmd) {
        ObjectCommandResponse result
        def gv = new GeneralValidator(cmd, new UpdateCamerasCommandValidator())
        BindingResult results = gv.validate()
        if (results.hasErrors()) {
            logService.cam.error "/onvif/updateCameras: Validation error: " + results.toString()
            BadRequestResult retVal = new BadRequestResult(results)
            return ResponseEntity
                    .badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(retVal)
        } else {
            result = onvifService.updateCameras(cmd)
            if (result.status != PassFail.PASS)
                return new ResponseEntity<Object>(result.error, HttpStatus.INTERNAL_SERVER_ERROR)
            return result.responseObject
        }
    }


    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD'])
    @PostMapping(value="/uploadMaskFile")
    def uploadMaskFile(@RequestParam("maskFile") MultipartFile maskFile) {
        if(maskFile == null || maskFile.empty) {
            return ResponseEntity
                    .badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body([maskFile: "maskFile should be a valid .pgm file"])
        }
        logService.cam.debug "CamController.uploadMaskFile() called"
        ObjectCommandResponse result
            result = onvifService.uploadMaskFile(maskFile)
            if (result.status == PassFail.PASS)
                return ResponseEntity.ok([])
            else
                throw new Exception(result.error)
    }


}
